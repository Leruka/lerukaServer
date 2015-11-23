package leruka;

import de.leifb.objectJson.Json;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by leif on 18.11.15.
 */
public class Helper {

    public static Json getRequestJson(HttpServletRequest request) {
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();

            String line;
            while((line = reader.readLine()) != null){
                buffer.append(line);
            }

            return new Json(buffer.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Json();
    }

    public static void answerError(
            HttpServletResponse response,
            int statusCode,
            int errorCode,
            String message) {

        // Set the response Code
        response.setStatus(statusCode);
        // Create tht response Json
        Json responseJson = new Json();
        responseJson.addAttribute("success", false);
        responseJson.addAttribute("errorCode", errorCode);
        responseJson.addAttribute("errorMessage", message);

        // Write the response
        try {
            response.getWriter().write(responseJson.toString());
            response.flushBuffer();
        } catch (IOException e) {
            // nothing to do than logging the error
            e.printStackTrace();
        }
    }
}
