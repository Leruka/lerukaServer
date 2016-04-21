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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!request.getContentType().equals("application/x-protobuf")) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_WRONG_CONTENT_TYPE,
                    Highscore.ResponseScores.newBuilder()
                        .setSuccess(false)
                        .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
                            .build().toByteArray());
            return;
        }

        // Get user id
        Highscore.RequestPrivateScore requestObject = Highscore.RequestPrivateScore.parseFrom(request.getInputStream());
        int userID;
        try {
            userID = SessionManager.getUserID(requestObject.getSessionID());
        }
        catch (IllegalArgumentException e) {
            // Illegal session id
            //TODO add error code REQUEST_SESSION_ID_INVALID
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    Highscore.ResponseScores.newBuilder()
                    .setSuccess(false)
                    .addErrorCode(ErrorCodes.ErrorCode.UNKNOWN)
                        .build().toByteArray());
            return;
        }

        // If the session is expired
        //TODO improve
        if (userID < 0) {
            //TODO add error code REQUEST_SESSION_EXPIRED
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    Highscore.ResponseScores.newBuilder()
                            .setSuccess(false)
                            .addErrorCode(ErrorCodes.ErrorCode.UNKNOWN)
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
