# Book Service — SOAP Web Service Demo

This project demonstrates how a real-world **SOAP-based web service** works using **JAX-WS**, **WSDL**, and **JAXB** in plain Java without Spring or heavyweight frameworks.

The project includes:
- Contract-first SOAP development
- XML schema validation
- SOAP headers and authentication
- Custom SOAP faults
- Generated client stubs
- Dockerized development workflow

Everything runs inside Docker for a fully reproducible environment.

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

When a client calls `getBookPort.getBook(input, creds)`, JAX-WS does all the XML work invisibly:

```
CLIENT                                          SERVER
──────────────────────────────────────────────────────────────────────

Java call:                                      Java method:
  getBookPort.getBook(input, creds)               getBook(input, creds)
       ↓ JAX-WS marshals (Java → XML)                  ↑ JAX-WS unmarshals (XML → Java)
  SOAP request XML:                             SOAP request XML arrives:
    <soapenv:Envelope>                            <soapenv:Envelope>
      <soapenv:Header>                              <soapenv:Header>
        <cmn:WsCredentials>...</cmn:WsCredentials>    <cmn:WsCredentials>...</cmn:WsCredentials>
      </soapenv:Header>                             </soapenv:Header>
      <soapenv:Body>                                <soapenv:Body>
        <bk:GetBookInput>                             <bk:GetBookInput>
          <bk:isbn>978-x</bk:isbn>                      <bk:isbn>978-x</bk:isbn>
        </bk:GetBookInput>                            </bk:GetBookInput>
      </soapenv:Body>                               </soapenv:Body>
    </soapenv:Envelope>                           </soapenv:Envelope>
       ↓ HTTP POST to :8080/getbook                     ↓ business logic runs
                                                        ↓ return new GetBookOutput(book)
  SOAP response XML arrives:                    SOAP response XML:
    <soapenv:Envelope>                            <soapenv:Envelope>
      <soapenv:Body>                                <soapenv:Body>
        <bk:GetBookOutput>                            <bk:GetBookOutput>
          <bk:book>...</bk:book>                        <bk:book>...</bk:book>
        </bk:GetBookOutput>                           </bk:GetBookOutput>
      </soapenv:Body>                               </soapenv:Body>
    </soapenv:Envelope>                           </soapenv:Envelope>
       ↑ JAX-WS unmarshals (XML → Java)                ↑ JAX-WS marshals (Java → XML)
Java result:
  GetBookOutput output = ...
```

### wsimport — what it generates and why

`wsimport` is a JDK tool that reads a WSDL and generates Java client stub classes. It runs automatically during `mvn generate-sources`.

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
GetBookOutput output = port.getBook(input, creds);
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
soap-bookservice/
│
├── docker-compose.yml
│
├── book-server/                          ← SOAP service (hand-written)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── handler-chain.xml         ← wires CredentialsHandler to endpoints
│       │   ├── wsdl/
│       │   │   ├── GetBook.wsdl          ← contract for GetBook operation
│       │   │   ├── CreateBook.wsdl       ← contract for CreateBook operation
│       │   │   ├── common.xsd            ← WsFault, WsCredentials
│       │   │   └── book.xsd              ← BookType, GetBookInput/Output, CreateBookInput/Output
│       │
│       └── java/com/bookservice/ws/
│           ├── ServerMain.java           ← publishes both endpoints
│           ├── fault/
│           │   ├── WsFault.java          ← JAXB bean (matches common.xsd WsFault)
│           │   └── WsException.java      ← @WebFault exception thrown by operations
│           ├── handler/
│           │   └── CredentialsHandler.java ← SOAPHandler: validates header credentials
│           ├── impl/
│           │   ├── BookRepository.java   ← in-memory data store
│           │   ├── GetBookPortTypeImpl.java   ← SIB for GetBook
│           │   └── CreateBookPortTypeImpl.java ← SIB for CreateBook
│           ├── model/
│           │   ├── package-info.java     ← JAXB namespace binding for this package
│           │   ├── BookType.java         ← matches book.xsd BookType
│           │   ├── WsCredentials.java    ← matches common.xsd WsCredentials
│           │   ├── GetBookInput.java     ← matches book.xsd GetBookInput
│           │   ├── GetBookOutput.java    ← matches book.xsd GetBookOutput
│           │   ├── CreateBookInput.java  ← matches book.xsd CreateBookInput
│           │   └── CreateBookOutput.java ← matches book.xsd CreateBookOutput
│           └── porttype/
│               ├── GetBookPortType.java  ← SEI: mirrors GetBook.wsdl portType
│               └── CreateBookPortType.java ← SEI: mirrors CreateBook.wsdl portType
│
├── book-client/                          ← Auto-generated client (Maven)
│   ├── Dockerfile
│   ├── pom.xml                           ← two wsimport executions
│   └── src/main/
│       ├── resources/
│       │   ├── wsdl/ (common.xsd, book.xsd, GetBook.wsdl, CreateBook.wsdl)  ← local copies for build-time wsimport
│       └── java/com/bookservice/client/
│           └── BookClient.java           ← calls both operations, handles faults
│
└── dev-tools/                            ← Manual workflow container
    ├── Dockerfile                        ← JDK8 + wsimport + Ant
    ├── ant/build.xml                     ← Ant targets: wsimport → compile → run
    ├── wsdl/  (copies of XSD and WSDL files)
    └── client-src/ (copy of BookClient.java)
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
   │    → javac compiles    │              │    → wsimport runs        │
   │    → fat JAR built     │              │  ant wsimport-createbook  │
   │    → java -jar         │              │    → wsimport runs        │
   └────────────────────────┘              │  ant compile              │
                                           │    → javac compiles       │
                                           │  ant run                  │
                                           │    → java runs            │
                                           └───────────────────────────┘
```

**Workflow 1** is what you use in production CI/CD pipelines. <br>
**Workflow 2** is for learning every step, debugging, or when you need to inspect generated code.

---

## How to Run

### Start everything (server + auto client + dev-tools)
```bash
cd soap-bookservice
docker-compose up --build
```

### Expected auto-client output
```
================================================
 Book Service SOAP Client
================================================
Waiting for server... Server ready!

── GetBook Service ──────────────────────────────
[1a] GetBook - existing book (ISBN: 978-0-13-468599-1)
  ISBN    : 978-0-13-468599-1
  Title   : Effective Java
  Author  : Joshua Bloch
  Price   : 49.99
  Stock   : 15
  Genre   : Programming

[1b] GetBook - non-existent book
Expected fault received:
  faultstring : No book found with ISBN: 000-0-00-000000-0
  errorCode   : BOOK_NOT_FOUND
  errorMessage: No book found with ISBN: 000-0-00-000000-0
  operation   : GetBook

[1c] GetBook - invalid credentials
Auth rejected (expected): ...

── CreateBook Service ───────────────────────────
[2a] CreateBook - new book
Status : CREATED
Book ID: BK-00004
Message: Book 'Clean Code' successfully registered ...

[2b] CreateBook - duplicate ISBN
Status : DUPLICATE
Message: A book with ISBN 978-0-13-468599-1 already exists.

================================================
 All demos completed.
================================================
```

### Browse WSDLs from your machine (server must be running)
```
http://localhost:8080/getbook?wsdl
http://localhost:8080/createbook?wsdl
```

---

## Manual Workflow — Inside dev-tools

Once `docker-compose up` has started the server:

```bash
# Connect to the dev-tools container
docker exec -it dev-tools bash

# You are now inside the container. The server is reachable as "book-server".

# ── Option A: Run everything at once ──────────────────────────────────────
ant all
# This runs: wsimport-getbook → wsimport-createbook → compile → run

# ── Option B: Step by step ────────────────────────────────────────────────

# Step 1: Generate stubs from GetBook.wsdl
ant wsimport-getbook
# Generated files appear in: /workspace/generated-src/getbook/
ls /workspace/generated-src/getbook/com/bookservice/generated/getbook/

# Step 2: Generate stubs from CreateBook.wsdl
ant wsimport-createbook
ls /workspace/generated-src/createbook/com/bookservice/generated/createbook/

# Step 3: Compile everything
ant compile
# .class files appear in: /workspace/build/

# Step 4: Run the client
ant run

# ── Option C: Raw wsimport command (no Ant) ───────────────────────────────
# You can also call wsimport directly:
wsimport -keep -verbose \
  -p com.bookservice.generated.getbook \
  -s /workspace/generated-src/getbook \
  /wsdl/GetBook.wsdl

# ── Option D: See what wsimport generated ────────────────────────────────
find /workspace/generated-src -name "*.java" | sort
cat /workspace/generated-src/getbook/com/bookservice/generated/getbook/GetBookService.java

# ── Option E: Compile and run manually without Ant ───────────────────────
# Compile just the stubs:
javac -d /workspace/build \
  $(find /workspace/generated-src -name "*.java")

# Compile the client code against the stubs:
javac -d /workspace/build \
  -cp /workspace/build \
  /workspace/client-src/com/bookservice/client/BookClient.java

# Run the client:
java -cp /workspace/build \
  -Dserver.host=book-server \
  com.bookservice.client.BookClient

# ── Clean up and start over ───────────────────────────────────────────────
ant clean
```

---

## File-by-File Relationships

This table shows how every file connects to the others:

```
common.xsd
  └─ defines: WsFault, WsCredentials
  └─ imported by: GetBook.wsdl, CreateBook.wsdl
  └─ hand-mapped to Java: WsFault.java, WsCredentials.java
  └─ package-info.java controls JAXB namespace for these classes

book.xsd
  └─ defines: BookType, GetBookInput, GetBookOutput, CreateBookInput, CreateBookOutput
  └─ imported by: GetBook.wsdl, CreateBook.wsdl (both import the same file)
  └─ hand-mapped to Java: BookType.java,
                           GetBookInput.java, GetBookOutput.java,
                           CreateBookInput.java, CreateBookOutput.java

GetBook.wsdl
  └─ imports XSD: book.xsd + common.xsd
  └─ defines messages: GetBookRequest, GetBookResponse, WsException
  └─ defines portType: GetBookPortType → operation: GetBook
  └─ defines binding: GetBookBinding
  └─ defines service: GetBookService → port: GetBookPort
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
<wsdl:definitions targetNamespace="http://bookservice.com/getbook/wsdl/v1"
                  xmlns:tns="http://bookservice.com/getbook/wsdl/v1"
                  xmlns:bk="http://bookservice.com/book/v1"
                  xmlns:cmn="http://bookservice.com/common/v1">

  <!-- 1. TYPES — imports external XSD files instead of defining types inline -->
  <wsdl:types>
    <xsd:schema>
      <xsd:import namespace="http://bookservice.com/book/v1"
                  schemaLocation="book.xsd"/>
      <xsd:import namespace="http://bookservice.com/common/v1"
                  schemaLocation="common.xsd"/>
    </xsd:schema>
  </wsdl:types>

  <!-- 2. MESSAGES — name the data packages used by operations -->
  <wsdl:message name="GetBookRequest">       ← WSDL-level name (the envelope concept)
    <wsdl:part element="bk:GetBookInput"     ← XSD-level element (the data shape)
               name="GetBookInput"/>
  </wsdl:message>
  <wsdl:message name="GetBookResponse">
    <wsdl:part element="bk:GetBookOutput" name="GetBookOutput"/>
  </wsdl:message>
  <wsdl:message name="WsException">          ← fault message
    <wsdl:part element="cmn:WsFault" name="WsFault"/>
  </wsdl:message>

  <!-- 3. PORT TYPE — abstract interface; maps to Java SEI interface -->
  <wsdl:portType name="GetBookPortType">     ← name ends in PortType → Java interface
    <wsdl:operation name="GetBook">
      <wsdl:input  message="tns:GetBookRequest"  name="GetBookRequest"/>
      <wsdl:output message="tns:GetBookResponse" name="GetBookResponse"/>
      <wsdl:fault  message="tns:WsException"     name="WsException"/>
    </wsdl:operation>
  </wsdl:portType>

  <!-- 4. BINDING — maps portType to SOAP/HTTP with soapAction per operation -->
  <wsdl:binding name="GetBookBinding" type="tns:GetBookPortType">
    <soap:binding style="document"
                  transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="GetBook">
      <soap:operation soapAction="GetBook"/>   ← non-empty soapAction (enterprise standard)
      <wsdl:input  name="GetBookRequest">  <soap:body use="literal"/> </wsdl:input>
      <wsdl:output name="GetBookResponse"> <soap:body use="literal"/> </wsdl:output>
      <wsdl:fault  name="WsException">
        <soap:fault name="WsException" use="literal"/>
      </wsdl:fault>
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

| Prefix | Namespace URI | Defined in | Used for                                                                          |
|--------|--------------|------------|-----------------------------------------------------------------------------------|
| `cmn`  | `http://bookservice.com/common/v1` | `common.xsd` | `WsFault`, `WsCredentials`                                                        |
| `bk`   | `http://bookservice.com/book/v1` | `book.xsd` | `BookType`, `GetBookInput`, `GetBookOutput`, `CreateBookInput`, `CreateBookOutput` |
| `tns`  | WSDL's own `targetNamespace` | Each `.wsdl` file | References within the same WSDL                                                   |
| `soap` | `http://schemas.xmlsoap.org/wsdl/soap/` | W3C spec | `soap:binding`, `soap:body`, `soap:address`                                       |
| `wsdl` | `http://schemas.xmlsoap.org/wsdl/` | W3C spec | All WSDL structural elements                                                      |
| `xsd`  | `http://www.w3.org/2001/XMLSchema` | W3C spec | `xsd:string`, `xsd:int`, `xsd:import`                                             |

---

## The SOAP Header (Credentials)

Every call must carry a `WsCredentials` element in the SOAP `<Header>`:

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:cmn="http://bookservice.com/common/v1"
    xmlns:bk="http://bookservice.com/book/v1">

  <!-- ← HEADER: authentication travels here, NOT in the body -->
  <soapenv:Header>
    <cmn:WsCredentials>
      <cmn:username>bookapp</cmn:username>
      <cmn:password>secret123</cmn:password>
      <cmn:systemId>MY_APP</cmn:systemId>
    </cmn:WsCredentials>
  </soapenv:Header>

  <!-- ← BODY: business data travels here -->
  <soapenv:Body>
    <bk:GetBookInput>
      <bk:isbn>978-0-13-468599-1</bk:isbn>
    </bk:GetBookInput>
  </soapenv:Body>

</soapenv:Envelope>
```

**Server-side flow:**
```
Incoming request
    ↓
CredentialsHandler.handleMessage()   ← reads <cmn:WsCredentials> from Header
    ↓ (if valid)
GetBookPortTypeImpl.getBook(input, credentials)   ← method called with both params
    ↓
Response sent
```

**Valid credentials for this demo:**
```
username: bookapp
password: secret123
systemId: (any string)
```

---

## WsException / SOAP Fault Flow

```
SERVER throws:
  throw new WsException("No book found with ISBN: 978-x", faultInfo);
          │
          │  JAX-WS serializes to:
          ▼
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
          │  JAX-WS deserializes on client to:
          ▼
CLIENT catches:
  catch (WsException e) {
    e.getMessage()                    // "No book found with ISBN: 978-x"
    e.getFaultInfo().getErrorCode()   // "BOOK_NOT_FOUND"
    e.getFaultInfo().getErrorMessage()// "No book found with ISBN: 978-x"
    e.getFaultInfo().getOperationName()// "GetBook"
  }
```

---

## Input / Output vs Request / Response

This naming convention is a real-world enterprise pattern:

| Level | Name pattern | Example | Where it lives |
|---|---|---|---|
| XSD (data shape) | `OperationInput` | `GetBookInput` | Inside `book.xsd` |
| XSD (data shape) | `OperationOutput` | `GetBookOutput` | Inside `book.xsd` |
| WSDL (message envelope) | `OperationRequest` | `GetBookRequest` | Inside `GetBook.wsdl` `<wsdl:message>` |
| WSDL (message envelope) | `OperationResponse` | `GetBookResponse` | Inside `GetBook.wsdl` `<wsdl:message>` |

The `<wsdl:message name="GetBookRequest">` wraps `<wsdl:part element="bk:GetBookInput">`.
The message is the **envelope concept** (this is a request); the element is the **data concept** (this is the input data shape). Keeping these names different prevents confusion in large projects with many operations.

---

## package-info.java Explained

`package-info.java` is a special Java file that holds **package-level annotations**.
It must be named exactly `package-info.java` and placed in the package directory.

```java
@XmlSchema(
    namespace = "http://bookservice.com/common/v1",   // default namespace for this package
    elementFormDefault = XmlNsForm.QUALIFIED,          // all elements must be namespace-qualified
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

The server must be running (`docker-compose up`).

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

### GetBook — wrong credentials (triggers auth fault)
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
      <cmn:username>hacker</cmn:username>
      <cmn:password>wrong</cmn:password>
      <cmn:systemId>X</cmn:systemId>
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

Example: add `DeleteBook` operation.

**1. Add to `common.xsd`** (if new shared types are needed — none here).

**2. Add to `book.xsd`** — add the new Input/Output elements:
```xml
<xsd:element name="DeleteBookInput" type="tns:DeleteBookInputType"/>
<xsd:complexType name="DeleteBookInputType">
  <xsd:sequence>
    <xsd:element name="isbn" type="xsd:string"/>
  </xsd:sequence>
</xsd:complexType>

<xsd:element name="DeleteBookOutput" type="tns:DeleteBookOutputType"/>
<xsd:complexType name="DeleteBookOutputType">
  <xsd:sequence>
    <xsd:element name="deleted" type="xsd:boolean"/>
    <xsd:element name="message" type="xsd:string" minOccurs="0"/>
  </xsd:sequence>
</xsd:complexType>
```

**3. Create `DeleteBook.wsdl`** — copy `GetBook.wsdl`, replace all `GetBook` with `DeleteBook`.
The `<xsd:import>` still points to `book.xsd` (same file, same namespace `bk:`).

**4. Create Java model classes:** `DeleteBookInput.java`, `DeleteBookOutput.java`.

**5. Create SEI:** `DeleteBookPortType.java`.

**6. Create SIB:** `DeleteBookPortTypeImpl.java` — add `@HandlerChain(file="/handler-chain.xml")`.

**7. Publish in `ServerMain.java`:**
```java
DeleteBookPortTypeImpl.publish("http://0.0.0.0:8080/deletebook");
```

**8. Copy WSDL + XSD to `book-client/src/main/resources/`** and add a third wsimport execution in `pom.xml`.

**9. Rebuild:**
```bash
docker-compose up --build
```

---

## Useful Links

- [XSD vs WSDL: What’s the difference?](https://www.tutorialworks.com/xsd-vs-wsdl/)
- [The simple guide to WSDL (with an example)](https://www.tutorialworks.com/wsdl/)
- [XML Schema Tutorial](https://www.w3schools.com/xml/schema_intro.asp)
- [XML WSDL](https://www.w3schools.com/xml/xml_wsdl.asp)
- [XML Web Services](https://www.w3schools.com/xml/xml_services.asp)
