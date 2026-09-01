package com.nexamarket.common.integration;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InternalApiKeyFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAValidInternalRequestAndClearsContextAfterward() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/catalog/variants/1");
        request.addHeader(InternalApiKeyFilter.HEADER, "expected-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> assertThat(
                SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("INTERNAL");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void leavesAnInvalidInternalRequestUnauthenticatedForSecurityToReject() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/catalog/variants/1");
        request.addHeader(InternalApiKeyFilter.HEADER, "wrong-key");
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
