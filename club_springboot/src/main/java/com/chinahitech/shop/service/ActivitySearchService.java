package com.chinahitech.shop.service;

import com.chinahitech.shop.bean.Activity;
import cn.hutool.json.JSONUtil;
import com.chinahitech.shop.exception.BusinessException;
import com.chinahitech.shop.mapper.ActivityMapper;
import com.chinahitech.shop.mapper.ActivitySearchOutboxMapper;
import com.chinahitech.shop.utils.PageUtils;
import com.chinahitech.shop.utils.Result;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.*;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** MySQL 是事实来源；ES 负责中文、拼音及附件正文相关性召回，失败时显式降级。 */
@Service
public class ActivitySearchService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ActivitySearchService.class);
    @Resource private ActivityMapper activityMapper;
    @Resource private ActivitySearchOutboxMapper outbox;
    @Resource private FileStorageService fileStorageService;
    @Autowired(required = false) private RestHighLevelClient client;
    @Value("${app.elasticsearch.index:baituan_activity_v2}") private String index;
    private volatile boolean initialized;
    @Value("${jwt.secret:${JWT_SECRET:}}") private String cursorSecret;


    /** 默认也分页；ES 精确计数，禁止先取 100 条再切页；限制深分页和超长关键词。 */
    public Result searchPage(String keyword, Integer pageNum, Integer pageSize) {
        String query = keyword == null ? "" : keyword.trim();
        int page = pageNum == null ? 1 : pageNum;
        int size = pageSize == null ? 10 : pageSize;
        if (page < 1 || size < 1 || size > 100 || (long) page * size > 10000 || query.length() > 200) {
            throw new BusinessException("SEARCH_PARAM_INVALID", "页码从1开始，每页1至100条，最多访问前10000条；关键词最多200字符");
        }
        int offset = (page - 1) * size;
        if (client == null || query.isEmpty()) return mysqlPage(query, page, size, offset, client == null);
        try {
            ensureIndex();
            SearchSourceBuilder source = new SearchSourceBuilder()
                    .query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("is_accepted", true))
                            .must(QueryBuilders.multiMatchQuery(query)
                                    .field("name", 5).field("name_pinyin", 4)
                                    .field("group_name", 3).field("group_name_pinyin", 2)
                                    .field("organizer", 3).field("organizer_pinyin", 2)
                                    .field("description").field("attachment_content")
                                    .operator(org.elasticsearch.index.query.Operator.AND)))
                    .sort("_score", SortOrder.DESC).sort("hot", SortOrder.DESC).sort("id", SortOrder.DESC)
                    .from(offset).size(size).trackTotalHits(true).timeout(TimeValue.timeValueSeconds(2))
                    .fetchSource(false);
            SearchResponse response = client.search(new SearchRequest(index).source(source)
                    .allowPartialSearchResults(false), RequestOptions.DEFAULT);
            if (response.isTimedOut() || response.getFailedShards() > 0) {
                throw new IllegalStateException("ES 返回超时或分片失败");
            }
            List<Integer> ids = Arrays.stream(response.getHits().getHits())
                    .map(SearchHit::getId).map(Integer::valueOf).collect(Collectors.toList());
            Map<Integer, Activity> current = ids.isEmpty() ? Collections.emptyMap()
                    : activityMapper.searchVisibleByIds(ids).stream()
                    .collect(Collectors.toMap(Activity::getId, a -> a));
            List<Activity> records = ids.stream().map(current::get).filter(Objects::nonNull).collect(Collectors.toList());
            long total = response.getHits().getTotalHits().value;
            // 同步窗口内撤回/删除记录会被主库复核滤掉，total 仍明确表示索引匹配总量。
            return PageUtils.ok("items", PageUtils.pageResult(records, total, page, size))
                    .data("searchSource", "elasticsearch").data("degraded", false)
                    .data("totalScope", "indexMatches").data("filteredStaleHits", ids.size() - records.size());
        } catch (Exception e) {
            LOG.warn("活动搜索降级到 MySQL: {}", e.toString());
            return mysqlPage(query, page, size, offset, true);
        }
    }

    private Result mysqlPage(String keyword, int page, int size, int offset, boolean degraded) {
        return PageUtils.ok("items", PageUtils.pageResult(activityMapper.queryPage(keyword, offset, size),
                activityMapper.countQuery(keyword), page, size))
                .data("searchSource", "mysql").data("degraded", degraded).data("totalScope", "databaseMatches")
                .data("searchCapabilities", "活动名、社团名、主办方 LIKE 查询；不含拼音和附件正文");
    }

    /** 单一映射资源用于自动建索引；analyzer 变更需要新索引名和回填，不覆盖旧索引。 */
    public synchronized void ensureIndex() throws Exception {
        if (client == null) throw new BusinessException("SEARCH_DISABLED", "ES未启用");
        if (initialized) return;
        if (!client.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT)) {
            String json;
            try (java.io.InputStream in = new ClassPathResource("search/activity-index.json").getInputStream()) {
                json = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }
            try {
                client.indices().create(new CreateIndexRequest(index + "_000001").source(json, XContentType.JSON)
                        .alias(new org.elasticsearch.action.admin.indices.alias.Alias(index)), RequestOptions.DEFAULT);
                // 新索引（含灾后重建）自动回填历史活动，不依赖后续恰好发生一次修改。
                outbox.enqueueAll();
            } catch (ElasticsearchStatusException e) {
                if (!e.getMessage().contains("resource_already_exists_exception")) throw e;
            }
        }
        if (!client.indices().existsAlias(new org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest(index),
                RequestOptions.DEFAULT)) throw new IllegalStateException("搜索目标必须为别名，请使用新的 ELASTICSEARCH_INDEX");
        Map<String, org.elasticsearch.cluster.metadata.MappingMetadata> mappings = client.indices()
                .getMapping(new GetMappingsRequest().indices(index), RequestOptions.DEFAULT).mappings();
        if (mappings.size() != 1) throw new IllegalStateException("搜索别名必须指向一个索引");
        Object schema = mappings.values().iterator().next().getSourceAsMap().get("_meta");
        if (!(schema instanceof Map) || !Integer.valueOf(2).equals(((Map<?, ?>) schema).get("schema_version"))) {
            throw new IllegalStateException("索引映射版本不匹配，请使用新的 ELASTICSEARCH_INDEX 并回填");
        }
        initialized = true;
    }

    /** 队列重放时读当前主库状态；外部版本号避免多个实例处理同一活动时乱序覆盖。 */
    public void synchronize(int activityId, long eventId) throws Exception {
        ensureIndex();
        Activity activity = activityMapper.getActivityById(activityId);
        try {
            if (activity == null || !Boolean.TRUE.equals(activity.getIsAccepted())) {
                // 永久版本墓碑避免物理删除后的版本过期让延迟旧请求复活文档。
                client.index(new IndexRequest(index).id(String.valueOf(activityId)).setRequireAlias(true)
                        .version(eventId).versionType(VersionType.EXTERNAL_GTE)
                        .source(Map.of("id", activityId, "is_accepted", false, "hot", 0)), RequestOptions.DEFAULT);
            } else {
                String text = fileStorageService.extractText(activity.getAttachment());
                client.index(new IndexRequest(index).id(String.valueOf(activityId)).setRequireAlias(true)
                        .version(eventId).versionType(VersionType.EXTERNAL_GTE).source(toDocument(activity, text)),
                        RequestOptions.DEFAULT);
            }
        } catch (ElasticsearchStatusException e) {
            if (e.status().getStatus() != 409) { initialized = false; throw e; }
            // 更高版本已经完成，旧任务无需覆盖。
        }
    }

    /** 仅报告入队数量；通过 status 观察后台完成进度，不把排队当作已经索引成功。 */
    public int rebuild() {
        if (client == null) throw new BusinessException("SEARCH_DISABLED", "ES未启用");
        return outbox.enqueueAll();
    }

    public Map<String, Object> status() {
        if (client == null) return Map.of("enabled", false);
        return outbox.status();
    }

    private Map<String, Object> toDocument(Activity a, String text) {
        Map<String, Object> d = new HashMap<>();
        d.put("id", a.getId()); d.put("name", a.getName()); d.put("name_pinyin", pinyin(a.getName()));
        d.put("group_name", a.getGroupName()); d.put("group_name_pinyin", pinyin(a.getGroupName()));
        d.put("organizer", a.getOrganizer()); d.put("organizer_pinyin", pinyin(a.getOrganizer()));
        d.put("description", a.getDescription()); d.put("attachment_content", text);
        d.put("hot", a.getHot()); d.put("is_accepted", true);
        return d;
    }

    private String pinyin(String value) {
        if (value == null) return "";
        try {
            HanyuPinyinOutputFormat f = new HanyuPinyinOutputFormat();
            f.setCaseType(HanyuPinyinCaseType.LOWERCASE); f.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            f.setVCharType(HanyuPinyinVCharType.WITH_V);
            StringBuilder out = new StringBuilder();
            for (char c : value.toCharArray()) {
                String[] py = PinyinHelper.toHanyuPinyinStringArray(c, f);
                if (py == null || py.length == 0) out.append(c);
                else out.append(' ').append(py[0]).append(' ');
            }
            String spaced = out.toString().trim().replaceAll("\\s+", " ");
            return spaced + " " + spaced.replace(" ", "");
        } catch (Exception e) { throw new IllegalStateException("拼音转换失败", e); }
    }

    /** PIT 固定搜索视图；游标包含上一页排序值，查询不统计总命中数。 */
    public Result searchSlice(String keyword, Integer pageSize, String cursor) {
        String query = keyword == null ? "" : keyword.trim();
        int size = pageSize == null ? 20 : pageSize;
        if (size < 1 || size > 100 || query.length() > 200 || (cursor != null && cursor.length() > 20000)) {
            throw new BusinessException("SEARCH_PARAM_INVALID", "搜索分页参数不合法");
        }
        cn.hutool.json.JSONObject state = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                var parts = cursor.split("\\.");
                if (parts.length != 2 || !java.security.MessageDigest.isEqual(
                        signCursor(parts[0]).getBytes(StandardCharsets.US_ASCII),
                        parts[1].getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException();
                state = JSONUtil.parseObj(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
                if (!query.equals(state.getStr("query")) || state.getLong("expires", 0L) < System.currentTimeMillis()) {
                    throw new IllegalArgumentException();
                }
            } catch (RuntimeException invalid) {
                throw new BusinessException("SEARCH_CURSOR_INVALID", "搜索游标无效或已过期，请重新搜索");
            }
        }
        // 同一次翻页固定数据源，ES 中途故障不能切换 MySQL 游标。
        if (state != null && "mysql".equals(state.getStr("source")) || state == null && (client == null || query.isEmpty())) {
            return mysqlSlice(query, size, state);
        }
        String pit = state == null ? null : state.getStr("pit");
        try {
            if (pit == null) {
                ensureIndex();
                var open = new org.elasticsearch.action.search.OpenPointInTimeRequest(index);
                open.keepAlive(TimeValue.timeValueMinutes(1));
                pit = client.openPointInTime(open, RequestOptions.DEFAULT).getPointInTimeId();
            }
            var source = new SearchSourceBuilder()
                    .pointInTimeBuilder(new org.elasticsearch.search.builder.PointInTimeBuilder(pit).setKeepAlive(TimeValue.timeValueMinutes(1)))
                    .query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("is_accepted", true))
                            .must(QueryBuilders.multiMatchQuery(query).field("name", 5).field("name_pinyin", 4)
                                    .field("group_name", 3).field("group_name_pinyin", 2)
                                    .field("organizer", 3).field("organizer_pinyin", 2)
                                    .field("description").field("attachment_content")
                                    .operator(org.elasticsearch.index.query.Operator.AND)))
                    .sort("_score", SortOrder.DESC).sort("hot", SortOrder.DESC)
                    .sort("id", SortOrder.DESC).sort("_shard_doc", SortOrder.ASC)
                    .size(size + 1).trackTotalHits(false).timeout(TimeValue.timeValueSeconds(2)).fetchSource(false);
            if (state != null) source.searchAfter(state.getJSONArray("after").toArray());
            var response = client.search(new SearchRequest().source(source).allowPartialSearchResults(false), RequestOptions.DEFAULT);
            if (response.isTimedOut() || response.getFailedShards() > 0) throw new IllegalStateException("搜索超时");
            String latestPit = response.pointInTimeId() == null ? pit : response.pointInTimeId();
            var hits = response.getHits().getHits();
            boolean hasNext = hits.length > size;
            int take = Math.min(size, hits.length);
            var ids = Arrays.stream(hits).limit(take).map(SearchHit::getId).map(Integer::valueOf).toList();
            var current = ids.isEmpty() ? Map.<Integer, Activity>of() : activityMapper.searchVisibleByIds(ids).stream()
                    .collect(Collectors.toMap(Activity::getId, a -> a));
            var records = ids.stream().map(current::get).filter(Objects::nonNull).toList();
            String next = null;
            if (hasNext) {
                var nextState = new HashMap<String, Object>();
                nextState.put("query", query);
                nextState.put("source", "elasticsearch");
                nextState.put("pit", latestPit);
                nextState.put("after", hits[take - 1].getSortValues());
                nextState.put("expires", System.currentTimeMillis() + 60000);
                next = encodeCursor(nextState);
            } else {
                closePit(latestPit);
            }
            return Result.ok().data("items", records).data("hasNext", hasNext).data("nextCursor", next)
                    .data("searchSource", "elasticsearch").data("degraded", false);
        } catch (Exception failure) {
            if (pit != null) closePit(pit);
            if (state != null) throw new BusinessException("SEARCH_CURSOR_EXPIRED", "搜索视图不可用，请重新搜索");
            LOG.warn("首次游标搜索降级到 MySQL，cause={}", failure.getClass().getSimpleName());
            return mysqlSlice(query, size, null);
        }
    }

    private Result mysqlSlice(String query, int size, cn.hutool.json.JSONObject state) {
        Long before = state == null ? null : state.getLong("before");
        if (state != null && (before == null || before <= 0)) throw new BusinessException("SEARCH_CURSOR_INVALID", "搜索游标无效");
        var rows = activityMapper.searchSlice(query, before, size + 1);
        boolean hasNext = rows.size() > size;
        var items = rows.subList(0, Math.min(size, rows.size()));
        String next = hasNext ? encodeCursor(Map.of("source", "mysql", "query", query,
                "before", items.getLast().getId(), "expires", System.currentTimeMillis() + 60000)) : null;
        return Result.ok().data("items", items).data("hasNext", hasNext).data("nextCursor", next)
                .data("searchSource", "mysql").data("degraded", true);
    }

    private String encodeCursor(Map<String, Object> state) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(JSONUtil.toJsonStr(state).getBytes(StandardCharsets.UTF_8));
        return payload + "." + signCursor(payload);
    }

    private String signCursor(String payload) {
        try {
            if (cursorSecret == null || cursorSecret.length() < 32) throw new IllegalStateException("游标签名密钥未配置");
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(cursorSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(("activity-search:" + payload).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("游标签名失败", failure);
        }
    }

    private void closePit(String pit) {
        try {
            client.closePointInTime(new org.elasticsearch.action.search.ClosePointInTimeRequest(pit), RequestOptions.DEFAULT);
        } catch (Exception failure) {
            LOG.warn("搜索视图将等待过期释放，cause={}", failure.getClass().getSimpleName());
        }
    }
}
