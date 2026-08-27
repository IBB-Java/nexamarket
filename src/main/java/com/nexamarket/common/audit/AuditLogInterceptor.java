package com.nexamarket.common.audit;

import com.nexamarket.auth.security.AuthPrincipal;
import com.nexamarket.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Records successful and failed mutating API operations after a response. */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditLogRepository;

    public AuditLogInterceptor(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        if ("GET".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())) {
            return;
        }
        try {
            auditLogRepository.save(new AuditLog(actorId(), request.getMethod(), request.getRequestURI(),
                    response.getStatus(), MDC.get(CorrelationIdFilter.MDC_KEY)));
        } catch (RuntimeException ignored) {
            // Audit storage must not turn an already-created API response into an error.
        }
    }

    private Long actorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
