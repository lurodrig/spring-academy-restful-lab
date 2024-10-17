package spring.academy.restful.jwt.utils;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;

public class TokenGenerator {

    private JwtEncoder jwtEncoder;

    public TokenGenerator(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generate(Consumer<JwtClaimsSet.Builder> consumer) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(100000))
                .subject("sarah1")
                .issuer("http://localhost:9000")
                .audience(Arrays.asList("cashcard-client"))
                .claim("scp", Arrays.asList("cashcard:read", "cashcard:write"));
        consumer.accept(builder);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(builder.build());
        return this.jwtEncoder.encode(parameters).getTokenValue();
    }
}
