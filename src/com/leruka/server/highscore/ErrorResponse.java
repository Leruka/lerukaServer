package com.leruka.server.highscore;

import com.leruka.protobuf.ErrorCodes;
import com.leruka.protobuf.Highscore;

/**
 * Created by leifb on 04.05.16.
 */
public class ErrorResponse {

    static Highscore.ResponseScores build(ErrorCodes.ErrorCode... codes) {
        // Create the builder
        Highscore.ResponseScores.Builder b = Highscore.ResponseScores.newBuilder()
                .setSuccess(false);

        // Add the error codes
        for (ErrorCodes.ErrorCode c : codes) {
            b.addErrorCode(c);
        }

        // convert to byte array
        return b.build();
    }
}
