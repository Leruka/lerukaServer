package leruka.user;

import java.util.*;

/**
 * The SessionManager is used to keep track of current sessions. When a user logs in, he should receive a session ID
 * which is used to skip further authentications.
 */
public class SessionManager {

    /** The duration until a session expires in ms */
    private static final long SESSION_DURATION = 3600000;
    /** The list of active sessions */
    private static List<Session> sessions;

    static {
        sessions = new ArrayList<>();
    }

    /**
     * Creates a new Session and returns the new Session ID
     * @return The ID of the new Session
     */
    static UUID createSession(int userID) {
        Session session = new Session(userID);
        sessions.add(session);
        return session.getSid();
    }

    /**
     * Checks, if the Session ID is in the list of active sessions. If it is, the expiration timer will be reset.
     * @param sid The ID of the Session
     * @return true if the session is active, else false
     */
    static boolean isInSession(UUID sid) {
        for (Session s : sessions) {
            if (s.getSid().equals(sid)) {
                s.resetSessionTime();
                return true;
            }
        }
        return false;
    }

    /**
     * Checks, if the Session ID is in the list of active sessions and returns the corresponding userID. Furthermore,
     * the expiration timer will be reset. If the Session ID is not in the list, -1 will be returned.
     * @param sid The ID of the Session
     * @return The userID bound to the Session or -1 if the Session is invalid
     */
    static int getUserID(UUID sid) {
        for (Session s : sessions) {
            if (s.getSid().equals(sid)) {
                s.resetSessionTime();
                return s.getUserID();
            }
        }
        return -1;
    }

    /**
     * Ends a session by the Session object itself. The Session will be removed from the active sessions list.
     * @param sid The ID of the session to end
     */
    static void endSession(UUID sid) {
        for (Session s : sessions) {
            if (s.getSid().equals(sid)) {
                endSession(s);
            }
        }
    }

    /**
     * Ends a session by the Session object itself. The Session will be removed from the active sessions list.
     * @param session The Session to end
     */
    static void endSession(Session session) {
        session.quitExpiration();
        sessions.remove(session);
    }

    /**
     * Private class for the sessions objects. These hold the session ID and a Task which is able to end itself.
     * <b>Warning:</b> The two constructors do different things: new Session() generates a new Session object, while
     * new Session(sid) is only used to compare if the Session equals another.
     */
    static class Session {

        private int userID;
        private UUID sid;
        private TimerTask expireTask;
        private Timer expireTimer;

        Session(int userID) {
            this.userID = userID;
            // Generate an new session id
            this.sid = UUID.randomUUID();
            // create the expire task and timer
            this.expireTask = new TimerTask() {
                @Override
                public void run() {
                    endSession(Session.this);
                }
            };
            this.expireTimer = new Timer();
            // schedule the expiration
            this.expireTimer.schedule(this.expireTask, SESSION_DURATION);
        }

        /**
         * Creates a new <b>fake</b> Session object. This should only be used with the equals method to check if
         * a other Session object has the same UUID. No expire Task will be created.
         * @param sid the session ID to use.
         */
        Session(UUID sid) {
            this.sid = sid;
            this.expireTask = null;
            this.expireTimer = null;
        }

        /**
         * Resets the duration until the session expires. This should be called, when the user shows that he is active.
         */
        void resetSessionTime() {
            this.quitExpiration();
            this.expireTimer.schedule(this.expireTask, SESSION_DURATION);
        }

        /**
         * cancels the scheduled session expiration.
         */
        void quitExpiration() {
            this.expireTimer.cancel();
            this.expireTimer.purge();
        }

        /**
         * Returns the Session id.
         * @return The Session id of the Session object
         */
        UUID getSid() {
            return sid;
        }

        /**
         * Returns the UserID which is bound to the Session.
         * @return The corresponding userID
         */
        int getUserID() {
            return this.userID;
        }

        /**
         * Checks, if another Object is equal to this instance. Two Session are equal to each other, when the
         * Session IDs are the same.
         * @param obj The Object to check for
         * @return true, if the Session IDs are the same, else false
         */
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Session other = (Session) obj;
            return other.getSid().equals(getSid());
        }

        /**
         * Returns a hash for the Session Object. Two Sessions with the same hash code are equal to each other.
         * The hash is the same as the hash of the session ID.
         * @return a hash of the Session
         */
        public int hashCode() {
            return sid.hashCode();
        }


    }

}
