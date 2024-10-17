package spring.academy.restful.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

@TestConfiguration
public class JwtConfig {

    @Bean
    JwtEncoder jwtEncoder(@Value("classpath:authz.pub") RSAPublicKey pub,
                          @Value("classpath:authz.pem") RSAPrivateKey pem) {
        RSAKey key = new RSAKey.Builder(pub).privateKey(pem).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("classpath:authz.pub") RSAPublicKey pub) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(pub).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer("http://localhost:9000");
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<Object>>(JwtClaimNames.AUD,
                (aud) -> !Collections.disjoint(aud, Collections.singleton("cashcard-client")));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience));
        return jwtDecoder;
    }

}
