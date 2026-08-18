package com.gc2026.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
    "SPRING_DATASOURCE_PASSWORD=test",
    "spring.flyway.enabled=false"
})
class PortfolioApplicationTests {

	@Test
	void contextLoads() {
	}

}