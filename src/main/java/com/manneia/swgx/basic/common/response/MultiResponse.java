package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @author luokaixuan
 * @description 列表响应
 * @created 2025/5/12 21:16
 */
@Getter
@Setter
public class MultiResponse<T> extends BaseResponse implements Serializable {

    private static final long serialVersionUID = 806291559912913359L;

    private transient List<T> dataList;

    public static <T> MultiResponse<T> of(List<T> resultList) {
        MultiResponse<T> multiResponse = new MultiResponse<>();
        multiResponse.setSuccess(true);
        multiResponse.setDataList(resultList);
        return multiResponse;
    }
}
