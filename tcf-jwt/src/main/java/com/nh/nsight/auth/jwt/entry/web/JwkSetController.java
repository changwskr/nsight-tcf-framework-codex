package com.nh.nsight.auth.jwt.entry.web;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwkSetController {
    private final JWKSet jwkSet;

    public JwkSetController(JWKSet jwtJwkSet) {
        this.jwkSet = jwtJwkSet;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwkSet.toJSONObject(true);
    }
}
