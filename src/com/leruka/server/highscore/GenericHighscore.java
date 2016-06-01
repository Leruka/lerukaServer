package com.leruka.server.highscore;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.user.SessionManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by leifb on 04.05.16.
 */
public abstract class GenericHighscore extends HttpServlet {

    static String EXPECTED_CONTENT = "application/x-protobuf";

    static byte[] WRONG_CONTENT_RESPONSE = Highscore.ResponseScores.newBuilder()
            .setSuccess(false)
            .addErrorCode(ErrorCodes.ErrorCode.REQUEST_WRONG_CONTENT_TYPE)
            .build().toByteArray();

    void sendScoreData(List<Highscore.Score> scores, HttpServletResponse response) throws IOException {
        // Create the response
        Highscore.ResponseScores responseObject = Highscore.ResponseScores.newBuilder()
                .addAllScores(scores).setSuccess(true).build();

        // send response
        response.getOutputStream().write(responseObject.toByteArray());
        response.getOutputStream().flush();
    }

    int getUserID(String sessioID, HttpServletResponse response) {
        int userID;
        try {
            userID = SessionManager.getUserID(sessioID);
        }
        // Illegal session id
        catch (IllegalArgumentException e) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    ErrorResponse.build(ErrorCodes.ErrorCode.REQUEST_SESSION_ID_INVALID).toByteArray());
            return -1;
        }
        // Session is expired
        if (userID < 0) {
            Helper.answerError(response,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    ErrorResponse.build(ErrorCodes.ErrorCode.REQUEST_SESSION_EXPIRED).toByteArray());
            return -1;
        }
        return userID;
    }

}
