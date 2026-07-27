# Book Service — JAX-WS Implementation
## Java 8 · Maven · Docker · JAX-WS · No Spring

This is the plain-Java implementation of the Book Service SOAP demo.
For the Spring Boot version see [`../springboot/`](../springboot/README.md).
For the project overview see [`../README.md`](../README.md).

---

## Table of Contents
1. [What is SOAP?](#what-is-soap)
2. [What is a WSDL?](#what-is-a-wsdl)
3. [Key Concepts](#key-concepts)
4. [What This Project Demonstrates](#what-this-project-demonstrates)
5. [Project Structure](#project-structure)
6. [The Two Workflows](#the-two-workflows)
7. [How to Run](#how-to-run)
8. [Manual Workflow — Inside dev-tools](#manual-workflow--inside-dev-tools)
9. [File-by-File Relationships](#file-by-file-relationships)
10. [WSDL Sections Explained](#wsdl-sections-explained)
11. [Namespace Map](#namespace-map)
12. [The SOAP Header (Credentials)](#the-soap-header-credentials)
13. [WsException / SOAP Fault Flow](#wsexception--soap-fault-flow)
14. [Input / Output vs Request / Response](#input--output-vs-request--response)
15. [package-info.java Explained](#package-infojava-explained)
16. [Testing with curl](#testing-with-curl)
17. [Adding a New Operation](#adding-a-new-operation)

---

## What is SOAP?

SOAP (Simple Object Access Protocol) is a protocol for exchanging structured
information between services using XML over HTTP.

Every SOAP call has the same envelope structure:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Header/>   ← optional metadata (auth tokens, correlation IDs, etc.)
  <soapenv:Body>      ← the actual request or response
    ... your data ...
  </soapenv:Body>
</soapenv:Envelope>
```

**SOAP vs REST comparison:**

| Topic              | SOAP                              | REST                        |
|--------------------|-----------------------------------|-----------------------------|
| Message format     | Always XML                        | JSON, XML, anything         |
| Contract           | WSDL (mandatory, formal)          | OpenAPI/Swagger (optional)  |
| Type safety        | Strong (enforced by XSD schema)   | Weak (convention-based)     |
| Code generation    | wsimport generates Java stubs     | openapi-generator (optional)|
| Error handling     | Formal SOAP Fault elements        | HTTP status codes           |
| Typical use        | Banking, healthcare, enterprise   | Web APIs, microservices     |
| Transport          | Usually HTTP, but protocol-agnostic | HTTP only                 |

---

## What is a WSDL?

WSDL (Web Services Description Language) is an XML document that formally
describes a SOAP service. It is the CONTRACT between the server and any client.

A WSDL answers the question: **"How do I talk to this service?"**

It has 5 sections that chain together like this:

```
<types>     → defines XML element SHAPES (using XSD schema, or imports external XSD files)
    ↑ referenced by
<message>   → gives NAMES to groups of data parts
    ↑ referenced by
<portType>  → lists available OPERATIONS and their input/output messages
    ↑ referenced by
<binding>   → says HOW portType maps to SOAP over HTTP
    ↑ referenced by
<service>   → the concrete NETWORK ADDRESS (URL)
```

**Where does the WSDL come from?**

In this project, there are two WSDL sources:

1. **Pre-written (server):** The WSDLs are hand-crafted files in `src/main/resources/wsdl/`.
   JAX-WS serves them as-is via `wsdlLocation = "wsdl/GetBook.wsdl"`.
   This is the **contract-first** approach — the WSDL is the source of truth.

2. **Local copy (client):** `book-client/src/main/resources/wsdl/` contains copies
   of the same WSDL files. `wsimport` reads them at **build time** to generate
   Java client stubs, without needing the server to be running.

---

## Key Concepts

### SEI and SIB — the two Java roles

Every JAX-WS service is split into two Java classes:

```
GetBookPortType (SEI — Service Endpoint Interface)
  → Java interface annotated with @WebService
  → Defines WHAT operations exist and their signatures
  → Maps directly to <wsdl:portType> in the WSDL
  → This is the public contract — clients and wsimport only need this

GetBookPortTypeImpl (SIB — Service Implementation Bean)
  → Java class that implements the SEI
  → Contains the actual business logic
  → Annotated with @WebService(endpointInterface = "...GetBookPortType")
  → The endpointInterface attribute is the critical link back to the SEI
```

This separation is intentional:
- The SEI is the public API. You can share it with clients without exposing implementation details.
- The SIB is the private implementation. You can swap it without changing the contract.

### Marshalling and unmarshalling

When a client calls `getBookPort.getBook(input)`, JAX-WS does all the XML work invisibly:

```
CLIENT                                          SERVER
──────────────────────────────────────────────────────────────────────

Java call:                                      Java method:
  getBookPort.getBook(input)                      getBook(input)
       ↓ JAX-WS marshals (Java → XML)                  ↑ JAX-WS unmarshals (XML → Java)
  SOAP request XML sent via HTTP POST           SOAP request XML arrives
       ↓                                               ↓ business logic runs
  SOAP response XML arrives                     return new GetBookOutput(book)
       ↑ JAX-WS unmarshals (XML → Java)               ↑ JAX-WS marshals (Java → XML)
Java result:
  GetBookOutput output = ...
```

### wsimport — what it generates and why

`wsimport` is a JDK tool that reads a WSDL and generates Java client stub classes.
It runs automatically during `mvn generate-sources`.

```
GetBook.wsdl  →  wsimport  →  com.bookservice.generated.getbook.*
                                 GetBookService.java      ← factory (from <wsdl:service>)
                                 GetBookPortType.java     ← port interface (from <wsdl:portType>)
                                 GetBookInput.java        ← request bean (from book.xsd)
                                 GetBookOutput.java       ← response bean (from book.xsd)
                                 WsException.java         ← fault exception (from <wsdl:fault>)
                                 WsFault.java             ← fault data bean (from common.xsd)
                                 WsCredentials.java       ← header bean (from common.xsd)
                                 BookType.java            ← domain type (from book.xsd)
                                 ObjectFactory.java       ← JAXB factory
                                 package-info.java        ← JAXB namespace binding
```

The client uses them like this:
```java
// Factory reads the WSDL to know where to send requests
GetBookService factory = new GetBookService(new URL(wsdlUrl));

// Port is a PROXY — every method call becomes a SOAP HTTP request
GetBookPortType port = factory.getGetBookPort();

// This looks like a normal Java call but sends XML over HTTP
GetBookOutput output = port.getBook(input);
```

### Contract-first vs code-first

This project uses **contract-first**: the WSDL is written by hand, then the Java code is written to match it. The `wsdlLocation = "wsdl/GetBook.wsdl"` attribute on the SIB tells JAX-WS to serve the pre-written WSDL as-is.

The alternative is **code-first**: write the Java SEI with annotations, and JAX-WS auto-generates the WSDL. This is simpler but gives you less control over the WSDL structure.

---

## What This Project Demonstrates

| Feature | Where |
|---|---|
| Shared XSD file per domain | `common.xsd` (shared types), `book.xsd` (all book operations) |
| XSD imported into WSDL (not inline) | `<xsd:import schemaLocation="..."/>` in each WSDL |
| One WSDL per operation | `GetBook.wsdl`, `CreateBook.wsdl` |
| PortType suffix on SEI class names | `GetBookPortType`, `CreateBookPortType` |
| SOAP Header credentials | `WsCredentials` element in `<soapenv:Header>` |
| Credentials validated by SOAPHandler | `CredentialsHandler.java` + `handler-chain.xml` |
| Named Input/Output elements (XSD level) | `GetBookInput`, `GetBookOutput`, `CreateBookInput`, `CreateBookOutput` |
| Named Request/Response messages (WSDL level) | `GetBookRequest`, `GetBookResponse` |
| `WsException` → SOAP Fault | `WsException.java` + `WsFault.java` |
| `package-info.java` | `com/bookservice/ws/model/package-info.java` |
| Auto-generated client (Maven wsimport) | `book-client/pom.xml` — two executions |
| Manual wsimport + Ant compile | `dev-tools/` container |

---

## Project Structure

```
jaxws/
│
├── docker-compose.yml
│
├── book-server/                          ← SOAP service (hand-written)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── handler-chain.xml         ← wires CredentialsHandler to endpoints
│       │   └── wsdl/
│       │       ├── GetBook.wsdl
│       │       ├── CreateBook.wsdl
│       │       ├── common.xsd
│       │       └── book.xsd
│       └── java/com/bookservice/ws/
│           ├── ServerMain.java           ← publishes both endpoints
│           ├── fault/
│           │   ├── WsFault.java
│           │   └── WsException.java
│           ├── handler/
│           │   └── CredentialsHandler.java
│           ├── impl/
│           │   ├── BookRepository.java
│           │   ├── GetBookPortTypeImpl.java
│           │   └── CreateBookPortTypeImpl.java
│           ├── model/
│           │   ├── package-info.java
│           │   ├── BookType.java
│           │   ├── WsCredentials.java
│           │   ├── GetBookInput.java
│           │   ├── GetBookOutput.java
│           │   ├── CreateBookInput.java
│           │   └── CreateBookOutput.java
│           └── porttype/
│               ├── GetBookPortType.java
│               └── CreateBookPortType.java
│
├── book-client/                          ← Auto-generated client (Maven + wsimport)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── resources/wsdl/               ← local WSDL/XSD copies for build-time wsimport
│       └── java/com/bookservice/client/
│           └── BookClient.java
│
└── dev-tools/                            ← Manual workflow container
    ├── Dockerfile
    ├── ant/build.xml
    ├── wsdl/
    └── client-src/
```

---

## The Two Workflows

```
                ┌─────────────────────────────────────────────────────┐
                │            WSDL + XSD files (the contract)          │
                └───────────────────┬─────────────────────────────────┘
                                    │
               ┌────────────────────┴────────────────────┐
               │                                         │
               ▼                                         ▼
   ┌────────────────────────┐              ┌───────────────────────────┐
   │   WORKFLOW 1 (AUTO)    │              │   WORKFLOW 2 (MANUAL)     │
   │   book-client/         │              │   dev-tools/ container    │
   │                        │              │                           │
   │  Maven build           │              │  docker exec -it          │
   │    → jaxws-maven-plugin│              │    dev-tools bash         │
   │    → wsimport runs     │              │                           │
   │    → stubs generated   │              │  ant wsimport-getbook     │
   │    → javac compiles    │              │  ant wsimport-createbook  │
   │    → fat JAR built     │              │  ant compile              │
   │    → java -jar         │              │  ant run                  │
   └────────────────────────┘              └───────────────────────────┘
```

---

## How to Run

```bash
cd jaxws
docker compose up --build
```

### Expected client output
```
================================================
 Book Service SOAP Client
================================================

── GetBook Service ──────────────────────────────
[1a] GetBook - existing book (ISBN: 978-0-13-468599-1)
  Title   : Effective Java
  Author  : Joshua Bloch
  Price   : 49.99

[1b] GetBook - non-existent book
  Expected fault: BOOK_NOT_FOUND

[1c] GetBook - invalid credentials
  Auth rejected (expected)

── CreateBook Service ───────────────────────────
[2a] CreateBook - new book
  Status : CREATED
  Book ID: BK-00004

[2b] CreateBook - duplicate ISBN
  Status : DUPLICATE
================================================
```

### WSDLs (server must be running)
```
http://localhost:8080/getbook?wsdl
http://localhost:8080/createbook?wsdl
```

---

## Manual Workflow — Inside dev-tools

```bash
# Connect to the dev-tools container
docker exec -it dev-tools bash

# Run everything at once
ant all

# Or step by step:
ant wsimport-getbook       # generate stubs from GetBook.wsdl
ant wsimport-createbook    # generate stubs from CreateBook.wsdl
ant compile                # compile everything
ant run                    # run the client

# Raw wsimport (no Ant):
wsimport -keep -verbose \
  -p com.bookservice.generated.getbook \
  -s /workspace/generated-src/getbook \
  /wsdl/GetBook.wsdl

# Clean up:
ant clean
```

---

## File-by-File Relationships

```
common.xsd
  └─ defines: WsFault, WsCredentials
  └─ imported by: GetBook.wsdl, CreateBook.wsdl
  └─ hand-mapped to Java: WsFault.java, WsCredentials.java

book.xsd
  └─ defines: BookType, GetBookInput, GetBookOutput, CreateBookInput, CreateBookOutput
  └─ imported by: GetBook.wsdl, CreateBook.wsdl
  └─ hand-mapped to Java: BookType.java, GetBook*.java, CreateBook*.java

GetBook.wsdl
  └─ imports XSD: book.xsd + common.xsd
  └─ defines portType: GetBookPortType → operation: GetBook
  └─ hand-mapped SEI: GetBookPortType.java (interface)
  └─ implemented by: GetBookPortTypeImpl.java
  └─ wsimport generates: GetBookService.java, GetBookPortType.java, ...

handler-chain.xml
  └─ lists: CredentialsHandler
  └─ referenced by: @HandlerChain on GetBookPortTypeImpl, CreateBookPortTypeImpl

WsException.java
  └─ @WebFault links to: WsFault.java
  └─ thrown by: GetBookPortTypeImpl, CreateBookPortTypeImpl
  └─ becomes: <soapenv:Fault><detail><cmn:WsFault>...</cmn:WsFault></detail></soapenv:Fault>
```

---

## WSDL Sections Explained

```xml
<wsdl:definitions targetNamespace="http://bookservice.com/getbook/wsdl/v1" ...>

  <!-- 1. TYPES — imports external XSD files -->
  <wsdl:types>
    <xsd:schema>
      <xsd:import namespace="http://bookservice.com/book/v1"   schemaLocation="book.xsd"/>
      <xsd:import namespace="http://bookservice.com/common/v1" schemaLocation="common.xsd"/>
    </xsd:schema>
  </wsdl:types>

  <!-- 2. MESSAGES — name the data packages -->
  <wsdl:message name="GetBookRequest">
    <wsdl:part element="bk:GetBookInput" name="GetBookInput"/>
  </wsdl:message>
  <wsdl:message name="GetBookResponse">
    <wsdl:part element="bk:GetBookOutput" name="GetBookOutput"/>
  </wsdl:message>
  <wsdl:message name="WsException">
    <wsdl:part element="cmn:WsFault" name="WsFault"/>
  </wsdl:message>

  <!-- 3. PORT TYPE — abstract interface -->
  <wsdl:portType name="GetBookPortType">
    <wsdl:operation name="GetBook">
      <wsdl:input  message="tns:GetBookRequest"  name="GetBookRequest"/>
      <wsdl:output message="tns:GetBookResponse" name="GetBookResponse"/>
      <wsdl:fault  message="tns:WsException"     name="WsException"/>
    </wsdl:operation>
  </wsdl:portType>

  <!-- 4. BINDING — maps portType to SOAP/HTTP -->
  <wsdl:binding name="GetBookBinding" type="tns:GetBookPortType">
    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="GetBook">
      <soap:operation soapAction="GetBook"/>
      <wsdl:input  name="GetBookRequest">  <soap:body use="literal"/> </wsdl:input>
      <wsdl:output name="GetBookResponse"> <soap:body use="literal"/> </wsdl:output>
      <wsdl:fault  name="WsException">     <soap:fault name="WsException" use="literal"/> </wsdl:fault>
    </wsdl:operation>
  </wsdl:binding>

  <!-- 5. SERVICE — the concrete URL -->
  <wsdl:service name="GetBookService">
    <wsdl:port binding="tns:GetBookBinding" name="GetBookPort">
      <soap:address location="REPLACE_WITH_ACTUAL_URL"/>
    </wsdl:port>
  </wsdl:service>

</wsdl:definitions>
```

---

## Namespace Map

| Prefix | Namespace URI | Defined in | Used for |
|--------|--------------|------------|----------|
| `cmn`  | `http://bookservice.com/common/v1` | `common.xsd` | `WsFault`, `WsCredentials` |
| `bk`   | `http://bookservice.com/book/v1` | `book.xsd` | `BookType`, Input/Output types |
| `tns`  | WSDL's own `targetNamespace` | Each `.wsdl` file | References within same WSDL |
| `soap` | `http://schemas.xmlsoap.org/wsdl/soap/` | W3C spec | `soap:binding`, `soap:body` |
| `wsdl` | `http://schemas.xmlsoap.org/wsdl/` | W3C spec | All WSDL structural elements |
| `xsd`  | `http://www.w3.org/2001/XMLSchema` | W3C spec | `xsd:string`, `xsd:import` |

---

## The SOAP Header (Credentials)

Every call must carry a `WsCredentials` element in the SOAP `<Header>`:

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cmn="http://bookservice.com/common/v1"
    xmlns:bk="http://bookservice.com/book/v1">
  <soapenv:Header>
    <cmn:WsCredentials>
      <cmn:username>bookapp</cmn:username>
      <cmn:password>secret123</cmn:password>
      <cmn:systemId>MY_APP</cmn:systemId>
    </cmn:WsCredentials>
  </soapenv:Header>
  <soapenv:Body>
    <bk:GetBookInput>
      <bk:isbn>978-0-13-468599-1</bk:isbn>
    </bk:GetBookInput>
  </soapenv:Body>
</soapenv:Envelope>
```

**Valid credentials for this demo:**
```
username: bookapp
password: secret123
systemId: (any string)
```

**Server-side validation:** `CredentialsHandler` implements `SOAPHandler<SOAPMessageContext>`,
wired via `@HandlerChain(file="/handler-chain.xml")` on each endpoint implementation class.

---

## WsException / SOAP Fault Flow

```
SERVER throws:
  throw new WsException("No book found with ISBN: 978-x", faultInfo);
          │
          ▼  JAX-WS serializes to:
<soapenv:Fault>
  <faultcode>soapenv:Server</faultcode>
  <faultstring>No book found with ISBN: 978-x</faultstring>
  <detail>
    <cmn:WsFault xmlns:cmn="http://bookservice.com/common/v1">
      <cmn:errorCode>BOOK_NOT_FOUND</cmn:errorCode>
      <cmn:errorMessage>No book found with ISBN: 978-x</cmn:errorMessage>
      <cmn:errorTimestamp>2024-01-15T10:30:00.000+02:00</cmn:errorTimestamp>
      <cmn:operationName>GetBook</cmn:operationName>
    </cmn:WsFault>
  </detail>
</soapenv:Fault>
          │
          ▼  JAX-WS deserializes on client to:
CLIENT catches:
  catch (WsException e) {
    e.getMessage()                     // "No book found with ISBN: 978-x"
    e.getFaultInfo().getErrorCode()    // "BOOK_NOT_FOUND"
    e.getFaultInfo().getOperationName()// "GetBook"
  }
```

---

## Input / Output vs Request / Response

| Level | Name pattern | Example | Where |
|---|---|---|---|
| XSD (data shape) | `OperationInput` | `GetBookInput` | `book.xsd` |
| XSD (data shape) | `OperationOutput` | `GetBookOutput` | `book.xsd` |
| WSDL (message) | `OperationRequest` | `GetBookRequest` | `GetBook.wsdl` `<wsdl:message>` |
| WSDL (message) | `OperationResponse` | `GetBookResponse` | `GetBook.wsdl` `<wsdl:message>` |

---

## package-info.java Explained

```java
@XmlSchema(
    namespace = "http://bookservice.com/common/v1",
    elementFormDefault = XmlNsForm.QUALIFIED,
    xmlns = {
        @XmlNs(prefix = "cmn", namespaceURI = "http://bookservice.com/common/v1"),
        @XmlNs(prefix = "bk",  namespaceURI = "http://bookservice.com/book/v1")
    }
)
package com.bookservice.ws.model;
```

**Without it:** JAXB serialises elements without namespace — XML validation fails.
**With it:** JAXB uses `cmn:`, `bk:` prefixes correctly, producing valid SOAP XML.

---

## Testing with curl

### GetBook — valid credentials
```bash
curl -s -X POST http://localhost:8080/getbook \
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
curl -s -X POST http://localhost:8080/createbook \
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

## Adding a New Operation

Example: add `DeleteBook`.

1. **`book.xsd`** — add `DeleteBookInput` and `DeleteBookOutput` elements.
2. **`DeleteBook.wsdl`** — new WSDL file, same 5-section structure as `GetBook.wsdl`.
3. **Model classes** — `DeleteBookInput.java`, `DeleteBookOutput.java`.
4. **SEI** — `DeleteBookPortType.java`.
5. **SIB** — `DeleteBookPortTypeImpl.java` with `@HandlerChain(file="/handler-chain.xml")`.
6. **`ServerMain.java`** — add `DeleteBookPortTypeImpl.publish("http://0.0.0.0:8080/deletebook")`.
7. **`book-client/pom.xml`** — add a third wsimport execution for `DeleteBook.wsdl`.
8. **Rebuild:** `docker compose up --build`

---

## Useful Links

- [XSD vs WSDL: What's the difference?](https://www.tutorialworks.com/xsd-vs-wsdl/)
- [The simple guide to WSDL (with an example)](https://www.tutorialworks.com/wsdl/)
- [XML Schema Tutorial](https://www.w3schools.com/xml/schema_intro.asp)
- [JAX-WS Reference Implementation](https://eclipse-ee4j.github.io/metro-jax-ws/)
