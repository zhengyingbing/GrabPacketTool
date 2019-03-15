package grabpacket.bryan.com.grabpackettool;

import android.util.Log;

/**
 * 作者：Bryan
 * 时间：2018/10/30 17:29
 */

public class Logger {

    private static String TAG = "wx";

    public static void d(String value){
        Log.d(TAG, value);
    }

    public static void i(String value){
        Log.i(TAG, value);
    }

    public static void e(String value){
        Log.e(TAG, value);
    }
}
