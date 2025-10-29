package com.manneia.swgx.basic.controller;

import com.manneia.swgx.basic.common.response.SingleResponse;
import com.manneia.swgx.basic.service.HealthService;
import com.manneia.swgx.basic.vo.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author luokaixuan
 * @description com.manneia.basic.controller
 * @created 2025/5/12 21:11
 */
@RestController
@RequestMapping("health")
public class HealthController {

    @Resource
    private HealthService healthService;

    @PostMapping("post")
    public Result<String> postHealth() {
        SingleResponse<String> postHealth = healthService.postHealth();
        return Result.success(postHealth.getData());
    }

    @PostMapping("get")
    public Result<String> getHealth() {
        SingleResponse<String> getHealth = healthService.getHealth();
        return Result.success(getHealth.getData());
    }
}
