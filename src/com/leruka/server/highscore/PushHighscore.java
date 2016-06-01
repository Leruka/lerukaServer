package com.leruka.server.highscore;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.Log;
import com.leruka.server.db.DatabaseConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by leifb on 01.06.16.
 */
public class PushHighscore extends GenericHighscore {


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Check content Type
        if (!Helper.checkContentType(req.getContentType(), EXPECTED_CONTENT, WRONG_CONTENT_RESPONSE, resp)) {
            return;
        }

        // Parse proto
        Highscore.RequestPushScore requestObject;
        try {
            requestObject = Highscore.RequestPushScore.parseFrom(req.getInputStream());
        }
        // Not a valid protobuf
        catch (IOException e) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    ErrorResponse.build(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT).toByteArray());
            return;
        }

        // Get user id
        int userID = getUserID(requestObject.getSessionID(), resp);
        if (userID < 0) return;


        try {
            this.pushScore(userID, requestObject.getScore());
        } catch (SQLException e) {
            Log.wrn(e.getMessage());
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    ErrorResponse.build(ErrorCodes.ErrorCode.UNKNOWN).toByteArray());
            return;
        }

        sendScoreData(new ArrayList<>(), resp);
    }

    private void pushScore(int userID, long score) throws SQLException {

        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call push_score(?, ?) }");
        proc.setInt(1, userID);
        proc.setInt(2, (int) score);
        proc.execute();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }
}
