package com.example.tsubuyaki.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    @DisplayName("クライアントIP取得_XForwardedForあり_先頭のIPを返す")
    void resolve_whenXForwardedForExists_returnsFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");

        String actual = resolver.resolve(request);

        assertThat(actual).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("クライアントIP取得_XForwardedForなし_getRemoteAddrを返す")
    void resolve_whenXForwardedForMissing_returnsRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.20");

        String actual = resolver.resolve(request);

        assertThat(actual).isEqualTo("203.0.113.20");
    }
}
