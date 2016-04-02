package com.leruka.server.user;

import de.leifb.objectJson.Json;
import com.leruka.server.ErrorCodes;
import com.leruka.server.HttpStatics;
import com.leruka.server.Helper;
import com.leruka.server.Log;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;


public class Register extends javax.servlet.http.HttpServlet {

    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {

        // Check content Type
        if (!request.getContentType().equals("application/json")) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                    ErrorCodes.REQUEST_CONTENT_TYPE_NOT_JSON,
                    "To create a user a json with the attributes user and pass is needed.");
            Log.inf("Register request with wrong content type. canceling.");
            return;
        }

        // Get params
        Json input = Helper.getRequestJson(request);
        String userName = input.getString("userName");
        String userPass = input.getString("passwordHash");

        Log.inf("Register Request with pw: " + userPass);

        // Validate
        if (!User.isValidUserPw(userName, userPass)) {
            Log.inf("Register request with invalid parameters received.");
            response.setStatus(HttpStatics.HTTP_STATUS_INVALID_PARAMS);
            return;
        }

        // create user in db
        try {
            createNewUser(userName, userPass);
        } catch (SQLException e) {
            response.setStatus(HttpStatics.HTTP_STATUS_SQL_EXCEPTION);
            Log.wrn("Could create a new user, due to a SQL exception!");
            Log.inf("Error Code: " + e.getErrorCode());
            Log.inf("Error Message: " + e.getMessage());
            // Answer with message: user used
            Helper.answerError(response, HttpStatics.HTTP_STATUS_SQL_EXCEPTION, ErrorCodes.USER_NAME_USED,
                    "The username is already used");
            return;
        }

        // Log the successful result
        Log.inf("successfully created user " + userName);

        // login. This will also response
        Login.doDefaultLogin(userName, userPass, response);
    }

    /**
     *
     * @param request
     * @param response
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    /**
     * Creates a new user in the Database.
     * @param userName The name of the new user
     * @param userPass The (already once hashed!) password of the new user
     * @throws SQLException
     */
    void createNewUser(String userName, String userPass) throws SQLException {
        CallableStatement proc = leruka.db.DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call create_user(?, ?) }");
        proc.setString(1, userName);
        proc.setString(2, userPass);
        proc.execute();
    }

}
