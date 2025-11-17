package com.manneia.swgx.basic.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 纳税人识别号清单
 *
 * <p>用于同步、采集等任务遍历调用接口。</p>
 *
 * @author lk
 */
public final class TaxpayerRegistry {

    private static final List<String> TAXPAYER_LIST = Collections.unmodifiableList(
            Arrays.asList(
                    "911100006259100634",
                    "91210200604876695D",
                    "9137022061438079XD",
                    "91500000559008169H",
                    "913101156072273832",
                    "91320100762116851Q",
                    "91320592737076763U",
                    "91440116618481272A",
                    "91440300MA5F1RUC82"
            )
    );

    private TaxpayerRegistry() {
    }

    public static List<String> getTaxpayerList() {
        return TAXPAYER_LIST;
    }
}


