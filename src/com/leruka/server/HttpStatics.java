package com.leruka.server;

import com.leruka.protobuf.ErrorCodes;

import javax.servlet.http.HttpServletResponse;

/**
 * This class holds the HTTP status codes the should be returned.
 */
public class HttpStatics {

    public static final int HTTP_STATUS_INVALID_PARAMS = 422;
    public static final int HTTP_STATUS_SQL_EXCEPTION = 507;
    public static final int HTTP_STATUS_WRONG_METHOD = 405;
    public static final int HTTP_STATUS_WRONG_CONTENT_TYPE = 415;
    public static final int HTTP_STATUS_UNKNOWN_SERVER_ERROR = 500;

    public static int fromInternalErrorCode(ErrorCodes.ErrorCode code) {
        switch (code) {
            case LOGIN_NAME_UNKNOWN:
            case LOGIN_PASS_WRONG:
            case USER_NAME_INVALID:
            case USER_PASS_INVALID:
            case REGISTER_NAME_USED:
                return HTTP_STATUS_INVALID_PARAMS;
            case DB_UNKNOWN_ERROR:
                return HTTP_STATUS_SQL_EXCEPTION;
            case REQUEST_WRONG_CONTENT_TYPE:
                return HTTP_STATUS_WRONG_CONTENT_TYPE;
            case UNKNOWN:
            default:
                return HTTP_STATUS_UNKNOWN_SERVER_ERROR;
        }
    }

}
