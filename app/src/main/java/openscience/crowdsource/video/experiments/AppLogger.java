package openscience.crowdsource.video.experiments;

import android.widget.EditText;

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


    synchronized public static void logMessage(String message) {
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            try {
                File externalSDCardFile = new File(LOG_DIR);
                if (!externalSDCardFile.mkdirs()) {
                    return ;
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
            buf.append("\n" + message + "\n\n");
            buf.newLine();
            buf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
                editText.append(text);
            }
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        editText.setSelection(editText.getText().length());
    }
}
