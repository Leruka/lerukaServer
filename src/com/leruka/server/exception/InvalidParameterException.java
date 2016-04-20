package com.leruka.server.exception;

import com.leruka.protobuf.ErrorCodes;

/**
 * Created by leif on 05.11.15.
 */
public class InvalidParameterException extends Exception {

    private ErrorCodes.ErrorCode errorCode;

    public InvalidParameterException(String msg, ErrorCodes.ErrorCode errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }

    public ErrorCodes.ErrorCode getErrorCode() {
        return this.errorCode;
    }
}
