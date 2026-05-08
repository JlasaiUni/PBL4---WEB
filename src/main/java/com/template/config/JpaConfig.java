package com.template.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.template.repository")
public class JpaConfig {
    // Spring Data JPA Auditing activa @CreatedDate / @LastModifiedDate
}