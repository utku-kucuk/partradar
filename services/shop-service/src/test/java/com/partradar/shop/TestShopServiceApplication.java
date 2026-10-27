package com.partradar.shop;

import org.springframework.boot.SpringApplication;

/** Runs the service against a throwaway Testcontainers database. */
public class TestShopServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(ShopServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
