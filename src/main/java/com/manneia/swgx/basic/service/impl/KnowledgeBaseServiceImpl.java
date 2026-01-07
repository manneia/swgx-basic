package com.manneia.swgx.basic.service.impl;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manneia.swgx.basic.common.response.PageResponse;
import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.exception.BizException;
import com.manneia.swgx.basic.exception.RepoErrorCode;
import com.manneia.swgx.basic.model.domain.ZlInfo;
import com.manneia.swgx.basic.model.domain.ZlInfoNr;
import com.manneia.swgx.basic.model.request.QueryArticleDto;
import com.manneia.swgx.basic.service.KnowledgeBaseService;
import com.manneia.swgx.basic.service.ZlInfoNrService;
import com.manneia.swgx.basic.service.ZlInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author lkx
 * @description com.manneia.swgx.basic.service.impl
 * @created 2026-01-05 20:07:41
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final ZlInfoService zlInfoService;

    private final ZlInfoNrService zlInfoNrService;

    @Override
    public PageResponse<ZlInfo> queryKnowledge(QueryArticleDto queryArticleDto) {
        Assert.notNull(queryArticleDto, "请求参数不能为null");
        if (StringUtils.isBlank(queryArticleDto.getKeyword())) {
            return PageResponse.error("REQUEST_PARAM_NOT_EMPTY","关键字不能为空");
        }
        String keyWord = queryArticleDto.getKeyword().trim();
        String[] keyWordList = keyWord.split(" ");
        try {
            Page<ZlInfo> page = zlInfoService.lambdaQuery()
                    .and(wrapper -> {
                        for (int i = 0; i < keyWordList.length; i++) {
                            String kw = keyWordList[i].trim();
                            if (i == 0) {
                                wrapper.like(ZlInfo::getDataKeyWord, kw);
                            } else {
                                wrapper.or().like(ZlInfo::getDataKeyWord, kw);
                            }
                        }
                    })
                    .page(new Page<>(queryArticleDto.getCurrentNo(), queryArticleDto.getCurrentSize()));
            return PageResponse.of(page.getRecords(), (int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent());
        } catch (Exception e) {
            log.error("查询知识库失败: {}", JSON.toJSONString(e));
            throw new BizException("获取知识库数据失败", RepoErrorCode.SELECT_FAILED);
        }
    }

    @Override
    public SingleResponse<ZlInfoNr> queryKnowledgeByArticleCode(QueryArticleDto queryArticleDto) {
        Assert.notNull(queryArticleDto, "请求参数不能为null");
        Assert.notBlank(queryArticleDto.getArticleCode(), "文章代码不能为空");
        try {
            ZlInfoNr result = zlInfoNrService.lambdaQuery()
                    .eq(ZlInfoNr::getDataCode, queryArticleDto.getArticleCode())
                    .one();
            return SingleResponse.of(result);
        } catch (Exception e) {
            log.error("查询知识库文章详情失败: {}", JSON.toJSONString(e));
            throw new BizException("获取知识库详情数据失败", RepoErrorCode.SELECT_FAILED);
        }
    }
}
