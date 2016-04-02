package com.leruka.server.exception;

/**
 * Created by leif on 05.11.15.
 */
public class InvalidParameterException extends Exception {

    private int errorCode;

    public InvalidParameterException(String msg, int errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}
