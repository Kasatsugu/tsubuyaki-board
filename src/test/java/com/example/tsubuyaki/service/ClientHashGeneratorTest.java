package com.example.tsubuyaki.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientHashGeneratorTest {

    private final ClientHashGenerator generator = new ClientHashGenerator();

    @Test
    @DisplayName("clientHash生成_IPとUserAgent_SHA256の先頭8文字を返す")
    void generate_returnsFirstEightCharsOfSha256() throws Exception {
        String source = "192.0.2.10Mozilla/5.0";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
        StringBuilder expected = new StringBuilder();
        for (byte value : hashed) {
            expected.append(String.format("%02x", value));
        }

        String actual = generator.generate("192.0.2.10", "Mozilla/5.0");

        assertThat(actual).isEqualTo(expected.substring(0, 8));
    }
}
