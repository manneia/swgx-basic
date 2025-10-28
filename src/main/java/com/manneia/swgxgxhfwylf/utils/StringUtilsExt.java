package com.manneia.swgxgxhfwylf.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhangshiwen
 * @date 2024/9/10
 */
public class StringUtilsExt {

    private StringUtilsExt() {
    }

    public static String trimToEmpty(String value, String defaultValue) {
        String tmp = StringUtils.trimToEmpty(value);
        if (tmp.isEmpty()) {
            tmp = defaultValue;
        }
        return tmp;
    }

    public static String getString(Object obj) {
        if (obj == null) {
            return "";
        }
        return StringUtils.trimToEmpty(obj.toString());
    }

    public static String getString(Object obj, String defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        String tmp = StringUtils.trimToEmpty(obj.toString());
        if (tmp.isEmpty()) {
            tmp = defaultValue;
        }
        return tmp;
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     *
     * @param str  指定字符串
     * @param strs 需要检查的字符串数组
     *
     * @return 是否匹配
     */
    public static boolean matches(String str, List<String> strs) {
        if (StringUtils.isBlank(str) || CollectionUtils.isEmpty(strs)) {
            return false;
        }
        for (String pattern : strs) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则配置:
     * ? 表示单个字符;
     * * 表示一层路径内的任意字符串，不可跨层级;
     * ** 表示任意层路径;
     *
     * @param pattern 匹配规则
     * @param url     需要匹配的url
     *
     * @return
     */
    public static boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }


    public static String replacePlaceholders(String mb, Map<String, Object> data) {
        // 使用正则表达式匹配占位符
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(mb);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = Optional.ofNullable((String) data.getOrDefault(placeholder, "")).orElse("");
            // 替换占位符为具体的值
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
