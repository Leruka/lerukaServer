package com.leruka.server.rating;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Rating;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.db.DatabaseConnection;
import com.leruka.server.user.SessionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Created by leifb on 18.05.16.
 */
public class RateLevel extends HttpServlet {

    private static final String EXPECTED_CONTENT = "application/x-protobuf";
    private static final byte[] WRONG_CONTENT_RESPONSE = Rating.ResponseRateLevel.newBuilder()
            .setSuccess(false)
            .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
            .build().toByteArray();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Check content Type
        if (!Helper.checkContentType(req.getContentType(), EXPECTED_CONTENT, WRONG_CONTENT_RESPONSE, resp)) {
            return;
        }

        // Parse input
        Rating.LevelRating rating;
        int userID;
        try {
            Rating.RequestRateLevel rateLevel = Rating.RequestRateLevel.parseFrom(req.getInputStream());
            userID = SessionManager.getUserID(rateLevel.getSessionID());
            rating = rateLevel.getRating();
        }
        // Not a valid protobuf
        catch (IOException ex) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT).toByteArray());
            return;
        }
        // Illegal session id
        catch (IllegalArgumentException e) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_SESSION_ID_INVALID).toByteArray());
            return;
        }
        // Session is expired
        if (userID < 0) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_SESSION_EXPIRED).toByteArray());
            return;
        }

        // rate
        try {
            this.rateLevel(userID, rating);
        } catch (SQLException e) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    buildErrorResponse(ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR).toByteArray());
            return;
        }

        // Answer success
        resp.getOutputStream().write(
                Rating.ResponseRateLevel.newBuilder().setSuccess(true).build().toByteArray()
        );
        resp.getOutputStream().flush();
    }

    private void rateLevel(int userID, Rating.LevelRating rating) throws SQLException {
        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call rate_level(?, ?, ?) }");
        proc.setInt(1, userID);
        proc.setInt(2, rating.getLevelID());
        proc.setInt(3, rating.getRating());
        proc.execute();
    }

    private static Rating.ResponseRateLevel buildErrorResponse(ErrorCodes.ErrorCode... codes) {
        // Create the builder
        Rating.ResponseRateLevel.Builder b = Rating.ResponseRateLevel.newBuilder()
                .setSuccess(false);

        // Add the error codes
        for (ErrorCodes.ErrorCode c : codes) {
            b.addErrorCode(c);
        }

        // convert to byte array
        return b.build();
    }
}
