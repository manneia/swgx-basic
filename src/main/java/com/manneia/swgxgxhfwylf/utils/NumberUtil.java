package com.manneia.swgxgxhfwylf.utils;

import cn.hutool.core.util.ObjectUtil;

import java.math.BigDecimal;

/**
 * 数字工具类
 */
public class NumberUtil {

    private NumberUtil(){
        throw new IllegalStateException("Utility class");
    }

    public static BigDecimal getBigDecimal(String valueStr){
        if(valueStr != null && ObjectUtil.isNotEmpty(valueStr)){
            return new BigDecimal(valueStr);
        }
        return null;
    }

}