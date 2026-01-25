# What Happens When You Use WebClient with Servlet (Spring MVC)

## Context

When your application uses:
- `spring-boot-starter-web` (Tomcat, servlet-based)
- `spring-boot-starter-webflux` (for WebClient only)

You must use `.block()` to convert reactive `Mono` to blocking result.

```java
ServiceResponse<MemberDTO> response = webClient.post()
    .uri("/member/register")
    .bodyValue(memberDTO)
    .retrieve()
    .bodyToMono(responseType)
    .block();  // ← Required in servlet environment
```

## What You Get vs What You Miss

| Feature | Available? | Notes |
|---------|------------|-------|
| Better error handling (`.onStatus()`) | ✅ Yes | Main benefit over RestTemplate |
| Fluent builder API | ✅ Yes | Cleaner code |
| Service discovery (`@LoadBalanced`) | ✅ Yes | Same as RestTemplate |
| **Non-blocking I/O** | ❌ No | `.block()` defeats this |
| **Thread efficiency** | ❌ No | Thread waits for response |
| **Backpressure support** | ❌ No | Not applicable |

## Thread Model Comparison

### Servlet + WebClient + `.block()` (Your Setup)

```
Request 1: [Thread-1] ████████ waiting ████████ [done]
Request 2: [Thread-2] ████████ waiting ████████ [done]
Request 3: [Thread-3] ████████ waiting ████████ [done]
Request 4: [Thread-4] ████████ waiting ████████ [done]

→ 4 threads for 4 concurrent requests
→ Tomcat default pool: ~200 threads
→ Max ~200 concurrent requests before queuing
```

### WebFlux + Netty (Full Reactive)

```
Request 1: [Thread-1] █ send → free
Request 2: [Thread-1] █ send → free     ← Same thread!
Request 3: [Thread-1] █ send → free
Request 4: [Thread-1] █ send → free
              ...
Response 1: [Thread-2] █ callback
Response 2: [Thread-1] █ callback

→ 2 threads handled 4 requests
→ Can handle thousands with few threads
```

## Why Use WebClient with Servlet Anyway?

1. **Better than RestTemplate** - RestTemplate is deprecated
2. **Cleaner error handling** - `.onStatus()` vs try-catch `HttpClientErrorException`
3. **Future-proof** - Easy migration if you go reactive later
4. **Modern API** - Fluent builder pattern

## When to Consider Full Reactive (WebFlux)

| Consider WebFlux When | Stick with Servlet When |
|-----------------------|-------------------------|
| High concurrency (10k+ req/sec) | Moderate load (~100s req/sec) |
| New service from scratch | Existing codebase |
| Team knows reactive programming | Team prefers traditional style |
| Using R2DBC (reactive DB) | Using JPA/JDBC |
| Streaming data (SSE, WebSocket) | Request-response only |

## Migration Path: Servlet → WebFlux

If you decide to go full reactive later:

1. Replace `spring-boot-starter-web` with `spring-boot-starter-webflux`
2. Change controller returns: `ResponseEntity<T>` → `Mono<ResponseEntity<T>>`
3. Remove all `.block()` calls
4. Replace JPA with R2DBC for database
5. Replace blocking libraries with reactive alternatives

## Example: Same Code in Both Models

### Servlet (Current)
```java
@GetMapping("/user/{id}")
public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
    ServiceResponse<UserDTO> response = serviceClient.get(
        "http://USER-SERVICE",
        "/user/" + id,
        new ParameterizedTypeReference<ServiceResponse<UserDTO>>() {}
    );  // blocks here
    return ResponseEntity.ok(response.getData());
}
```

### WebFlux (Reactive)
```java
@GetMapping("/user/{id}")
public Mono<ResponseEntity<UserDTO>> getUser(@PathVariable String id) {
    return serviceClient.getReactive(
        "http://USER-SERVICE",
        "/user/" + id,
        new ParameterizedTypeReference<ServiceResponse<UserDTO>>() {}
    )  // no blocking
    .map(response -> ResponseEntity.ok(response.getData()));
}
```

## Summary

Using WebClient with servlet is like driving a sports car in city traffic — you're not using its full speed, but you still get better handling, modern features, and comfort compared to an old car (RestTemplate).

**It's a valid and practical choice** when:
- You want modern HTTP client features
- Full reactive migration is not feasible
- Your load doesn't require extreme concurrency
