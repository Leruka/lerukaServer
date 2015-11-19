package leruka;

import javax.servlet.http.HttpServletResponse;

/**
 * This class holds the HTTP status codes the should be returned.
 */
public class HttpStatics {

    public static final int HTTP_STATUS_INVALID_PARAMS = 422;
    public static final int HTTP_STATUS_SQL_EXCEPTION = 507;
    public static final int HTTP_STATUS_WRONG_METHOD = 405;
    public static final int HTTP_STATUS_WRONG_CONTENT_TYPE = 415;

    public static void errorRespond(int errorCode, HttpServletResponse response) {

    }

}
