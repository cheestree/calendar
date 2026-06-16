package integration.third;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

abstract class ThirdPartyProviderIntegrationTestSupport {

    protected static void replaceHttp(Object provider, String baseUrl) throws Exception {
        Field http = provider.getClass().getDeclaredField("http");
        http.setAccessible(true);
        http.set(provider, RestClient.builder().baseUrl(baseUrl).build());
    }

    protected static final class LocalJsonServer implements AutoCloseable {
        private final HttpServer server;
        private volatile String lastRawQuery = "";

        private LocalJsonServer(String responseBody) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> respond(exchange, responseBody));
            server.start();
        }

        static LocalJsonServer respondingWith(String responseBody) throws IOException {
            return new LocalJsonServer(responseBody);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        String lastRawQuery() {
            return lastRawQuery;
        }

        private void respond(HttpExchange exchange, String responseBody) throws IOException {
            lastRawQuery = exchange.getRequestURI().getRawQuery() == null
                    ? ""
                    : exchange.getRequestURI().getRawQuery();
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
