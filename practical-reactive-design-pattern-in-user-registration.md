# Practical Reactive Design Pattern in User Registration Process

## Overview

Study notes on reactive save patterns in Spring WebFlux with R2DBC — understanding when to use `save()` vs `saveAll()`. Current implementation of save() in Member-service
is not full end-to-end non-blocking in theory, but still effectively non-blocking even without `saveAll()`.

## save() vs saveAll()

| Aspect | `save()` | `saveAll()` |
|--------|----------|-------------|
| **Input** | Concrete entity object | `Publisher` (Mono or Flux) |
| **Return** | `Mono<T>` | `Flux<T>` |
| **Use case** | Entity needs modification before save | Entity flows straight from request to DB untouched |

### saveAll() — Fully End-to-End Non-Blocking Pipeline

```java
@PostMapping(consumes = "application/json")
@ResponseStatus(HttpStatus.CREATED)
public Mono<Taco> postTaco(@RequestBody Mono<Taco> tacoMono) {
    return tacoRepo.saveAll(tacoMono).next();
}
```

The `Mono<Taco>` is never unwrapped. It flows directly from the request body into the repository. `saveAll()` accepts any `Publisher`, and `.next()` converts the resulting `Flux` back to a `Mono`. The entire pipeline — from HTTP request deserialization to database write — is a single unbroken reactive chain with no materialization at any point.

### save() — Why It Is Not Fully End-to-End Non-Blocking

```java
// Controller
@PostMapping("registerMember")
public Mono<ResponseEntity<ServiceResponse<MemberDTO>>> registerMember(
        @RequestBody MemberDTO memberDTO) {        // ← BLOCKED HERE: request body fully deserialized into concrete object
    Member newMember = memberMapper.memberDTOtoMember(memberDTO);  // ← object materialized in memory
    return memberService.registerMember(newMember);
}
```

With `save()`, the reactive chain is **broken at the controller entry point**. The `@RequestBody MemberDTO memberDTO` parameter forces Spring to fully deserialize the HTTP request body into a concrete Java object **before** the handler method executes. This is a synchronous, blocking operation — the thread must wait for the complete request body to arrive and be parsed.

In contrast, `@RequestBody Mono<MemberDTO>` would let the framework handle deserialization reactively — the method executes immediately, and the actual data arrives later through the reactive pipeline.

**However**, even after this initial materialization, the database operations (`findByEmail`, `save`) are still non-blocking via R2DBC. So the blocking only occurs at the deserialization step, not at the I/O-heavy database layer.

## Why Member/User Services Can't Use saveAll()

User profile data almost always requires pre-processing before persistence:

- Duplicate email check (`findByEmail`)
- Manual UUID generation (R2DBC has no `@GeneratedValue`)
- Manual timestamp setting (R2DBC has no `@PrePersist`)
- INSERT vs UPDATE control via `Persistable.isNew()`
- Business validation

This is true for most companies' member/user services. **User data inherently requires business logic before persistence.** You must unwrap the Mono via `flatMap` to modify the entity, which means accepting `Mono<T>` as input gives minimal benefit — you'd unwrap it immediately anyway.

## Where saveAll() Shines

Content data that is:

- **Simple** — no duplicate checks needed
- **Append-only** — always INSERT, never UPDATE
- **No pre-processing** — data goes straight from request to DB

Examples: comments, logs, events, analytics data.

## Is the Difference Significant in Practice?

**No — it's effectively non-blocking.** The only "blocking" part is request body deserialization, which is CPU-bound work taking microseconds. True performance-killing blocking means a thread *waiting for I/O* (database, network) for milliseconds.

| Aspect | WebFlux + R2DBC (save) | Spring MVC + JPA (truly blocking) |
|--------|------------------------|-----------------------------------|
| Request deserialization | Blocks briefly (microseconds) | Same |
| `findByEmail()` | Thread released, notified when done | Thread **waits** 5-50ms |
| Setting UUID/timestamp | Nanoseconds, negligible | Same |
| `save()` | Thread released, notified when done | Thread **waits** 5-50ms |
| Thread during DB I/O | **Free to handle other requests** | **Doing nothing, just waiting** |

## When Does It Matter?

- **High concurrency (1000+ simultaneous users)** — WebFlux handles this with a few threads; MVC needs 1000 threads sitting idle waiting for DB
- **Low traffic (<500 concurrent users)** — traditional Spring MVC + JPA handles this fine

## Key Takeaway

The `save()` pattern with `@RequestBody MemberDTO` is not fully end-to-end non-blocking in theory — the request deserialization step is synchronous. But in practice, the performance difference is negligible because the real bottleneck is always database I/O, and that part **is** fully non-blocking with R2DBC. The `save()` inside `flatMap` is the correct and standard approach for member/user services using WebFlux + R2DBC.
