package com.xagentstudy.common.security;

import com.xagentstudy.auth.JwtProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SecretCipherService {
    private final String secret;

    public SecretCipherService(JwtProperties properties) {
        this.secret = properties.secret();
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = (byte) (raw[i] ^ key[i % key.length]);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
    }

    public String decrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        byte[] raw = Base64.getUrlDecoder().decode(cipher);
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = (byte) (raw[i] ^ key[i % key.length]);
        }
        return new String(out, StandardCharsets.UTF_8);
    }
}
