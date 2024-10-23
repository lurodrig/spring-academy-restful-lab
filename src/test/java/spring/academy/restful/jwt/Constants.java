package spring.academy.restful.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.List;
import java.util.function.Consumer;

public class Constants {

    private static final Logger log = LoggerFactory.getLogger(Constants.class);

    // Defaults
    public static final String SUBJECT = "johndoe";
    public static final String ISSUER = "http://localhost:9000";
    public static final List<String> AUDIENCE = List.of("rewards-api");
    public static final String[] SCOPE = {"rewards:BANKER", "rewards:CUSTOMER"};

    public static final Consumer<JwtClaimsSet.Builder> EMPTY_BUILDER_COMSUMER = new Consumer<JwtClaimsSet.Builder>() {
        @Override
        public void accept(JwtClaimsSet.Builder builder) {
            log.info("Empty JWTClaimsSet.Builder ");
        }
    };
}
