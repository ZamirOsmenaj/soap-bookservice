# Book Service — SOAP Demo
## Two implementations of the same contract: JAX-WS and Spring Boot

This repository contains a real-world SOAP web service demo implemented **twice** —
once with plain Java / JAX-WS, and once with Spring Boot / Spring-WS.

Both implementations expose the **identical WSDL contract** and accept the same SOAP messages.
The goal is to let you compare how the two frameworks solve the same problem.

---

## Repository Structure

```
soap-bookservice/
│
├── jaxws/          ← Plain Java 8 · JAX-WS · No Spring
│   ├── README.md   ← Full JAX-WS documentation
│   ├── docker-compose.yml
│   ├── book-server/      ← JAX-WS SOAP server
│   ├── book-client/      ← wsimport-generated stubs client
│   └── dev-tools/        ← Interactive container (wsimport + Ant)
│
└── springboot/     ← Java 17 · Spring Boot · Spring-WS
    ├── README.md   ← Full Spring Boot documentation
    ├── docker-compose.yml
    ├── spring-book-server/   ← Spring Boot SOAP server
    └── spring-book-client/   ← WebServiceTemplate client
```

Each implementation is self-contained with its own `docker-compose.yml`.
Run them independently — they do not depend on each other.

---

## Quick Start

### JAX-WS version
```bash
cd jaxws
docker compose up --build
```
- Server endpoints: `http://localhost:8080/getbook` · `http://localhost:8080/createbook`
- WSDLs: `http://localhost:8080/getbook?wsdl` · `http://localhost:8080/createbook?wsdl`

### Spring Boot version
```bash
cd springboot
docker compose up --build
```
- Server endpoints: `http://localhost:8080/ws/getbook` · `http://localhost:8080/ws/createbook`
- WSDLs: `http://localhost:8080/ws/getbook.wsdl` · `http://localhost:8080/ws/createbook.wsdl`

---

## The Service

Both versions implement the **Book Service**: a simple catalog of books with two operations.

### Operations

| Operation | Input | Output | Fault |
|---|---|---|---|
| `GetBook` | ISBN string | Full `BookType` object | `BOOK_NOT_FOUND` if ISBN doesn't exist |
| `CreateBook` | `BookType` object | `bookId`, `status` (`CREATED` / `DUPLICATE`) | `INVALID_INPUT` if data is missing |

### Authentication

Every SOAP call must include a `WsCredentials` header:

```xml
<soapenv:Header>
  <cmn:WsCredentials xmlns:cmn="http://bookservice.com/common/v1">
    <cmn:username>bookapp</cmn:username>
    <cmn:password>secret123</cmn:password>
    <cmn:systemId>MY_APP</cmn:systemId>
  </cmn:WsCredentials>
</soapenv:Header>
```

### Pre-loaded catalog

Both servers start with the same three books:

| ISBN | Title | Author | Price |
|---|---|---|---|
| 978-0-13-468599-1 | Effective Java | Joshua Bloch | $49.99 |
| 978-0-13-110362-7 | The Pragmatic Programmer | David Thomas, Andrew Hunt | $52.99 |
| 978-0-596-51774-8 | JavaScript: The Good Parts | Douglas Crockford | $29.99 |

---

## Framework Comparison

| Concern | JAX-WS | Spring-WS |
|---|---|---|
| Java version | 8 | 17 |
| Entry point | `Endpoint.publish()` in `main()` | `@SpringBootApplication` |
| Endpoint definition | `@WebService` impl + SEI interface | `@Endpoint` + `@PayloadRoot` |
| Routing | One URL per operation | One servlet, routed by payload root element |
| Auth | `SOAPHandler` + `handler-chain.xml` | `EndpointInterceptor` in config class |
| Client | `wsimport`-generated stubs | `WebServiceTemplate` |
| JAXB / SAAJ | Built into JDK | Explicit Maven dependencies (`jakarta.*`) |

---

## Shared Contract

The WSDL and XSD files are identical across both implementations — the contract is the single source of truth.

```
wsdl/
├── GetBook.wsdl     ← contract for GetBook operation
├── CreateBook.wsdl  ← contract for CreateBook operation
├── book.xsd         ← BookType, GetBookInput/Output, CreateBookInput/Output
└── common.xsd       ← WsFault, WsCredentials
```

Namespaces:

| Prefix | URI | Purpose |
|---|---|---|
| `bk` | `http://bookservice.com/book/v1` | Book domain types |
| `cmn` | `http://bookservice.com/common/v1` | Shared infra types (fault, credentials) |

---

## Detailed Documentation

- **JAX-WS** — full guide covering WSDL structure, SEI/SIB pattern, wsimport, handler chain, SOAP faults, curl examples, and more: [`jaxws/README.md`](jaxws/README.md)
- **Spring Boot** — full guide covering Spring-WS concepts, `@Endpoint`/`@PayloadRoot`, `WebServiceTemplate`, `EndpointInterceptor`, Java 17 dependency notes: [`springboot/README.md`](springboot/README.md)
