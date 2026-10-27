package com.partradar.inquiry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InquiryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InquiryServiceApplication.class, args);
  }
}
