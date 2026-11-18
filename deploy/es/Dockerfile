FROM docker.elastic.co/elasticsearch/elasticsearch:7.17.22

# 使用内置 cjk analyzer；拼音在应用层生成，部署无需安装第三方分析插件。
COPY elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml
