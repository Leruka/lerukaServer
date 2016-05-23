package com.leruka.server.rating;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Rating;
import com.leruka.server.Helper;
import com.leruka.server.HttpStatics;
import com.leruka.server.db.DatabaseConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by leifb on 23.05.16.
 */
public class GetRating extends HttpServlet {
    private static final String EXPECTED_CONTENT = "application/x-protobuf";
    private static final byte[] WRONG_CONTENT_RESPONSE = Rating.ResponseGetRating.newBuilder()
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
        List<Integer> levelIDs;
        try {
            Rating.RequestGetRating requestObject = Rating.RequestGetRating.parseFrom(req.getInputStream());
            levelIDs = requestObject.getLevelIDList();
        }
        // Not a valid protobuf
        catch (IOException ex) {
            Helper.answerError(resp,
                    HttpStatics.HTTP_STATUS_INVALID_PARAMS,
                    buildErrorResponse(ErrorCodes.ErrorCode.REQUEST_CANNOT_PARSE_INPUT).toByteArray());
            return;
        }

        // Get ratings
        List<Rating.LevelRating> ratings = levelIDs.stream().map(this::getRating).collect(Collectors.toList());

        // answer
        resp.getOutputStream().write(
                Rating.ResponseGetRating.newBuilder()
                        .setSuccess(true)
                        .addAllRating(ratings)
                        .build().toByteArray()
        );
        resp.getOutputStream().flush();

    }

    private Rating.LevelRating getRating(int levelID) {
        Rating.LevelRating.Builder builder = Rating.LevelRating.newBuilder().setLevelID(levelID);
        try {
            PreparedStatement st = DatabaseConnection.getCurrentConnection().prepareStatement(
                    "CALL get_average_rating(?)"
            );
            st.setInt(1, levelID);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                return builder.setRating((int) rs.getDouble("avg_rating")).build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return builder.setRating(0).build();
    }

    private static Rating.ResponseGetRating buildErrorResponse(ErrorCodes.ErrorCode... codes) {
        // Create the builder
        Rating.ResponseGetRating.Builder b = Rating.ResponseGetRating.newBuilder()
                .setSuccess(false);

        // Add the error codes
        for (ErrorCodes.ErrorCode c : codes) {
            b.addErrorCode(c);
        }

        // convert to byte array
        return b.build();
    }
}
