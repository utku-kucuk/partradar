package com.partradar.platform.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registers RFC 9457 problem detail responses for every servlet service. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  GlobalExceptionHandler globalExceptionHandler() {
    return new GlobalExceptionHandler();
  }
}
