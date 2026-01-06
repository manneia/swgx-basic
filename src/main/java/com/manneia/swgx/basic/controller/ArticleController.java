package com.manneia.swgx.basic.controller;

import com.manneia.swgx.basic.common.response.PageResponse;
import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.model.domain.ZlInfo;
import com.manneia.swgx.basic.model.domain.ZlInfoNr;
import com.manneia.swgx.basic.model.request.QueryArticleDto;
import com.manneia.swgx.basic.service.KnowledgeBaseService;
import com.manneia.swgx.basic.vo.MultiResult;
import com.manneia.swgx.basic.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lkx
 * @description com.manneia.swgx.basic.controller
 * @created 2026-01-05 18:18:23
 */
@RestController
@RequestMapping("article")
@RequiredArgsConstructor
public class ArticleController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/query")
    public MultiResult<ZlInfo> getArticleByKeyword(@RequestBody QueryArticleDto queryArticleDto) {
        PageResponse<ZlInfo> response = knowledgeBaseService.queryKnowledge(queryArticleDto);
        if (Boolean.TRUE.equals(response.getSuccess())) {
            return MultiResult.successMulti(response.getDataList(), response.getTotal(), response.getCurrentPage(), response.getPageSize());
        } else {
            return MultiResult.errorMulti(response.getResponseCode(), response.getResponseMessage());
        }
    }

    @PostMapping("/query/code")
    public Result<ZlInfoNr> getArticleByCode(@RequestBody QueryArticleDto queryArticleDto) {
        SingleResponse<ZlInfoNr> response = knowledgeBaseService.queryKnowledgeByArticleCode(queryArticleDto);
        if (Boolean.TRUE.equals(response.getSuccess())) {
            return Result.success(response.getData());
        } else {
            return Result.error(response.getResponseCode(), response.getResponseMessage());
        }
    }
}
