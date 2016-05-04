package com.leruka.server.highscore;

import com.leruka.protobuf.Highscore;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by leifb on 04.05.16.
 */
public abstract class GenericHighscore extends HttpServlet {

    void sendScoreData(List<Highscore.Score> scores, HttpServletResponse response) throws IOException {
        // Create the response
        Highscore.ResponseScores responseObject = Highscore.ResponseScores.newBuilder()
                .addAllScores(scores).build();

        // send response
        response.getOutputStream().write(responseObject.toByteArray());
        response.getOutputStream().flush();
    }

}
