package com.manneia.swgx.basic;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.manneia.swgx.basic.common.constant.BasicKey;
import com.manneia.swgx.basic.config.CustomizeUrlConfig;
import com.manneia.swgx.basic.utils.BwHttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.alibaba.fastjson.JSON.toJSONString;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest
class SwgxBasicApplicationTests {

    private final BwHttpUtil bwHttpUtil;

    private final CustomizeUrlConfig customizeUrlConfig;

    /**
     * 获取公司ID
     */
    @Test
    void getCompanyId() {
        JSONObject getCompanyInfoRequest = new JSONObject();
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_NAME, "悠乐芳（中国）商贸有限公司");
        getCompanyInfoRequest.put(BasicKey.TAXPAYER_REG_NO, "91310000MA1HNBYL8J");
        String companyId;
        try {
            String request = Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(getCompanyInfoRequest).getBytes(StandardCharsets.UTF_8));
            String response = bwHttpUtil.httpPostRequest(customizeUrlConfig.getQueryCompanyInfoUrl(), request, "json");
            JSONObject companyInfoResult = JSON.parseObject(response);
            log.info("企业信息:{}", toJSONString(companyInfoResult));
            JSONObject data = companyInfoResult.getJSONObject(BasicKey.INTERFACE_DATA);
            companyId = data.getString(BasicKey.COMPANY_ID);
            log.info("companyId:{}", companyId);
        } catch (Exception e) {
            log.error("获取企业信息失败:{}", toJSONString(e));
            throw new RuntimeException("获取企业信息失败");
        }
    }

}
