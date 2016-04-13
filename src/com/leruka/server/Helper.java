package com.leruka.server;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by leif on 18.11.15.
 */
public class Helper {

    public static void answerError(
            HttpServletResponse response,
            int statusCode,
            int errorCode,
            String message) {

        //TODO this needs a rework!

        // Set the response Code
        response.setStatus(statusCode);
        // Create tht response Json

        StringBuilder sb = new StringBuilder();
        sb.append("success=false;errorCode=").append(errorCode).append("errorMessage=").append(message);

        // Write the response
        try {
            response.getWriter().write(sb.toString());
            response.flushBuffer();
        } catch (IOException e) {
            // nothing to do than logging the error
            e.printStackTrace();
        }
    }
}
