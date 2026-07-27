package com.bookservice.ws.porttype;

import com.bookservice.ws.fault.WsException;
import com.bookservice.ws.model.CreateBookInput;
import com.bookservice.ws.model.CreateBookOutput;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

// =============================================================================
// CreateBookPortType — SEI for the CreateBook operation
// =============================================================================
// Mirrors <wsdl:portType name="CreateBookPortType"> in CreateBook.wsdl.
// Pattern is identical to GetBookPortType — the naming and structure is consistent.
//
// For the full educational breakdown of SEI concepts, @WebService, @SOAPBinding,
// @WebMethod, @WebResult, @WebParam, and @XmlSeeAlso — see GetBookPortType.java.
// This file intentionally stays lean to avoid repetition.
// =============================================================================
@WebService(
    name            = "CreateBookPortType",
    targetNamespace = "http://bookservice.com/createbook/wsdl/v1"
)
@SOAPBinding(
    style          = SOAPBinding.Style.DOCUMENT,
    use            = SOAPBinding.Use.LITERAL,
    parameterStyle = SOAPBinding.ParameterStyle.BARE
)
@XmlSeeAlso({ CreateBookInput.class, CreateBookOutput.class })
public interface CreateBookPortType {

    @WebMethod(
        operationName = "CreateBook",
        action = "CreateBook"
    )

    @WebResult(
        name            = "CreateBookOutput",
        targetNamespace = "http://bookservice.com/book/v1",
        partName        = "CreateBookOutput"
    )
    CreateBookOutput createBook(

        @WebParam(
            name            = "CreateBookInput",
            targetNamespace = "http://bookservice.com/book/v1",
            partName        = "CreateBookInput",
            mode            = WebParam.Mode.IN
        )
        CreateBookInput createBookInput

    ) throws WsException;
}

