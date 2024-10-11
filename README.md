# Spring Academy restful lab with Java 21 and spring-boot-starter-data-jpa

## What?

This project contains a simplified version of the [Spring Academy RESTful Application with Spring Boot Lab](https://spring.academy/courses/spring-boot/lessons/spring-boot-rest-app-lab)

## Why?

I implemented here my own solutions and there is some code that I want to keep :) Also this project uses latest spring boot stable version (3.3.4) to date, makes use of the starter data jpa and Java 21. The original one is from 2021 and uses vanilla spring data jpa and an older version of hibernate.

## How?

```
$ java --version
java 21.0.2 2024-01-16 LTS

$ ./gradlew --version

------------------------------------------------------------
Gradle 8.10.2
------------------------------------------------------------

$ ./gradlew bootRun

$ ./gradlew test

$ curl http://localhost:8080/accounts
```
**Note:** `AccountClientTests` do what they promise and test the `AccountController` trough and http client (`org.springframework.web.client.RestTemplate`), so they need the application to be running in order to succeed. You can run them with `$ ./gradlew test --tests "spring.academy.restful.common.accounts.client.AccountClientTests"`

**Note:** [hsqldb is running in server mode](https://github.com/lurodrig/hsqldb-in-server-mode)

## Thanks

Model, JPA repositories, REST Controllers and SQL scripts taken from [spring academy labs](https://spring.academy/courses/spring-boot)


