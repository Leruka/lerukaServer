package com.leruka.server.user;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.db.DatabaseConnection;
import com.leruka.server.exception.InvalidParameterException;
import com.leruka.server.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.UUID;

import com.leruka.protobuf.User;

/**
 * Created by leif on 05.11.15.
 *
 * This class handles login requests
 */
public class Login extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!request.getContentType().equals("application/x-protobuf")) {
            Helper.answerError(
                    response,
                    HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                    User.ResponseLogin.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
                            .build().toByteArray()
                    );
            return;
        }

        // Get params
        User.RequestLogin requestObject = User.RequestLogin.parseFrom(request.getInputStream());
        String userName = requestObject.getName();
        String userPass = requestObject.getPassword();

        // Validate syntactical
        if (!Validation.isValidUserPw(userName, userPass)) {
            //TODO differentiate between invalid name / invalid pass
            Helper.answerError(
                    response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    User.ResponseLogin.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.USER_NAME_INVALID)
                            .build().toByteArray()
                    );
            return;
        }

        // login
        User.ResponseLogin login = doDefaultLogin(userName, userPass);

        // Respond
        Login.respond(login, response);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    private static UUID login(String userName, String userPass) throws InvalidParameterException, SQLException {

        // Get userID, salt and stored password
        DatabaseUser databaseUser = getDatabaseUser(userName);

        // validate Password
        if (!isPasswordValid(
                userPass,
                databaseUser.getDbPass(),
                databaseUser.getSalt()
        )) {
            throw new InvalidParameterException("The given Password does not match!", ErrorCodes.ErrorCode.LOGIN_PASS_WRONG);
        }

        // create session
        return SessionManager.createSession(databaseUser.getUserID());

    }

    static User.ResponseLogin doDefaultLogin(String userName, String userPass)
            throws IOException {
        //TODO check for specific SQL exceptions and give corresponding error messages
        //TODO Respond with protobuf objects on errors
        // login
        UUID sid;
        try {
            sid = login(userName, userPass);
        } catch (InvalidParameterException e) {
            return createErrorResponse(e.getErrorCode());
        } catch (SQLException e) {
            return createErrorResponse(ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR);
        }

        // return the new session ID
        return User.ResponseLogin.newBuilder()
                .setSuccess(true)
                .setSessionID(sid.toString())
                .build();
    }

    static void respond(User.ResponseLogin login, HttpServletResponse response) {
        if (login.getSuccess()) {
            Helper.answerError(response, 200, login.toByteArray());
        }
        else if (login.getErrorCodeCount() > 0) {
            Helper.answerError(response, HttpStatics.fromInternalErrorCode(login.getErrorCode(0)), login.toByteArray());
        }
        else {
            Helper.answerError(response, HttpStatics.HTTP_STATUS_UNKNOWN_SERVER_ERROR, login.toByteArray());
        }
    }

    static boolean isPasswordValid(String userPass, String dbPass, String salt) throws SQLException {
        // hash the password in the database to ensure, that hashing is always done the same way
        CallableStatement st = DatabaseConnection.getCurrentConnection().prepareCall(
                "{ ? = call hash_pw(?,?) }"
        );
        st.registerOutParameter(1, Types.CHAR);
        st.setString(2, userPass);
        st.setString(3, salt);
        st.execute();

        String doubleHash = st.getString(1);

        Log.inf("Test passwords:" + doubleHash + "," + dbPass);

        return dbPass.equals(doubleHash);
    }

    private static DatabaseUser getDatabaseUser(String userName) throws InvalidParameterException, SQLException {
        DatabaseUser dbUser;
        PreparedStatement st = com.leruka.server.db.DatabaseConnection.getCurrentConnection().prepareStatement(
                "SELECT userID, salt, passwordHash FROM lerukatest.User WHERE name = ?"
        );
        st.setString(1, userName);
        ResultSet rs = st.executeQuery();

        // Get strings from result
        if (rs.next()) {
            dbUser = new DatabaseUser(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3)
            );
        }
        else {
            throw new InvalidParameterException("The given Username could not be found", ErrorCodes.ErrorCode.LOGIN_NAME_UNKNOWN);
        }

        // Check for a second result
        if (rs.next()) {
            Log.wrn("Multiple matches for one username '" + userName  + "'!");
        }

        return dbUser;
    }

    static DatabaseUser getDatabaseUser(int userID) throws InvalidParameterException, SQLException {
        DatabaseUser dbUser;
        PreparedStatement st = com.leruka.server.db.DatabaseConnection.getCurrentConnection().prepareStatement(
                "SELECT salt, passwordHash FROM lerukatest.User WHERE userID = ?"
        );
        st.setInt(1, userID);
        ResultSet rs = st.executeQuery();

        // Get strings from result
        if (rs.next()) {
            dbUser = new DatabaseUser(
                    userID,
                    rs.getString(1),
                    rs.getString(2)
            );
        }
        else {
            throw new InvalidParameterException("The given user id could not be found.", ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR);
        }

        // Check for a second result
        if (rs.next()) {
            Log.wrn("Multiple matches for one user id '" + userID + "'!");
        }

        return dbUser;
    }

    private static User.ResponseLogin createErrorResponse(ErrorCodes.ErrorCode[] codes) {
        return User.ResponseLogin.newBuilder()
                .setSuccess(false)
                .addAllErrorCode(Arrays.asList(codes))
                .build();
    }

    private static User.ResponseLogin createErrorResponse(ErrorCodes.ErrorCode code) {
        return createErrorResponse(new ErrorCodes.ErrorCode[] { code });
    }

}
