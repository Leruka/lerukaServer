package leruka;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by leif on 05.11.15.
 */
public class Log {

    private static final String PRE_INF = "# [INF|%s] ";
    private static final String PRE_ERR = "# [ERR|%s] ";
    private static final String PRE_WRN = "# [WRN|%s] ";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm");

    public static void inf(String msg) {
        System.out.println(logInfo(PRE_INF) + msg);
    }

    public static void err(String msg) {
        System.out.println(logInfo(PRE_ERR) + msg);
    }

    public static void wrn(String msg) {
        System.out.println(logInfo(PRE_WRN) + msg);
    }

    private static String logInfo(String warp) {
        return String.format(warp, dateFormat.format(new Date()));
    }

}
