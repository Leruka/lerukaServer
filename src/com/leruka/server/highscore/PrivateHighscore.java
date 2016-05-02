package com.leruka.server.highscore;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leif on 15.04.16.
 */
public class PrivateHighscore extends HttpServlet {

    private static byte[] WRONG_CONTENT_RESPONSE = Highscore.ResponseScores.newBuilder()
            .setSuccess(false)
            .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
            .build().toByteArray();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!Helper.checkContentType(request.getContentType(), "application/x-protobuf", WRONG_CONTENT_RESPONSE, response)) {
            return;
        }

        // Get user id
        int userID;
        try {
            Highscore.RequestPrivateScore requestObject = Highscore.RequestPrivateScore.parseFrom(request.getInputStream());
            userID = SessionManager.getUserID(requestObject.getSessionID());
        }
        catch (IOException e) {
            // Not a valid protobuf
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    Highscore.ResponseScores.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT));
            return;
        }
        catch (IllegalArgumentException e) {
            // Illegal session id
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    Highscore.ResponseScores.newBuilder()
                    .setSuccess(false)
                    .addErrorCode(ErrorCodes.ErrorCode.REQUEST_SESSION_ID_INVALID)
                        .build().toByteArray());
            return;
        }

        // If the session is expired
        if (userID < 0) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    Highscore.ResponseScores.newBuilder()
                            .setSuccess(false)
                            .addErrorCode(ErrorCodes.ErrorCode.REQUEST_SESSION_EXPIRED)
                            .build().toByteArray());
            return;
        }

        // Get recent data
        List<Highscore.Score> data = new ArrayList<>();
        try {
            CallableStatement proc = DatabaseConnection.getCurrentConnection()
                    .prepareCall("{ call get_private_score(?) }");
            proc.setInt(1, userID);
            ResultSet rs = proc.executeQuery();

            int rankCount = 0;
            while(rs.next()) {
                data.add(Highscore.Score.newBuilder()
                        .setScore(rs.getInt("score"))
                        .setRank(++rankCount)
                        .setTimestamp(rs.getDate("date").getTime()).build()
                );
            }
        } catch (SQLException e) {
            //TODO If fetching does not work, respond with an error
            Helper.answerError(
                    response,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    Highscore.ResponseScores.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR)
                            .build().toByteArray()
            );
            return;
        }

        // Create the response
        Highscore.ResponseScores responseObject = Highscore.ResponseScores.newBuilder()
                .addAllScores(data).build();

        // send response
        response.getOutputStream().write(responseObject.toByteArray());
        response.getOutputStream().flush();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }
}
