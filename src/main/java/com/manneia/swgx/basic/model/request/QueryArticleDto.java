package com.manneia.swgx.basic.model.request;

import lombok.Data;

/**
 * @author lkx
 * @description com.manneia.swgx.basic.model.request
 * @created 2026-01-05 20:19:47
 */
@Data
public class QueryArticleDto {

    private String keyword;

    private String brevityCode;

    private String articleCode;

    private Long currentNo;

    private Long currentSize;
}
