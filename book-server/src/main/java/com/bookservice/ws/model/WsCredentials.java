package com.bookservice.generated.getbook;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// =============================================================================
// WsCredentials — the SOAP Header authentication object
// =============================================================================
// Maps to <xsd:element name="WsCredentials"> in common.xsd.
//
// Wire format (inside the SOAP Envelope Header):
//   <soapenv:Header>
//     <cmn:WsCredentials xmlns:cmn="http://bookservice.com/common/v1">
//       <cmn:username>myapp</cmn:username>
//       <cmn:password>secret123</cmn:password>
//       <cmn:systemId>FRONT_OFFICE</cmn:systemId>
//     </cmn:WsCredentials>
//   </soapenv:Header>
// =============================================================================
@XmlRootElement(name = "WsCredentials", namespace = "http://bookservice.com/common/v1")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    namespace = "http://bookservice.com/common/v1",
    propOrder = {"username", "password", "systemId"}
)
public class WsCredentials {

    @XmlElement(name = "username", namespace = "http://bookservice.com/common/v1", required = true)
    private String username;

    @XmlElement(name = "password", namespace = "http://bookservice.com/common/v1", required = true)
    private String password;

    @XmlElement(name = "systemId", namespace = "http://bookservice.com/common/v1", required = true)
    private String systemId;

    public WsCredentials() {
    }

    public WsCredentials(String username, String password, String systemId) {
        this.username = username;
        this.password = password;
        this.systemId = systemId;
    }

    public String getUsername() {
        return this.username;
    }
    public void setUsername(String value) {
        this.username = value;
    }

    public String getPassword() {
        return this.password;
    }
    public void setPassword(String value) {
        this.password = value;
    }

    public String getSystemId() {
        return this.systemId;
    }
    public void setSystemId(String value) {
        this.systemId = value;
    }
}
