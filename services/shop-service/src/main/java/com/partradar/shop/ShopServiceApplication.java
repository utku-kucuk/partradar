package com.partradar.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShopServiceApplication.class, args);
  }
}
