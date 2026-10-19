package com.partradar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTests {

  @Autowired MockMvc mvc;

  @Test
  void healthEndpointIsPublicAndUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void openApiDocumentIsPublic() throws Exception {
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void otherEndpointsRequireAuthentication() throws Exception {
    mvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
  }
}
