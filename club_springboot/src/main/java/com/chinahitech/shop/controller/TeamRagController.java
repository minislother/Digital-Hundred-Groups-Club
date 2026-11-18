package com.chinahitech.shop.controller;

import com.chinahitech.shop.service.TeamRagAccessService;
import com.chinahitech.shop.service.TeamRagClient;
import com.chinahitech.shop.utils.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import com.chinahitech.shop.aop.RepeatLimit;
import com.chinahitech.shop.aop.RateLimiter;

/** 浏览器只传社团编号，项目、租户及上游凭据全部在服务端决定。 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class TeamRagController {
    private final TeamRagAccessService access;
    private final TeamRagClient client;

    @GetMapping("/groups")
    public Result groups(Authentication auth) { return Result.ok().data("groups", access.availableGroups(auth)); }

    @PostMapping(value = "/groups/{groupId}/chat", produces = "text/event-stream;charset=UTF-8")
    @RepeatLimit(key = "knowledge:chat:", window = 60, limit = 10, type = RateLimiter.LimitType.USER)
    public ResponseEntity<StreamingResponseBody> chat(@PathVariable int groupId, @RequestBody Question question, Authentication auth) {
        StreamingResponseBody stream = client.ask(access.authorize(groupId, auth, false), question.getQuery(), question.getConversationId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/event-stream;charset=UTF-8"))
                .cacheControl(CacheControl.noStore()).header("X-Accel-Buffering", "no").body(stream);
    }

    @GetMapping("/groups/{groupId}/documents")
    public Result documents(@PathVariable int groupId, Authentication auth) {
        return Result.ok().data("documents", client.documents(access.authorize(groupId, auth, false)));
    }

    @PostMapping("/groups/{groupId}/documents")
    @RepeatLimit(key = "knowledge:upload:", window = 60, limit = 5, type = RateLimiter.LimitType.USER)
    public Result upload(@PathVariable int groupId, @RequestParam("file") MultipartFile file, Authentication auth) {
        return Result.ok().data("document", client.upload(access.authorize(groupId, auth, true), file));
    }

    @Data
    public static class Question { private String query; private String conversationId; }
}
