package com.liu.community.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

//@Component
//@Aspect
@Slf4j
public class AlphaAspect {

    @Before("pointcut()")
    public void before(){
        System.out.println("aop--------before");
    }

    /*
    "execution(* com.liu.community.service.*.*(..))"
    第一个* 筛选所有的返回值
    第二个* 筛选包下的所有类
    第三个* 筛选类中的所有方法
    ..  筛选所有参数
     */
    @Pointcut("execution(* com.liu.community.service.*.*(..))")
    public void pointcut(){}

    @AfterReturning("pointcut()")
    public void afterReturning(){
        System.out.println("aop--------afterReturning");
    }

    @After("pointcut()")
    public void after(){
        System.out.println("aop--------after");
    }

    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("aop--------afterThrowing");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("aop--------around--------before");
        Object proceed = joinPoint.proceed();
        System.out.println("aop--------around--------after");
        return proceed;
    }

}
