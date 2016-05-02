package com.leruka.server.user;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.server.HttpStatics;
import com.leruka.server.Helper;
import com.leruka.server.db.DatabaseConnection;

import com.leruka.protobuf.User;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;


public class Register extends javax.servlet.http.HttpServlet {

    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {

        // Check content Type
        if (!request.getContentType().equals("application/x-protobuf")) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                    User.ResponseRegister.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
                            .build().toByteArray()
                    );
            return;
        }

        // Get params
        User.RequestRegister requestObject = User.RequestRegister.parseFrom(request.getInputStream());
        String userName = requestObject.getName();
        String userPass = requestObject.getPassword();

        // Validate
        if (!Validation.isValidUserPw(userName, userPass)) {
            //TODO differentiate between invalid name / invalid pass
            Helper.answerError(
                    response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    User.ResponseRegister.newBuilder()
                            .setSuccess(false)
                            .addErrorCode(ErrorCodes.ErrorCode.USER_NAME_INVALID)
                            .build().toByteArray()
            );
            return;
        }

        // create user in db
        try {
            createNewUser(userName, userPass);
        } catch (SQLException e) {
            // Answer with message: user used
            //TODO check for sql error code
            Helper.answerError(
                    response,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    User.ResponseRegister.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.REGISTER_NAME_USED)
                            .build().toByteArray());
            return;
        }

        // login
        User.ResponseLogin login = Login.doDefaultLogin(userName, userPass);

        // Answer
        Helper.answerError(response, 200,
                User.ResponseRegister.newBuilder()
                .setSuccess(true)
                .setLogin(login)
                    .build().toByteArray());
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
    private void createNewUser(String userName, String userPass) throws SQLException {
        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call create_user(?, ?) }");
        proc.setString(1, userName);
        proc.setString(2, userPass);
        proc.execute();
    }

}
