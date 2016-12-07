package openscience.crowdsource.video.experiments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.http.conn.ConnectTimeoutException;
import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Common utilities for
 * * JSON processing
 * * string formating
 * * etc
 *
 * @author Daniil Efremov
 */
public class Utils {

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


    /**
     * returns get CPU frequencies
     */
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

            val = readOneStringFile(xpath);
            if (val == null) break;

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val);

            cpu[0] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/cpuinfo_max_freq";

            val = readOneStringFile(xpath);
            if (val == null) val = "";

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val) / 1E3;

            cpu[1] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/scaling_cur_freq";

            val = readOneStringFile(xpath);
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


    /**
     * reads one string file
     */
    public static String readOneStringFile(String fname) {
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

    /**
     * read one string file
     */
    public static boolean saveOneStringFile(String fname, String text) {

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


    /**
     * exchange info with CK server
     */
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

    public static String get_shared_computing_resource(String url) {
        String s = "";

        try {
            //Connect
            URL u = new URL(url);

            URLConnection urlc = u.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

            String line = "";
            while ((line = br.readLine()) != null)
                s += line + '\n';
            br.close();
        } catch (Exception e) {
            AppLogger.logMessage("Error shared computing resource is not reachable " + e.getLocalizedMessage() + "...\n\n");
            return null;
        }

            /* Trying to convert to dict from JSON */
        JSONObject a = null;

        try {
            a = new JSONObject(s);
        } catch (JSONException e) {
            AppLogger.logMessage("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
            return null;
        }

            /* For now just take default one, later addRecognitionScenario random or balancing */
        JSONObject rs = null;
        try {
            if (a.has("default"))
                rs = (JSONObject) a.get("default");
            if (rs != null) {
                if (rs.has("url"))
                    s = (String) rs.get("url");
            }
        } catch (JSONException e) {
            AppLogger.logMessage("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
            return null;
        }

        if (s == null)
            s = "";
        else if (!s.endsWith("?"))
            s += "/?";

        AppLogger.logMessage("\n");

        if (s.startsWith("ERROR")) {
            AppLogger.logMessage(s);
            AppLogger.logMessage("\n");
            return null;
        } else {
            AppLogger.logMessage("Public Collective Knowledge Server found:\n");
            AppLogger.logMessage(s);
            AppLogger.logMessage("\n");
        }

        return s;
    }


    private static final int CONNECTION_MAX_TRIES = 10;
    private static final int CONNECTION_DELAY_MS = 500;
    /**
     * Downloads File from povided urlString saves to file
     * with provided target file path and checks Md5 sum
     *
     * @param urlString
     * @param localPath
     * @param md5
     * @param progressPublisher
     * @return
     */
    public static boolean downloadFileAndCheckMd5(String urlString, String localPath, String md5, MainActivity.ProgressPublisher progressPublisher) {
        try {
            String existedlocalPathMD5 = fileToMD5(localPath);
            if (existedlocalPathMD5 != null && existedlocalPathMD5.equalsIgnoreCase(md5)) {
                File localFile = new File(localPath);
                progressPublisher.addBytes(localFile.length());
                return true;
            }

            int BUFFER_SIZE = 1024;
            for(int i = 0; i < CONNECTION_MAX_TRIES; i++) {
                try {
                    byte data[] = new byte[BUFFER_SIZE];
                    int count;

                    URL url = new URL(urlString);
                    URLConnection conection = url.openConnection();
                    conection.connect();

                    int lenghtOfFile = conection.getContentLength();
                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(localPath);


                    long total = 0;
                    int progressPercent = 0;
                    int prevProgressPercent = 0;

                    progressPublisher.setPercent(-1); // Print only text

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (lenghtOfFile > 0) {
                            progressPercent = (int) ((total * 100) / lenghtOfFile);
                        }
                        if (progressPercent != prevProgressPercent) {
                            progressPublisher.setPercent(progressPercent);
                            prevProgressPercent = progressPercent;
                        }
                        progressPublisher.addBytes(count);
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                    break;// successfully downloaded
                } catch (IOException e) {
                    progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
                    Thread.sleep(CONNECTION_DELAY_MS);
                    AppLogger.logMessage("Trying to reconnect " + i + " of " + CONNECTION_MAX_TRIES);
                }
            }

            String gotMD5 = fileToMD5(localPath);
            if (!gotMD5.equalsIgnoreCase(md5)) {
                progressPublisher.println("ERROR: MD5 is not satisfied, please try again.");
                return false;
            } else {
                progressPublisher.println("File successfully downloaded from " + urlString + " to local files " + localPath);
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        }
    }

    public static String getCachedMD5(String filePath) {
        String md5FilePath = filePath + ".md5";
        String md5Cached = Utils.readOneStringFile(md5FilePath);
        if (md5Cached != null) {
            return md5Cached;
        }
        return "";
    }

    /**
     * @param filePath
     * @return md5 summ for provided file
     */
    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            String md5FilePath = filePath + ".md5";
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            String md5 = convertHashToString(md5Bytes);
            Utils.saveOneStringFile(md5FilePath, md5);
            return md5;
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    public static boolean validateReturnCode(JSONObject r) {
        int rr = 0;
        if (!r.has("return")) {
            AppLogger.logMessage("Error obtaining key 'return' from OpenME output ...");
            return true;
        }

        try {
            Object rx = r.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            AppLogger.logMessage("Error obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...");
            return true;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) r.get("error");
            } catch (JSONException e) {
                AppLogger.logMessage("Error obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...");
                return true;
            }

            AppLogger.logMessage("Problem accessing CK server: " + err);
            return true;
        }
        return false;
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

    public static Bitmap decodeSampledBitmapFromResource(String imagePath,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        return BitmapFactory.decodeFile(imagePath, options);
    }
}
