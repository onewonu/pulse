package com.pulse.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class DataLoadAspect {

    private static final String OPERATION_ID_KEY = "operationId";

    @Around("@annotation(com.pulse.annotation.DataLoadOperation)")
    public Object setOperationId(ProceedingJoinPoint joinPoint) throws Throwable {
        MDC.put(OPERATION_ID_KEY, UUID.randomUUID().toString().substring(0, 8));
        try {
            return joinPoint.proceed();
        } finally {
            MDC.remove(OPERATION_ID_KEY);
        }
    }
}
