# Book Service — Spring Boot Implementation
## Java 17 · Maven · Docker · Spring Boot · Spring-WS

This is the Spring Boot implementation of the Book Service SOAP demo.
It exposes the **exact same WSDL contract** as the JAX-WS version — only the framework changes.
For the JAX-WS version see [`../jaxws/`](../jaxws/README.md).
For the project overview see [`../README.md`](../README.md).

---

## Table of Contents
1. [How Spring-WS Differs from JAX-WS](#how-spring-ws-differs-from-jax-ws)
2. [Project Structure](#project-structure)
3. [Key Components Explained](#key-components-explained)
4. [How to Run](#how-to-run)
5. [Testing with curl](#testing-with-curl)
6. [The SOAP Header (Credentials)](#the-soap-header-credentials)
7. [SOAP Fault Flow](#soap-fault-flow)
8. [Dependency Notes — Java 17](#dependency-notes--java-17)
9. [Adding a New Operation](#adding-a-new-operation)

---

## How Spring-WS Differs from JAX-WS

Both implementations serve the same WSDL and accept identical SOAP messages.
The difference is entirely in how the framework wires things together.

| Concern | JAX-WS (plain Java) | Spring-WS (Spring Boot) |
|---|---|---|
| Entry point | `ServerMain` + `Endpoint.publish()` | `@SpringBootApplication` + embedded Tomcat |
| Endpoint class | `@WebService` impl + SEI interface | `@Endpoint` bean, no interface needed |
| Operation routing | One URL path per operation (`/getbook`, `/createbook`) | One servlet path (`/ws/*`), routed by payload root element |
| WSDL serving | `wsdlLocation` in `@WebService` | `SimpleWsdl11Definition` `@Bean` in `WebServiceConfig` |
| WSDL URL | `GET /getbook?wsdl` | `GET /ws/getbook.wsdl` |
| Auth handler | JAX-WS `SOAPHandler` + `handler-chain.xml` | Spring-WS `EndpointInterceptor` in `WebServiceConfig` |
| Client | `wsimport`-generated stubs | `WebServiceTemplate` + hand-written JAXB models |
| Credentials injection | `SOAPHandler` on binding chain | `WebServiceMessageCallback` per call |
| JAXB namespace | `javax.xml.bind.*` (Java 8 built-in) | `jakarta.xml.bind.*` (explicit Maven dependency) |
| SOAP API (SAAJ) | `javax.xml.soap.*` (Java 8 built-in) | `jakarta.xml.soap.*` (explicit Maven dependency) |

### URL comparison

| | JAX-WS | Spring-WS |
|---|---|---|
| GetBook endpoint | `POST http://localhost:8080/getbook` | `POST http://localhost:8080/ws/getbook` |
| CreateBook endpoint | `POST http://localhost:8080/createbook` | `POST http://localhost:8080/ws/createbook` |
| GetBook WSDL | `GET  http://localhost:8080/getbook?wsdl` | `GET  http://localhost:8080/ws/getbook.wsdl` |
| CreateBook WSDL | `GET  http://localhost:8080/createbook?wsdl` | `GET  http://localhost:8080/ws/createbook.wsdl` |

---

## Project Structure

```
springboot/
│
├── docker-compose.yml
│
├── spring-book-server/                        ← Spring Boot SOAP server
│   ├── Dockerfile
│   ├── pom.xml                                ← spring-boot-starter-web-services + JAXB + SAAJ
│   └── src/main/
│       ├── resources/
│       │   ├── application.properties         ← server.port=8080
│       │   └── wsdl/
│       │       ├── GetBook.wsdl               ← same contract as JAX-WS version
│       │       ├── CreateBook.wsdl
│       │       ├── common.xsd
│       │       └── book.xsd
│       └── java/com/bookservice/springserver/
│           ├── SpringBookServerApplication.java  ← @SpringBootApplication entry point
│           ├── config/
│           │   └── WebServiceConfig.java         ← MessageDispatcherServlet + WSDL beans + interceptors
│           ├── interceptor/
│           │   └── CredentialsInterceptor.java   ← EndpointInterceptor (replaces handler-chain.xml)
│           ├── endpoint/
│           │   ├── GetBookEndpoint.java           ← @Endpoint + @PayloadRoot (replaces GetBookPortTypeImpl)
│           │   └── CreateBookEndpoint.java        ← @Endpoint + @PayloadRoot
│           ├── repository/
│           │   └── BookRepository.java            ← @Repository (Spring-managed singleton)
│           ├── fault/
│           │   └── WsException.java              ← @SoapFault exception
│           └── model/
│               ├── package-info.java
│               ├── BookType.java
│               ├── GetBookInput.java
│               ├── GetBookOutput.java
│               ├── CreateBookInput.java
│               ├── CreateBookOutput.java
│               └── WsFault.java
│
└── spring-book-client/                        ← Spring Boot SOAP client
    ├── Dockerfile
    ├── pom.xml                                ← spring-ws-core + JAXB + SAAJ
    └── src/main/
        ├── resources/
        │   └── application.properties         ← spring.main.web-application-type=none
        └── java/com/bookservice/springclient/
            ├── SpringBookClientApplication.java  ← @SpringBootApplication + CommandLineRunner
            ├── config/
            │   └── WebServiceClientConfig.java   ← Jaxb2Marshaller + WebServiceTemplate beans
            ├── callback/
            │   └── CredentialsCallback.java      ← WebServiceMessageCallback (replaces CredentialsInjector)
            ├── client/
            │   └── BookServiceClient.java        ← demo scenarios using WebServiceTemplate
            └── model/
                ├── package-info.java
                ├── BookType.java
                ├── GetBookInput.java / GetBookOutput.java
                ├── CreateBookInput.java / CreateBookOutput.java
                └── WsFault.java
```

---

## Key Components Explained

### WebServiceConfig — the control centre

`WebServiceConfig` replaces three things from the JAX-WS version in one class:

```java
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    // 1. Replaces Endpoint.publish() — registers the SOAP front controller servlet
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(...) { ... }

    // 2. Replaces wsdlLocation in @WebService — serves WSDL files from classpath
    //    Bean name "getbook" → available at GET /ws/getbook.wsdl
    @Bean(name = "getbook")
    public SimpleWsdl11Definition getBookWsdl() { ... }

    // 3. Replaces handler-chain.xml + @HandlerChain — wires the auth interceptor globally
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new CredentialsInterceptor());
    }
}
```

### @Endpoint + @PayloadRoot — operation routing

In JAX-WS, each operation had its own URL path (`/getbook`, `/createbook`).
In Spring-WS, all operations share `/ws/*` and are routed by the SOAP Body's root element:

```java
@Endpoint
public class GetBookEndpoint {

    // "when the Body contains <bk:GetBookInput>, call this method"
    @PayloadRoot(namespace = "http://bookservice.com/book/v1", localPart = "GetBookInput")
    @ResponsePayload
    public GetBookOutput getBook(@RequestPayload GetBookInput request) throws WsException {
        // business logic — identical to GetBookPortTypeImpl.getBook()
    }
}
```

### CredentialsInterceptor — auth without SAAJ

The interceptor uses only Spring-WS's own API — no `javax.xml.soap` imports:

```java
// Write a fault using Spring-WS abstraction (no SAAJ needed):
SoapBody body = ((SoapMessage) messageContext.getResponse()).getSoapBody();
body.addClientOrSenderFault(faultString, Locale.ENGLISH);

// Read the header element using javax.xml.transform (always in JDK):
Source source = headerElement.getSource();
DOMResult result = new DOMResult();
TransformerFactory.newInstance().newTransformer().transform(source, result);
```

### WebServiceTemplate — client without wsimport

The plain-Java client used `wsimport`-generated proxy objects:
```java
GetBookPortType port = new GetBookService(wsdlUrl).getGetBookPort();
GetBookOutput out = port.getBook(input);   // looks like a method call
```

The Spring-WS client uses `WebServiceTemplate` directly:
```java
GetBookOutput out = (GetBookOutput) webServiceTemplate.marshalSendAndReceive(
    "http://spring-book-server:8080/ws/getbook",
    new GetBookInput("978-0-13-468599-1"),
    new CredentialsCallback("bookapp", "secret123", "MY_APP")
);
```
No stubs, no code generation — just a template, a marshaller, and a callback.

---

## How to Run

```bash
cd springboot
docker compose up --build
```

### Expected client output
```
================================================
 Book Service SOAP Client  [Spring Boot / Spring-WS]
================================================

── Scenario 1: GetBook — existing ISBN ─────────
  ✓ Found: Effective Java
    Author : Joshua Bloch
    Price  : $49.99

── Scenario 2: GetBook — unknown ISBN ──────────
  ✓ Got expected fault: No book found with ISBN: 000-0-00-000000-0

── Scenario 3: GetBook — bad credentials ───────
  ✓ Got expected auth fault: Invalid credentials. Access denied.

── Scenario 4: CreateBook — new book ───────────
  ✓ Status  : CREATED
    Book ID : BK-00004
    Message : Book 'Clean Code' successfully registered with ID BK-00004

── Scenario 5: CreateBook — duplicate ISBN ──────
  ✓ Status  : DUPLICATE
    Message : A book with ISBN 978-0-13-468599-1 already exists.

================================================
 Demo complete.
================================================
```

### WSDLs (server must be running)
```
http://localhost:8080/ws/getbook.wsdl
http://localhost:8080/ws/createbook.wsdl
```

---

## Testing with curl

### GetBook — valid credentials
```bash
curl -s -X POST http://localhost:8080/ws/getbook \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: GetBook" \
  -d '
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cmn="http://bookservice.com/common/v1"
    xmlns:bk="http://bookservice.com/book/v1">
  <soapenv:Header>
    <cmn:WsCredentials>
      <cmn:username>bookapp</cmn:username>
      <cmn:password>secret123</cmn:password>
      <cmn:systemId>CURL_TEST</cmn:systemId>
    </cmn:WsCredentials>
  </soapenv:Header>
  <soapenv:Body>
    <bk:GetBookInput>
      <bk:isbn>978-0-13-468599-1</bk:isbn>
    </bk:GetBookInput>
  </soapenv:Body>
</soapenv:Envelope>'
```

### CreateBook — new book
```bash
curl -s -X POST http://localhost:8080/ws/createbook \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: CreateBook" \
  -d '
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cmn="http://bookservice.com/common/v1"
    xmlns:bk="http://bookservice.com/book/v1">
  <soapenv:Header>
    <cmn:WsCredentials>
      <cmn:username>bookapp</cmn:username>
      <cmn:password>secret123</cmn:password>
      <cmn:systemId>CURL_TEST</cmn:systemId>
    </cmn:WsCredentials>
  </soapenv:Header>
  <soapenv:Body>
    <bk:CreateBookInput>
      <bk:book>
        <bk:isbn>978-0-20-163361-0</bk:isbn>
        <bk:title>Design Patterns</bk:title>
        <bk:author>Gang of Four</bk:author>
        <bk:price>54.99</bk:price>
        <bk:stockQuantity>20</bk:stockQuantity>
        <bk:genre>Programming</bk:genre>
      </bk:book>
    </bk:CreateBookInput>
  </soapenv:Body>
</soapenv:Envelope>'
```

---

## The SOAP Header (Credentials)

Identical wire format to the JAX-WS version — the contract is the same:

```xml
<soapenv:Header>
  <cmn:WsCredentials xmlns:cmn="http://bookservice.com/common/v1">
    <cmn:username>bookapp</cmn:username>
    <cmn:password>secret123</cmn:password>
    <cmn:systemId>MY_APP</cmn:systemId>
  </cmn:WsCredentials>
</soapenv:Header>
```

**Valid credentials:** `username=bookapp` / `password=secret123`

**Server-side:** `CredentialsInterceptor.handleRequest()` reads the header before the endpoint method runs.
**Client-side:** `CredentialsCallback.doWithMessage()` adds the header to every outbound request.

---

## SOAP Fault Flow

```
GetBookEndpoint throws WsException
  → @SoapFault(faultCode = FaultCode.SERVER) on WsException
  → Spring-WS produces:

<soapenv:Fault>
  <faultcode>soapenv:Server</faultcode>
  <faultstring>No book found with ISBN: 978-x</faultstring>
</soapenv:Fault>

Client catches SoapFaultClientException:
  e.getFaultStringOrReason()  // "No book found with ISBN: 978-x"
```

Note: the plain-Java client receives a typed `WsException` with a full `WsFault` detail bean.
The Spring-WS client receives a `SoapFaultClientException` — the fault string carries the message.

---

## Dependency Notes — Java 17

Java 17 removed several APIs that were bundled in Java 8. All must be added explicitly:

| API | Old location | Maven dependency |
|---|---|---|
| JAXB | Built into JDK 8 | `jakarta.xml.bind:jakarta.xml.bind-api` + `com.sun.xml.bind:jaxb-impl` |
| SAAJ | Built into JDK 8 | `jakarta.xml.soap:jakarta.xml.soap-api` + `com.sun.xml.messaging.saaj:saaj-impl` |

Also note the namespace shift: `javax.*` → `jakarta.*` across all annotations and imports.

---

## Adding a New Operation

Example: add `DeleteBook`.

1. **`book.xsd`** — add `DeleteBookInput` and `DeleteBookOutput` elements (same namespace `bk:`).
2. **`DeleteBook.wsdl`** — new WSDL file following the same 5-section structure.
3. **Copy WSDL/XSD** into both `spring-book-server/src/main/resources/wsdl/` and `spring-book-client/src/main/resources/wsdl/`.
4. **Model classes** — `DeleteBookInput.java`, `DeleteBookOutput.java` in `model/` packages (both server and client).
5. **Server endpoint:**
   ```java
   @Endpoint
   public class DeleteBookEndpoint {
       @PayloadRoot(namespace = "http://bookservice.com/book/v1", localPart = "DeleteBookInput")
       @ResponsePayload
       public DeleteBookOutput deleteBook(@RequestPayload DeleteBookInput request) { ... }
   }
   ```
6. **`WebServiceConfig`** — add a new `SimpleWsdl11Definition` bean:
   ```java
   @Bean(name = "deletebook")
   public SimpleWsdl11Definition deleteBookWsdl() {
       SimpleWsdl11Definition def = new SimpleWsdl11Definition();
       def.setWsdl(new ClassPathResource("wsdl/DeleteBook.wsdl"));
       return def;
   }
   ```
7. **Client** — register new model classes in `Jaxb2Marshaller.setClassesToBeBound(...)` and call via `WebServiceTemplate`.
8. **Rebuild:** `docker compose up --build`

---

## Useful Links

- [Spring-WS Reference Documentation](https://docs.spring.io/spring-ws/docs/current/reference/)
- [Spring Boot + SOAP Web Service tutorial](https://spring.io/guides/gs/producing-web-service/)
- [Jakarta EE SOAP API (SAAJ)](https://jakarta.ee/specifications/soap-attachments/)
- [JAXB in Jakarta EE](https://jakarta.ee/specifications/xml-binding/)
