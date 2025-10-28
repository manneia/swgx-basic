package com.manneia.swgxgxhfwylf.common.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author luokaixuan
 * @description com.manneia.basic.common.response
 * @created 2025/5/12 21:16
 */
@Getter
@Setter
public class PageResponse<T> extends MultiResponse<T> {

    private static final long serialVersionUID = -4425689838336931735L;

    /**
     * 当前页
     */
    private int currentPage;
    /**
     * 每页结果数
     */
    private int pageSize;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 总数
     */
    private int total;

    public static <T> PageResponse<T> of(List<T> datas, int total, int pageSize, int currentPage) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setSuccess(true);
        pageResponse.setDataList(datas);
        pageResponse.setTotal(total);
        pageResponse.setPageSize(pageSize);
        pageResponse.setCurrentPage(currentPage);
        pageResponse.setTotalPage((pageSize + total - 1) / pageSize);
        return pageResponse;
    }
}
