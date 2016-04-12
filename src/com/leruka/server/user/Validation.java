package com.leruka.server.user;

/**
 * Created by leif on 05.11.15.
 */
public class Validation {


    private static final int HASH_LENGTH = 64;

    /**
     * Tests if the user / password combination is syntactic correct.
     * @param userName The user name to check
     * @param pwh The hashed password to check
     * @return true if it is valid, else false
     */
    static boolean isValidUserPw(String userName, String pwh) {
        // Check for null
        if (userName == null || pwh == null) return false;
        // Check for valid hash length
        if (pwh.length() != HASH_LENGTH) return false;
        // Check for valid user name length
        if (userName.length() > 16 || userName.length() <= 0) return false;
        return true;
    }
}
