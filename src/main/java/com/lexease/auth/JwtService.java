package com.lexease.auth;

import com.lexease.shared.api.ApiException;
import com.lexease.users.UserAccount;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AuthProperties authProperties;
    private final Clock clock;

    public JwtService(AuthProperties authProperties, Clock clock) {
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public String createAccessToken(UserAccount user) {
        try {
            Instant now = Instant.now(clock);
            Instant expiresAt = now.plus(authProperties.jwt().accessTokenTtl());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(authProperties.jwt().issuer())
                    .subject(user.getId().toString())
                    .claim("role", user.getRole().name())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secretBytes()));
            return signedJWT.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign access token", ex);
        }
    }

    public ParsedAccessToken parse(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw invalidToken();
            }
            if (!signedJWT.verify(new MACVerifier(secretBytes()))) {
                throw invalidToken();
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (!authProperties.jwt().issuer().equals(claims.getIssuer())) {
                throw invalidToken();
            }
            Date expiration = claims.getExpirationTime();
            if (expiration == null || !expiration.toInstant().isAfter(Instant.now(clock))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Access token expired");
            }
            return new ParsedAccessToken(UUID.fromString(claims.getSubject()), claims.getStringClaim("role"));
        } catch (ParseException | JOSEException | IllegalArgumentException ex) {
            throw invalidToken();
        }
    }

    public long accessTokenTtlSeconds() {
        return authProperties.jwt().accessTokenTtl().toSeconds();
    }

    private byte[] secretBytes() {
        byte[] secret = authProperties.jwt().accessSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_ACCESS_SECRET must be at least 32 bytes for HS256");
        }
        return secret;
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "Invalid access token");
    }

    public record ParsedAccessToken(UUID userId, String role) {
    }
}
