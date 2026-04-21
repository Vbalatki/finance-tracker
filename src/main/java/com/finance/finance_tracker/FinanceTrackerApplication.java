package com.finance.finance_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class FinanceTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceTrackerApplication.class, args);
	}

}
