package com.leruka.server.highscore;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.Log;
import com.leruka.server.db.DatabaseConnection;
import com.sun.org.apache.xpath.internal.SourceTree;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

/**
 * Created by leif on 09.11.15.
 */
public class PublicHighscore extends GenericHighscore {

    private List<Highscore.Score> cachedScores;
    private long lastCacheTime;


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get recent data
        List<Highscore.Score> data;
        try {
            data = this.getScoreData();
        }
        // Cannot fetch
        catch (SQLException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_SQL_EXCEPTION,
                    ErrorResponse.build(ErrorCodes.ErrorCode.DB_UNKNOWN_ERROR).toByteArray());
            return;
        }

        // Send the data
        sendScoreData(data, response);
    }

    private List<Highscore.Score> getScoreData() throws SQLException {

        // Check, if cache can be used
        if (Instant.now().getEpochSecond() - lastCacheTime < 60 && this.cachedScores != null) {
            return this.cachedScores;
        }

        // Else get new scores
        List<Highscore.Score> data = new ArrayList<>();

        // Create the SQL statement
        Statement st = DatabaseConnection.getCurrentConnection().createStatement();
        String sql = ("CALL get_public_score()");
        ResultSet rs = st.executeQuery(sql);

        // Gather the data
        int rankCount = 0;
        while(rs.next()) {
            data.add(Highscore.Score.newBuilder()
                    .setUserName(rs.getString("name"))
                    .setScore(rs.getInt("score"))
                    .setRank(++rankCount).build()
            );
        }

        // If fetching worked, update cache
        this.updateCache(data);

        return data;
    }

    private void updateCache(List<Highscore.Score> scores) {
        this.cachedScores = scores;
        this.lastCacheTime = Instant.now().getEpochSecond();
    }

}

