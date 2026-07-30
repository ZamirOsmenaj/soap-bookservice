package com.bookservice.springclient.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

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

    public String               getErrorCode()      { return errorCode; }
    public String               getErrorMessage()   { return errorMessage; }
    public XMLGregorianCalendar getErrorTimestamp() { return errorTimestamp; }
    public String               getOperationName()  { return operationName; }
}
