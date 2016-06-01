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
public class PrivateHighscore extends GenericHighscore {




    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check content Type
        if (!Helper.checkContentType(request.getContentType(), EXPECTED_CONTENT, WRONG_CONTENT_RESPONSE, response)) {
            return;
        }
        // Parse proto
        Highscore.RequestPrivateScore requestObject;
        try {
            requestObject = Highscore.RequestPrivateScore.parseFrom(request.getInputStream());
        }
        // Not a valid protobuf
        catch (IOException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    ErrorResponse.build(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT).toByteArray());
            return;
        }

        // Get user id
        int userID = getUserID(requestObject.getSessionID(), response);
        if (userID < 0) return;

        // Get recent data
        List<Highscore.Score> data;
        try {
            data = this.getScoreData(userID);
        }
        // Cannot fetch from DB
        catch (SQLException e) {
            //TODO Check for the DB Error (maybe give more information on what went wrong)
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    ErrorResponse.build(ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR).toByteArray());
            return;
        }

        this.sendScoreData(data, response);
    }

    private List<Highscore.Score> getScoreData(int userID) throws SQLException {
        List<Highscore.Score> data = new ArrayList<>();

        // Create the SQL statement
        CallableStatement proc = DatabaseConnection.getCurrentConnection()
                .prepareCall("{ call get_private_score(?) }");
        proc.setInt(1, userID);

        // Gather the data
        ResultSet rs = proc.executeQuery();
        int rankCount = 0;
        while(rs.next()) {
            data.add(Highscore.Score.newBuilder()
                    .setScore(rs.getInt("score"))
                    .setRank(++rankCount)
                    .setTimestamp(rs.getDate("date").getTime()).build()
            );
        }

        return data;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

}
