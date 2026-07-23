package com.finance.finance_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
class FinanceTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
