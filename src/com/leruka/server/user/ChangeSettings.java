package com.leruka.server.user;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;
import com.leruka.protobuf.User;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.db.DatabaseConnection;
import com.leruka.server.highscore.ErrorResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Created by leifb on 17.05.16.
 */
public class ChangeSettings extends HttpServlet {

    private static final String EXPECTED_CONTENT = "application/x-protobuf";
    private static final byte[] WRONG_CONTENT_RESPONSE = User.ResponseChangeSettings.newBuilder()
            .setSuccess(false)
            .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
            .build().toByteArray();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!Helper.checkContentType(request.getContentType(), EXPECTED_CONTENT, WRONG_CONTENT_RESPONSE, response)) {
            return;
        }
        // Get user id
        int userID;
        User.RequestChangeSettings requestObject;
        try {
            requestObject = User.RequestChangeSettings.parseFrom(request.getInputStream());
            userID = SessionManager.getUserID(requestObject.getSessionID());
        }
        // Not a valid protobuf
        catch (IOException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT).toByteArray());
            return;
        }
        // Illegal session id
        catch (IllegalArgumentException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_SESSION_ID_INVALID).toByteArray());
            return;
        }
        // Session is expired
        if (userID < 0) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_SESSION_EXPIRED).toByteArray());
            return;
        }

        String newName = requestObject.getNewName();
        String newPass = requestObject.getNewPassword();

        if (newName != null) {
            try {
                changeName(userID, newName);
            }
            // Unknown SQL exception
            catch (SQLException e) {
                e.printStackTrace();
                Helper.answerError(response,
                        HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                        buildErrorResponse(ErrorCodes.ErrorCode.UNKNOWN).toByteArray());
                return;
            }
        }

        if (newPass != null) {
            try {
                changePass(userID, newPass);
            }
            // Unknown SQL exception
            catch (SQLException e) {
                e.printStackTrace();
                Helper.answerError(response,
                        HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                        buildErrorResponse(ErrorCodes.ErrorCode.UNKNOWN).toByteArray());
                return;
            }
        }

        response.getOutputStream().write(
                User.ResponseChangeSettings.newBuilder().setSuccess(true).build().toByteArray()
        );
        response.getOutputStream().flush();

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    private void changeName(int userID, String newName) throws SQLException {
        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call change_username(?, ?) }");
        proc.setInt(1, userID);
        proc.setString(2, newName);
        proc.execute();
    }

    private void changePass(int userID, String newPass) throws SQLException {
        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call change_password(?, ?) }");
        proc.setInt(1, userID);
        proc.setString(2, newPass);
        proc.execute();
    }

    private static User.ResponseChangeSettings buildErrorResponse(ErrorCodes.ErrorCode... codes) {
        // Create the builder
        User.ResponseChangeSettings.Builder b = User.ResponseChangeSettings.newBuilder()
                .setSuccess(false);

        // Add the error codes
        for (ErrorCodes.ErrorCode c : codes) {
            b.addErrorCode(c);
        }

        // convert to byte array
        return b.build();
    }
}
