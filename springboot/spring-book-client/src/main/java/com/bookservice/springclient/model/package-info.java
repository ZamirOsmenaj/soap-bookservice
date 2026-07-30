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
package com.bookservice.springclient.model;
