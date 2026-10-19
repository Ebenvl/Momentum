package com.momentum.backend.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.momentum.backend.config.Config;
import com.momentum.backend.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Creates and checks JWT login tokens.
// After login, the app gets a token and sends it with every request,
// so it never has to send the password again.
public class TokenService {

    private final Algorithm algorithm;

    public TokenService(Config config) {
        // Signs every token with our secret from config.properties, so nobody
        // can make a fake token or change one, like changing their role.
        this.algorithm = Algorithm.HMAC256(config.get("jwt.secret"));
    }

    public String createToken(User user) {
        return JWT.create()
                .withSubject(String.valueOf(user.userId))
                .withClaim("role", user.role)
                .withExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        try {
            return JWT.require(algorithm).build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
