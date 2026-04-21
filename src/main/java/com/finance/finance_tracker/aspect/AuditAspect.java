package com.finance.finance_tracker.aspect;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finance_tracker.Util.SecurityUtil;
import com.finance.finance_tracker.service.AuditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    private final ObjectMapper objectMapper;



    @Pointcut("execution(* com.finance.finance_tracker.service.Impl.AccountServiceImpl.saveAccount(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.BudgetServiceImpl.saveBudget(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.CategoryServiceImpl.saveCategory(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.TransactionServiceImpl.saveTransaction(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.UserServiceImpl.registerUser(..))")
    public void saveMethods() {}

    @Pointcut("execution(* com.finance.finance_tracker.service.Impl.AccountServiceImpl.updateAccount(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.AccountServiceImpl.deposit(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.AccountServiceImpl.withdraw(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.BudgetServiceImpl.resetSpending(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.CategoryServiceImpl.updateCategory(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.TransactionServiceImpl.updateTransaction(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.UserServiceImpl.updateUser(..))")
    public void updateMethods() {}

    @Pointcut("execution(* com.finance.finance_tracker.service.Impl.AccountServiceImpl.deleteAccount(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.BudgetServiceImpl.deleteBudget(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.CategoryServiceImpl.deleteCategory(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.TransactionServiceImpl.deleteTransaction(..))" +
            " || execution(* com.finance.finance_tracker.service.Impl.UserServiceImpl.deleteUser(..))")
    public void deleteMethods() {}

    @Pointcut("saveMethods() || updateMethods() || deleteMethods()")
    public void auditableMethods() {}


    @Around("auditableMethods()")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object args[] = joinPoint.getArgs();
        Object result = null;
        Throwable e = null;
        Long start = System.currentTimeMillis();

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            e = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            final Object finalResult = result;
            final Throwable finalException = e;

            CompletableFuture.runAsync(() -> {
                String action = determineAction(methodName);
                String entityType = determineEntityType(joinPoint.getTarget().getClass());
                Object entityId = extractEntityId(args, action, finalResult);
                String details = buildDetails(action, entityType, entityId, args, finalResult, finalException, duration);

                auditService.log(
                        SecurityUtil.getCurrentUserId(),
                        SecurityUtil.getCurrentUsername(),
                        action,
                        entityType,
                        entityId instanceof Long ? (Long) entityId : null,
                        details
                );
            });
        }
    }

    private String determineAction(String methodName) {
        if (methodName.startsWith("save") || methodName.equals("registerUser")) return "CREATE";
        if (methodName.startsWith("update")
                || "deposit".equals(methodName)
                || "withdraw".equals(methodName)
                || "resetSpending".equals(methodName)) return "UPDATE";
        if (methodName.startsWith("delete")) return "DELETE";
        return "UNKNOWN";
    }

    private String determineEntityType(Class<?> targetClass) {
        String name = targetClass.getSimpleName();
        if (name.contains("Account")) return "Account";
        if (name.contains("Transaction")) return "Transaction";
        if (name.contains("Category")) return "Category";
        if (name.contains("Budget")) return "Budget";
        if (name.contains("User")) return "User";
        return "Unknown";
    }

    private Object extractEntityId(Object[] args, String action, Object result) {
        if (("DELETE".equals(action) || "UPDATE".equals(action)) && args.length > 0 && args[0] instanceof Long) {
            return args[0];
        }

        if ("CREATE".equals(action) && result != null) {
            try {
                Field idField = result.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                return idField.get(result);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private String buildDetails(String action, String entityType, Object entityId, Object[] args,
                                Object result, Throwable exception, long duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action: ").append(action).append("\n");
        sb.append("Entity: ").append(entityType).append("\n");
        if (entityId != null) sb.append("ID: ").append(entityId).append("\n");
        try {
            sb.append("Args: ").append(objectMapper.writeValueAsString(args)).append("\n");
            if (result != null) sb.append("Result: ").append(objectMapper.writeValueAsString(result)).append("\n");
        } catch (Exception e) {
            sb.append("Args: ").append(Arrays.toString(args)).append("\n");
            if (result != null) sb.append("Result: ").append(result).append("\n");
        }
        if (exception != null) sb.append("Exception: ").append(exception.getMessage()).append("\n");
        sb.append("Duration: ").append(duration).append(" ms");
        return sb.toString();
    }
}
