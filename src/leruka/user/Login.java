package leruka.user;

import de.leifb.objectJson.Json;
import leruka.ErrorCodes;
import leruka.Helper;
import leruka.HttpStatics;
import leruka.exception.InvalidParameterException;
import leruka.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

/**
 * Created by leif on 05.11.15.
 */
public class Login extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!request.getContentType().equals("application/json")) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                    ErrorCodes.REQUEST_CONTENT_TYPE_NOT_JSON,
                    "To log in a json with the attributes user and pass is needed.");
            return;
        }


        // Get params
        Json input = Helper.getRequestJson(request);
        String userName = input.getString("userName");
        String userPass = input.getString("passwordHash");

        Log.inf("Login Request with pw: " + userPass);

        // Validate syntactical
        if (!User.isValidUserPw(userName, userPass)) {
            Helper.answerError(response, HttpStatics.HTTP_STATUS_INVALID_PARAMS, ErrorCodes.USER_PASS_INVALID, "NEIN");
            return;
        }

        // login
        doDefaultLogin(userName, userPass, response);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    static UUID login(String userName, String userPass) throws InvalidParameterException, SQLException {

        // Get userID, salt and stored password
        DatabaseUser databaseUser = getDatabaseUser(userName);

        // validate Password
        if (!isPasswordValid(
                userPass,
                databaseUser.getDbPass(),
                databaseUser.getSalt()
        )) {
            throw new InvalidParameterException("The given Password does not match!", ErrorCodes.USER_PASS_INVALID);
        }

        // create session
        return SessionManager.createSession(databaseUser.getUserID());

    }

    static void doDefaultLogin(String userName, String userPass, HttpServletResponse response) throws IOException {
        //TODO check for specific SQL exceptions and give corresponding error messages
        // login
        UUID sid;
        try {
            sid = login(userName, userPass);
        } catch (InvalidParameterException e) {
            Helper.answerError(response, HttpStatics.HTTP_STATUS_INVALID_PARAMS, e.getErrorCode(), e.getMessage());
            e.printStackTrace();
            return;
        } catch (SQLException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    ErrorCodes.DB_UNKNOWN_ERROR,
                    "There was an error in the database while trying to log in.");
            e.printStackTrace();
            return;
        }

        // return the new session ID
        Json responseJson = new Json();
        responseJson.addAttribute("success", true);
        responseJson.addAttribute("sessionID", sid.toString());
        response.getWriter().write(responseJson.toString());
        response.flushBuffer();
    }

    private static boolean isPasswordValid(String userPass, String dbPass, String salt) throws SQLException {
        // hash the password in the database to ensure, that hashing is always done the same way
        CallableStatement st = leruka.db.DatabaseConnection.getCurrentConnection().prepareCall(
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
        PreparedStatement st = leruka.db.DatabaseConnection.getCurrentConnection().prepareStatement(
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
            throw new InvalidParameterException("The given Username could not be found", ErrorCodes.USER_NAME_INVALID);
        }

        // Check for a second result
        if (rs.next()) {
            Log.wrn("Multiple matches for one username!");
        }
        return dbUser;
    }

    private static class DatabaseUser {
        private int userID;
        private String salt, dbPass;

        DatabaseUser(int userID, String salt, String dbPass) {
            this.userID = userID;
            this.salt = salt;
            this.dbPass = dbPass;
        }

        public int getUserID() {
            return userID;
        }

        public String getSalt() {
            return salt;
        }

        public String getDbPass() {
            return dbPass;
        }
    }
}
