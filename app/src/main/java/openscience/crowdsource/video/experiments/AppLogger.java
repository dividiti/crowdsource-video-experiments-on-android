package openscience.crowdsource.video.experiments;

import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Daniil Efremov
 */

public class AppLogger {

    private final static String LOG_DIR = "/sdcard/openscience/tmp/"; //todo get log dir from common config service
    private final static String LOG_FILE_PATH = LOG_DIR + "log.txt";

    synchronized public static void cleanup() {
        File logFile = new File(LOG_FILE_PATH);
        if (logFile.exists()) {
           logFile.delete();
        }
    }

    private static Updater updater;

    synchronized public static void logMessage(String message) {
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            try {
                File logDir = new File(LOG_DIR);
                if (!logDir.exists()) {
                    if (!logDir.mkdirs()) {
                        // some problem with storage
                        return;
                    }
                }
                logFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(message + "\n\n");
            buf.newLine();
            buf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (updater != null) {
            updater.update(message);
        }

    }


    public static void registerTextView(Updater updaterNew) {
        updater = updaterNew;
    }


    public static void unregisterTextView() {
        updater = null;
    }


    synchronized public static void updateTextView(EditText editText) {
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            return;
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            editText.setText("");
            BufferedReader buf = new BufferedReader(new FileReader(logFile));
            while (true) {
                String text = buf.readLine();
                if (text == null){
                    break;
                }
                editText.append(text + "\n");
            }
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        editText.setSelection(editText.getText().length());
    }


    public interface Updater {
        void update(String message);
    }

    public static String getAllLogs() {
        StringBuilder allLogs = new StringBuilder();
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            return "";
        }
        try {
            BufferedReader buf = new BufferedReader(new FileReader(logFile));
            while (true) {
                String text = buf.readLine();
                if (text == null){
                    break;
                }
                allLogs.append(text).append("\n");
            }
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return allLogs.toString();
    }
}
