package spring.academy.restful;

import spring.academy.restful.config.AppConfig;
import spring.academy.restful.config.DbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({AppConfig.class, DbConfig.class})
public class SpringAcademyRestfulLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAcademyRestfulLabApplication.class, args);
	}

}
