<<<<<<< HEAD
# Kaal

## Time aware Rate Limiter  
Spring Boot + Redis Token Bucket

---

## What is Kaal?

Kaal is an API rate limiter that controls requests per client IP using the Token Bucket algorithm.

- Each client IP has its own bucket  
- Bucket capacity is 10 tokens  
- Refill rate is 5 tokens per second  
- If no tokens are available, request is rejected with HTTP 429  

---

## How Token Bucket Works

- Every request needs 1 token  
- If token exists, request is allowed  
- If bucket is empty, request is blocked  
- Tokens are refilled continuously based on time  

---


## Tech Stack

| Component              | Role                         |
|-----------------------|------------------------------|
| Spring Boot 3.4.5     | Core framework               |
| Spring Cloud Gateway  | Rate limiting gateway        |
| Redis 8.0             | Distributed token storage    |
| Java 21               | Runtime                      |
| Gradle 8.10           | Build tool                   |
=======
# kaal
rate-limiter
>>>>>>> origin/main
