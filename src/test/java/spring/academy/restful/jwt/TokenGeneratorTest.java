package spring.academy.restful.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import spring.academy.restful.config.JwtConfig;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

@SpringJUnitConfig(classes = {JwtConfig.class})
@TestConstructor(autowireMode = ALL)
public class TokenGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(TokenGeneratorTest.class);

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    private final TokenGenerator tokenGenerator;

    public TokenGeneratorTest(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.tokenGenerator = new TokenGenerator(jwtEncoder);
    }

    @Test
    void generateDefaultTokenWhenEmptyBuilderIsValid() {
        String token = tokenGenerator.generate(Constants.EMPTY_BUILDER_COMSUMER);
        assertNotNull(token);
        assertEquals(Constants.SUBJECT, jwtDecoder.decode(token).getClaim(Claims.SUBJECT));
        assertEquals(Constants.ISSUER, jwtDecoder.decode(token).getClaim(Claims.ISSUER));
        assertEquals(Constants.AUDIENCE, jwtDecoder.decode(token).getClaim(Claims.AUDIENCE));
        assertEquals(Arrays.asList(Constants.SCOPE), jwtDecoder.decode(token).getClaim(JwtConfig.SCOPE_CLAIM));
    }

    @Test
    void generateTokenWithWrongAudienceThrowsExceptionWhenDecoding() {
        String token = tokenGenerator.generate((claims) -> claims.audience(List.of("https://wrong")));
        assertNotNull(token);
        Exception exception = assertThrows(JwtValidationException.class, () -> {
            jwtDecoder.decode(token);
        });
        String expectedMessage = "An error occurred while attempting to decode the Jwt: The aud claim is not valid";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void generateExpiredTokenThrowsExceptionWhenDecoding() {
        String token = tokenGenerator.generate((claims) -> claims
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(3599))
        );
        Exception exception = assertThrows(JwtValidationException.class, () -> {
            jwtDecoder.decode(token);
        });
        String expectedMessage = "Jwt expired at";
        assertTrue(exception.getMessage().contains(expectedMessage), "Exception message does not contain '" + expectedMessage + "'");
    }
}
