package vn.edu.hust.dms.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One instance behaves like one browser tab talking to the running server: it keeps the
 * JSESSIONID and XSRF-TOKEN cookies and echoes the CSRF token in the X-XSRF-TOKEN header on
 * mutating requests, exactly as the SPA does. Responses never throw, so tests assert on status codes.
 */
public final class ApiClient {

    public static final String XSRF_COOKIE = "XSRF-TOKEN";
    public static final String XSRF_HEADER = "X-XSRF-TOKEN";
    public static final String SESSION_COOKIE = "JSESSIONID";

    private final URI baseUri;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final RestClient client;
    private final JsonMapper json = JsonMapper.shared();

    public ApiClient(int port) {
        this.baseUri = URI.create("http://localhost:" + port);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(cookies)
                .build();
        this.client = RestClient.builder()
                .baseUrl(baseUri.toString())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultStatusHandler(status -> true, (request, response) -> {
                })
                .build();
    }

    public record ApiResponse(int status, HttpHeaders headers, JsonNode body) {

        public String contentType() {
            return Optional.ofNullable(headers.getContentType()).map(MediaType::toString).orElse("");
        }

        public List<String> setCookies() {
            return headers.getOrEmpty(HttpHeaders.SET_COOKIE);
        }
    }

    public ApiResponse get(String path) {
        return exchange(HttpMethod.GET, path, null, Map.of());
    }

    public ApiResponse post(String path, Object body) {
        return exchange(HttpMethod.POST, path, body, csrfHeader());
    }

    public ApiResponse put(String path, Object body) {
        return exchange(HttpMethod.PUT, path, body, csrfHeader());
    }

    public ApiResponse delete(String path) {
        return exchange(HttpMethod.DELETE, path, null, csrfHeader());
    }

    /** Any method, with the CSRF header on everything but GET; body may be null. */
    public ApiResponse send(HttpMethod method, String path, Object body) {
        return exchange(method, path, body, HttpMethod.GET.equals(method) ? Map.of() : csrfHeader());
    }

    /** Sends exactly the given headers, without the automatic CSRF header. */
    public ApiResponse postRaw(String path, Object body, Map<String, String> headers) {
        return exchange(HttpMethod.POST, path, body, headers);
    }

    /** Sends exactly the given headers, without the automatic CSRF header. */
    public ApiResponse putRaw(String path, Object body, Map<String, String> headers) {
        return exchange(HttpMethod.PUT, path, body, headers);
    }

    /** Fetches the CSRF cookie first if needed (as the SPA does on load), then posts the credentials. */
    public ApiResponse loginAs(String username, String password) {
        if (csrfToken().isEmpty()) {
            get("/api/me");
        }
        return post("/api/auth/login", Map.of("username", username, "password", password));
    }

    public ApiResponse loginAs(TestUsers.Credentials credentials) {
        return loginAs(credentials.username(), credentials.password());
    }

    public Optional<String> cookie(String name) {
        return cookies.getCookieStore().get(baseUri).stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst();
    }

    public Optional<String> csrfToken() {
        return cookie(XSRF_COOKIE);
    }

    public void removeCookie(String name) {
        cookies.getCookieStore().get(baseUri).stream()
                .filter(cookie -> cookie.getName().equals(name))
                .toList()
                .forEach(cookie -> cookies.getCookieStore().remove(baseUri, cookie));
    }

    private Map<String, String> csrfHeader() {
        Map<String, String> headers = new HashMap<>();
        csrfToken().ifPresent(token -> headers.put(XSRF_HEADER, token));
        return headers;
    }

    private ApiResponse exchange(HttpMethod method, String path, Object body, Map<String, String> headers) {
        RestClient.RequestBodySpec spec = client.method(method)
                .uri(path)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON);
        headers.forEach((name, value) -> spec.header(name, value));
        RestClient.RequestHeadersSpec<?> ready = spec;
        if (body != null) {
            ready = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        ResponseEntity<String> entity = ready.retrieve().toEntity(String.class);
        return new ApiResponse(entity.getStatusCode().value(), entity.getHeaders(), parse(entity.getBody()));
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return json.readTree(body);
        } catch (JacksonException e) {
            throw new IllegalStateException("Response is not JSON: " + body, e);
        }
    }
}
