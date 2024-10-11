package spring.academy.restful.config;

import spring.academy.restful.accounts.AccountManager;
import spring.academy.restful.accounts.internal.JpaAccountManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.academy.restful.rewards.internal.account.AccountRepository;
import spring.academy.restful.rewards.internal.account.JpaAccountRepository;
import spring.academy.restful.rewards.internal.restaurant.JpaRestaurantRepository;
import spring.academy.restful.rewards.internal.restaurant.RestaurantRepository;
import spring.academy.restful.rewards.internal.reward.JdbcRewardRepository;
import spring.academy.restful.rewards.internal.reward.RewardRepository;

import javax.sql.DataSource;

/**
 * Rewards application configuration - services and repositories.
 * <p>
 * Because this is used by many similar lab projects with slightly different
 * classes and packages, everything is explicitly created using @Bean methods.
 * Component-scanning risks picking up unwanted beans in the same package in
 * other projects.
 */
@Configuration
public class AppConfig {

	@Bean
	public AccountManager accountManager() {
		return new JpaAccountManager();
	}

	@Bean
	public AccountRepository accountRepository() {
		return new JpaAccountRepository();
	}

	@Bean
	public RestaurantRepository restaurantRepository() {
		return new JpaRestaurantRepository();
	}

	@Bean
	public RewardRepository rewardRepository(DataSource dataSource) {
		return new JdbcRewardRepository(dataSource);
	}

}
