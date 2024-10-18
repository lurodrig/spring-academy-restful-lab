package spring.academy.restful.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
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
import spring.academy.restful.jwt.Defaults;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

@Configuration
public class JwtConfig {

    public static final String SCOPE_CLAIM = "scp";

    @Value("classpath:keys/authz.pub")
    private Resource publicKeyResource;

    @Value("classpath:keys/authz.pem")
    private Resource privateKeyResource;

    private RSAPrivateKey pem;
    private RSAPublicKey pub;

    @PostConstruct
    void init() throws IOException {
        pem = RsaKeyConverters.pkcs8().convert(new ByteArrayInputStream(privateKeyResource.getContentAsByteArray()));
        pub = RsaKeyConverters.x509().convert(new ByteArrayInputStream(publicKeyResource.getContentAsByteArray()));
    }

    @Bean
    JwtEncoder jwtEncoder() {
        RSAKey key = new RSAKey.Builder(pub).privateKey(pem).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(pub).build();
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(Defaults.ISSUER);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<Object>>(JwtClaimNames.AUD,
                (aud) -> !Collections.disjoint(aud, Defaults.AUDIENCE));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, audience));
        return jwtDecoder;
    }

}
