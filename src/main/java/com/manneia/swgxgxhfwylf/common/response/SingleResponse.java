package com.manneia.swgxgxhfwylf.common.response;

import lombok.Getter;
import lombok.Setter;

/**
 * @author luokaixuan
 * @description 单条响应
 * @created 2025/5/12 21:18
 */
@Getter
@Setter
public class SingleResponse<T> extends BaseResponse {

    private static final long serialVersionUID = -3128970571429107133L;

    private transient T data;

    public static <T> SingleResponse<T> of(T data) {
        SingleResponse<T> singleResponse = new SingleResponse<>();
        singleResponse.setSuccess(true);
        singleResponse.setData(data);
        return singleResponse;
    }

    public static <T> SingleResponse<T> fail(String errorCode, String errorMessage) {
        SingleResponse<T> singleResponse = new SingleResponse<>();
        singleResponse.setSuccess(false);
        singleResponse.setResponseCode(errorCode);
        singleResponse.setResponseMessage(errorMessage);
        return singleResponse;
    }
}
