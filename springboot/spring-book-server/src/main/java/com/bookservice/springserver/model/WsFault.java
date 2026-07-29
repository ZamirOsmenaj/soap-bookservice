package com.bookservice.springserver.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

// Maps to <xsd:element name="WsFault"> in common.xsd
@XmlRootElement(name = "WsFault", namespace = "http://bookservice.com/common/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name      = "",
    namespace = "http://bookservice.com/common/v1",
    propOrder = { "errorCode", "errorMessage", "errorTimestamp", "operationName" }
)
public class WsFault {

    @XmlElement(name = "errorCode",      namespace = "http://bookservice.com/common/v1", required = true)
    private String errorCode;

    @XmlElement(name = "errorMessage",   namespace = "http://bookservice.com/common/v1", required = true)
    private String errorMessage;

    @XmlElement(name = "errorTimestamp", namespace = "http://bookservice.com/common/v1", required = true)
    @XmlSchemaType(name = "dateTime")
    private XMLGregorianCalendar errorTimestamp;

    @XmlElement(name = "operationName",  namespace = "http://bookservice.com/common/v1", required = false)
    private String operationName;

    public WsFault() {}

    public WsFault(String errorCode, String errorMessage,
                   XMLGregorianCalendar errorTimestamp, String operationName) {
        this.errorCode      = errorCode;
        this.errorMessage   = errorMessage;
        this.errorTimestamp = errorTimestamp;
        this.operationName  = operationName;
    }

    public String               getErrorCode()                                  { return errorCode; }
    public void                 setErrorCode(String v)                          { this.errorCode = v; }

    public String               getErrorMessage()                               { return errorMessage; }
    public void                 setErrorMessage(String v)                       { this.errorMessage = v; }

    public XMLGregorianCalendar getErrorTimestamp()                             { return errorTimestamp; }
    public void                 setErrorTimestamp(XMLGregorianCalendar v)       { this.errorTimestamp = v; }

    public String               getOperationName()                              { return operationName; }
    public void                 setOperationName(String v)                      { this.operationName = v; }
}
