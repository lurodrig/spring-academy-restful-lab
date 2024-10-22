package spring.academy.restful.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.academy.restful.rewards.internal.account.AccountRepository;
import spring.academy.restful.rewards.internal.account.JpaAccountRepository;
import spring.academy.restful.rewards.internal.restaurant.JpaRestaurantRepository;
import spring.academy.restful.rewards.internal.restaurant.RestaurantRepository;
import spring.academy.restful.rewards.internal.reward.JdbcRewardRepository;
import spring.academy.restful.rewards.internal.reward.RewardRepository;

import javax.sql.DataSource;

@Configuration
public class RepositoriesConfig {

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
