package com.manneia.swgx.basic.service;

import com.manneia.swgx.basic.common.response.PageResponse;
import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.model.domain.ZlInfo;
import com.manneia.swgx.basic.model.domain.ZlInfoNr;
import com.manneia.swgx.basic.model.request.QueryArticleDto;

/**
 * @author lkx
 * @description 知识库服务
 * @created 2026-01-05 20:05:26
 */
public interface KnowledgeBaseService {

    /**
     * 根据关键字获取文章列表
     *
     * @param queryArticleDto 查询请求参数
     *
     * @return 文章列表
     */
    PageResponse<ZlInfo> queryKnowledge(QueryArticleDto queryArticleDto);

    /**
     * 根据文章code获取文章内容
     *
     * @param queryArticleDto 查询请求参数
     *
     * @return 文章内容
     */
    SingleResponse<ZlInfoNr> queryKnowledgeByArticleCode(QueryArticleDto queryArticleDto);
}
