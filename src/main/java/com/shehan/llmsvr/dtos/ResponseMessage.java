package com.shehan.llmsvr.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {
    protected String code;
    protected String message;
    protected Object data;
    protected Object error;

    public static ResponseMessage getInstance(ResponseCode responseCode, Object data, Object error) {
        ResponseMessage responseMessage = new ResponseMessage(responseCode.getCode(), responseCode.getMessage(), data, error);
        return responseMessage;
    }
}
