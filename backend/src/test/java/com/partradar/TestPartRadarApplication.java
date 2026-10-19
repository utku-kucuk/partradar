package com.partradar;

import org.springframework.boot.SpringApplication;

/** Runs the application against a throwaway Testcontainers database. */
public class TestPartRadarApplication {

  public static void main(String[] args) {
    SpringApplication.from(PartRadarApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
