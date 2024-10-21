package spring.academy.restful.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableMethodSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/accounts/**").hasAnyAuthority("SCOPE_rewards:CUSTOMER", "SCOPE_rewards:BANKER")
                        .requestMatchers(HttpMethod.POST, "/accounts/{accountId}/beneficiaries").hasAnyAuthority("SCOPE_rewards:CUSTOMER", "SCOPE_rewards:BANKER")
                        .requestMatchers(HttpMethod.DELETE, "/accounts/{accountId}/beneficiaries/{beneficiaryId}").hasAnyAuthority("SCOPE_rewards:CUSTOMER", "SCOPE_rewards:BANKER")
                        .requestMatchers(HttpMethod.PUT, "/accounts/{accountId}").hasAnyAuthority("SCOPE_rewards:CUSTOMER", "SCOPE_rewards:BANKER")
                        .requestMatchers(HttpMethod.POST, "/accounts").hasAuthority("SCOPE_rewards:BANKER")
                        .requestMatchers(HttpMethod.DELETE, "/accounts/{accountId}").hasAuthority("SCOPE_rewards:BANKER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
                .csrf((csfr -> csfr.ignoringRequestMatchers("/accounts/**")));
        return http.build();
    }
}
