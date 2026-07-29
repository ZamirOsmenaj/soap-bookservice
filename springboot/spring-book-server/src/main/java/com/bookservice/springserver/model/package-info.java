// JAXB package-level namespace binding — tells JAXB what default namespace
// to use for classes in this package and how to prefix them in serialised XML.
// Equivalent to the same file in the plain-Java version, just using
// jakarta.xml.bind.* annotations instead of javax.xml.bind.*.
@jakarta.xml.bind.annotation.XmlSchema(
    namespace = "http://bookservice.com/common/v1",
    elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED,
    xmlns = {
        @jakarta.xml.bind.annotation.XmlNs(
            prefix       = "cmn",
            namespaceURI = "http://bookservice.com/common/v1"
        ),
        @jakarta.xml.bind.annotation.XmlNs(
            prefix       = "bk",
            namespaceURI = "http://bookservice.com/book/v1"
        )
    }
)
package com.bookservice.springserver.model;
