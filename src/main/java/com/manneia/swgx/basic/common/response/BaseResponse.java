package com.manneia.swgx.basic.common.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author luokaixuan
 * @description com.manneia.basic.common
 * @created 2025/5/12 21:14
 */
@Getter
@Setter
@ToString
public class BaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private String responseCode;

    private String responseMessage;
}
