package com.nexamarket.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    @Test
    void rejectsRequestsAfterClientConsumesItsBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest firstRequest = requestFrom("203.0.113.10");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, chain);

        MockHttpServletRequest secondRequest = requestFrom("203.0.113.10");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, chain);

        verify(chain, times(1)).doFilter(firstRequest, firstResponse);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    void keepsSeparateBucketsForDifferentForwardedClients() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest firstRequest = requestFrom("203.0.113.11");
        MockHttpServletRequest secondRequest = requestFrom("203.0.113.12");
        filter.doFilter(firstRequest, new MockHttpServletResponse(), chain);
        filter.doFilter(secondRequest, new MockHttpServletResponse(), chain);

        verify(chain, times(2)).doFilter(
                org.mockito.ArgumentMatchers.any(MockHttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(MockHttpServletResponse.class));
    }

    private MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/search");
        request.addHeader("X-Forwarded-For", address);
        return request;
    }
}
