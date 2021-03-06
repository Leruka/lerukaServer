package com.leruka.server;

import com.leruka.protobuf.*;
import com.leruka.protobuf.ErrorCodes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by leif on 18.11.15.
 */
public class Helper {

    public static void answerError(
            HttpServletResponse response,
            int statusCode,
            byte[] message
    ) {

        // Set the response Code
        response.setStatus(statusCode);

        // Write the response
        try {
            response.getOutputStream().write(message);
            response.getOutputStream().flush();
        } catch (IOException e) {
            // nothing to do than logging the error
            e.printStackTrace();
        }

    }

    public static boolean checkContentType(
            String sendContent,
            String expectedContent,
            byte[] failureAnswer,
            HttpServletResponse response
    ) {
        if (sendContent.equals(expectedContent)) {
            return true;
        }
        Helper.answerError(
                response,
                HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                failureAnswer
        );
        return false;
    }


}
