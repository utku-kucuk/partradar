package com.partradar.inquiry;

import org.springframework.boot.SpringApplication;

/** Runs the service against a throwaway Testcontainers database. */
public class TestInquiryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(InquiryServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
