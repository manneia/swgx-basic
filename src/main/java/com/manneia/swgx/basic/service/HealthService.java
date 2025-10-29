package com.manneia.swgx.basic.service;


import com.manneia.swgx.basic.common.response.SingleResponse;

/**
 * @author luokaixuan
 * @description 健康检查服务
 * @created 2025/5/12 21:07
 */
public interface HealthService {

    SingleResponse<String> postHealth();

    SingleResponse<String> getHealth();
}
