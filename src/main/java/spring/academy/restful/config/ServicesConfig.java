package spring.academy.restful.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.academy.restful.accounts.AccountManager;
import spring.academy.restful.accounts.internal.JpaAccountManager;

@Configuration
public class ServicesConfig {

	@Bean
	public AccountManager accountManager() {
		return new JpaAccountManager();
	}

}
