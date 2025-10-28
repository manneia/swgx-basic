package com.manneia.swgx.basic.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lkx
 * @description com.manneia.baiwangbasic.interceptor
 * @created 2025-05-19 08:55:22
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    public LogAspect() {
        log.info("Common LogAspect");
    }

    /**
     * 定义一个切点
     */
    @Pointcut("execution(public * com..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Before("controllerPointcut()")
    public void doBefore(JoinPoint joinPoint) {

        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        HttpServletRequest request = attributes.getRequest();
        Signature signature = joinPoint.getSignature();
        String name = signature.getName();

        // 打印请求信息
        log.info("------------- 开始 -------------");
        log.info("请求地址: {} {}", request.getRequestURL(), request.getMethod());
        log.info("类名方法: {}.{}", signature.getDeclaringTypeName(), name);
        log.info("远程地址: {}", request.getRemoteAddr());

        // 打印请求参数
        Object[] args = joinPoint.getArgs();

        // 排除特殊类型的参数，如文件类型
        Object[] filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile))
                .toArray();

        // 排除字段，敏感字段或太长的字段不显示：身份证、手机号、邮箱、密码等
        Set<String> excludeProperties = new HashSet<>(Arrays.asList("身份证", "手机号", "邮箱", "密码"));

        // 过滤后的请求参数
        String argsExcluded = JSON.toJSONString(filteredArgs, (PropertyPreFilter) (object, names, value) -> !excludeProperties.contains(names));

        log.info("请求参数: {}", argsExcluded);
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();

        // 定义排除的属性列表，排除字段，敏感字段或太长的字段不显示：身份证、手机号、邮箱、密码等
        Set<String> excludeProperties = new HashSet<>(Arrays.asList("身份证", "手机号", "邮箱", "密码"));

        // 过滤后的结果
        String resultExcluded = JSON.toJSONString(result, (PropertyPreFilter) (object, name, value) -> !excludeProperties.contains(name));

        log.info("返回结果: {}", resultExcluded);
        log.info("------------- 结束 耗时：{} ms -------------", System.currentTimeMillis() - startTime);
        return result;
    }
}
