package com.partradar.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.partradar.platform.testapp.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class PlatformAutoConfigurationTests {

  @Autowired MockMvc mvc;

  @Test
  void healthIsPublic() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void otherEndpointsRequireAuthentication() throws Exception {
    mvc.perform(get("/things/1")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void notFoundBecomesProblemDetail() throws Exception {
    mvc.perform(get("/things/42"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("https://partradar.dev/problems/not-found"))
        .andExpect(jsonPath("$.detail").value("Thing 42 not found"));
  }

  @Test
  @WithMockUser
  void businessRuleViolationBecomesUnprocessableContent() throws Exception {
    mvc.perform(get("/things/0"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.type").value("https://partradar.dev/problems/thing-zero"));
  }

  @Test
  @WithMockUser
  void validationErrorsListInvalidFields() throws Exception {
    mvc.perform(post("/things").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
  }
}
