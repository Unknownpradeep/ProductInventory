package com.hepl.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
        System.out.println("WELCOME TO THE PRODUCT INVENTORY");
	}

}
