package com.partradar.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Downstream services build links from X-Forwarded-* headers, so the gateway must send them. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForwardedHeadersTests {

  static final Map<String, String> received = new ConcurrentHashMap<>();
  static final HttpServer backend = startBackend();

  @LocalServerPort int port;

  @DynamicPropertySource
  static void routeToBackend(DynamicPropertyRegistry registry) {
    registry.add("SHOP_SERVICE_URL", () -> "http://localhost:" + backend.getAddress().getPort());
  }

  @AfterAll
  static void stopBackend() {
    backend.stop(0);
  }

  @Test
  void forwardsOriginalHostAndProtocol() throws Exception {
    HttpResponse<Void> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/shops"))
                    .build(),
                HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(received)
        .containsEntry("x-forwarded-host", "localhost:" + port)
        .containsEntry("x-forwarded-proto", "http");
  }

  private static HttpServer startBackend() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            exchange
                .getRequestHeaders()
                .forEach((name, values) -> received.put(name.toLowerCase(), values.getFirst()));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
