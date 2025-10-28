package com.manneia.swgxgxhfwylf.service.impl;

import com.manneia.baiwangbasic.common.response.SingleResponse;
import com.manneia.baiwangbasic.service.HealthService;
import org.springframework.stereotype.Service;

/**
 * @author luokaixuan
 * @description com.manneia.basic.service.impl
 * @created 2025/5/12 21:09
 */
@Service
public class HealthServiceImpl implements HealthService {
    @Override
    public SingleResponse<String> postHealth() {
        return SingleResponse.of("post is health");
    }

    @Override
    public SingleResponse<String> getHealth() {
        return SingleResponse.of("get is health");
    }
}
