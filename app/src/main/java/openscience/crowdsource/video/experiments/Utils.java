package openscience.crowdsource.video.experiments;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniil on 11/16/16.
 */

public class Utils {
    //todo move out to ScenarioService
    public static String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1000;
        long megabyte = kilobyte * 1000;
        long gigabyte = megabyte * 1000;
        long terabyte = gigabyte * 1000;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return Math.round(1.0 * bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return Math.round(1.0 * bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return Math.round(1.0 * bytes / gigabyte) + " GB";

        } else if (bytes >= terabyte) {
            return Math.round(1.0 * bytes / terabyte) + " TB";

        } else {
            return bytes + " Bytes";
        }
    }


    /* get CPU frequencies */
    public static List<Double[]> get_cpu_freqs() {
        int num_procs = 0;

        List<Double[]> cpu_list = new ArrayList<Double[]>();

        JSONObject r = null;

        String xpath = "";
        String val = "";
        double fval = 0;

        for (int i = 0; i < 1024; i++) {

            Double[] cpu = new Double[3];

            // Check if online
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/online";

            val = read_one_string_file(xpath);
            if (val == null) break;

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val);

            cpu[0] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/cpuinfo_max_freq";

            val = read_one_string_file(xpath);
            if (val == null) val = "";

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val) / 1E3;

            cpu[1] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/scaling_cur_freq";

            val = read_one_string_file(xpath);
            if (val == null) val = "";

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val) / 1E3;

            cpu[2] = fval;

            //adding to final array
            cpu_list.add(cpu);
        }

        return cpu_list;
    }


    /*************************************************************************/
    /* read one string file */
    public static String read_one_string_file(String fname) {
        String ret = null;
        Boolean fail = false;

        BufferedReader fp = null;
        try {
            fp = new BufferedReader(new FileReader(fname));
        } catch (IOException ex) {
            fail = true;
        }

        if (!fail) {
            try {
                ret = fp.readLine();
            } catch (IOException ex) {
                fail = true;
            }
        }

        try {
            if (fp != null) fp.close();
        } catch (IOException ex) {
            fail = true;
        }

        return ret;
    }

    /*************************************************************************/
    /* read one string file */
    public static boolean save_one_string_file(String fname, String text) {

        FileOutputStream o = null;
        try {
            o = new FileOutputStream(fname, false);
        } catch (FileNotFoundException e) {
            return false;
        }

        OutputStreamWriter oo = new OutputStreamWriter(o);

        try {
            oo.append(text);
            oo.flush();
            oo.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }


    /* exchange info with CK server */
    public static JSONObject exchange_info_with_ck_server(JSONObject ii) {
        JSONObject r = null;

        try {
            r = openme.remote_access(ii);
        } catch (JSONException e) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error calling OpenME interface (" + e.getMessage() + ")");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        int rr = 0;
        if (!r.has("return")) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error obtaining key 'return' from OpenME output");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        try {
            Object rx = r.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error obtaining key 'return' from OpenME output (" + e.getMessage() + ")");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        //Update return with integer
        try {
            r.put("return", rr);
        } catch (JSONException e) {
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) r.get("error");
            } catch (JSONException e) {
                try {
                    r = new JSONObject();
                    r.put("return", 32);
                    r.put("error", "Error obtaining key 'error' from OpenME output (" + e.getMessage() + ")");
                } catch (JSONException e1) {
                    return null;
                }
            }
        }

        return r;
    }

    public static void createDirIfNotExist(String dirPath) {
        File externalSDCardFile = new File(dirPath);
        if (!externalSDCardFile.exists()) {
            if (!externalSDCardFile.mkdirs()) {
                AppLogger.logMessage("\nError creating dir (" + dirPath + ") ...\n\n");
                return;
            }
        }
    }

}
