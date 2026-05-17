// =============================================================================
// package-info.java — JAXB Package-Level Namespace Binding
// =============================================================================
// This file serves one critical purpose: it tells JAXB what XML namespace
// to use by DEFAULT for all classes in the com.bookservice.ws.model package.
//
// WITHOUT this file:
//   JAXB would serialize elements into the default (empty) namespace.
//   The SOAP body would look like:
//     <GetBookInput>           ← no namespace prefix — WRONG
//       <isbn>978-x</isbn>
//     </GetBookInput>
//
// WITH this file:
//   JAXB uses the correct namespaces as declared by @XmlElement annotations
//   and the schema declarations, producing properly namespaced XML:
//     <bk:GetBookInput xmlns:bk="http://bookservice.com/book/v1">
//       <bk:isbn>978-x</bk:isbn>
//     </bk:GetBookInput>
//
// STRUCTURE of the annotation:
//   @XmlSchema(
//     namespace = "..."
//       → The DEFAULT namespace for classes in this package.
//         Used when a class or field doesn't specify its own namespace.
//
//     elementFormDefault = XmlNsForm.QUALIFIED
//       → All elements must be namespace-qualified.
//         Matches elementFormDefault="qualified" in our XSD files.
//         This is REQUIRED for correct SOAP document/literal serialisation.
//
//     xmlns = { @XmlNs(...) }
//       → Preferred namespace prefix declarations.
//         These control what prefix JAXB PREFERS to use when serialising.
//         Example: @XmlNs(prefix="cmn", namespaceURI="http://bookservice.com/common/v1")
//         means JAXB will write cmn:BookType instead of ns2:BookType.
//         This only affects readability — both are semantically equivalent XML.
//   )
//
// WHY does this file exist separately from the classes?
//   JAXB applies @XmlSchema at the PACKAGE level — it affects all classes
//   in the package. Java requires package-level annotations to be in a special
//   file named exactly "package-info.java" in the package directory.
//   You cannot put package-level annotations anywhere else.
// =============================================================================
@javax.xml.bind.annotation.XmlSchema(
    namespace = "http://bookservice.com/common/v1",
    elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
    xmlns = {
        @javax.xml.bind.annotation.XmlNs(
            prefix       = "cmn",
            namespaceURI = "http://bookservice.com/common/v1"
        ),
        @javax.xml.bind.annotation.XmlNs(
            prefix       = "bk",
            namespaceURI = "http://bookservice.com/book/v1"
        ),
        @javax.xml.bind.annotation.XmlNs(
            prefix       = "bk",
            namespaceURI = "http://bookservice.com/book/v1"
        )
    }
)
package com.bookservice.ws.model;
