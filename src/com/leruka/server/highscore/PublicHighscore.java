package com.leruka.server.highscore;

import com.leruka.protobuf.Highscore;
import com.leruka.server.HttpStatics;
import com.leruka.server.Log;
import com.leruka.server.db.DatabaseConnection;
import com.sun.org.apache.xpath.internal.SourceTree;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leif on 09.11.15.
 */
public class PublicHighscore extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpStatics.HTTP_STATUS_WRONG_METHOD);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get recent data
        List<Highscore.Score> data = new ArrayList<>();
        try {
            Statement st = DatabaseConnection.getCurrentConnection().createStatement();
            String sql = ("CALL get_public_score()");
            ResultSet rs = st.executeQuery(sql);
            int rankCount = 0;
            while(rs.next()) {
                data.add(Highscore.Score.newBuilder()
                        .setUserName(rs.getString("name"))
                        .setScore(rs.getInt("score"))
                        .setRank(++rankCount).build()
                );
            }
        } catch (SQLException e) {
            //TODO If fetching does not work, respond with an error
            e.printStackTrace();
            response.getWriter().write(e.getMessage());
        }

        // Create the response
        Highscore.ResponseScores responseObject = Highscore.ResponseScores.newBuilder()
                .addAllScores(data).build();

        // send response
        response.getOutputStream().write(responseObject.toByteArray());
        response.getOutputStream().flush();
    }
}

