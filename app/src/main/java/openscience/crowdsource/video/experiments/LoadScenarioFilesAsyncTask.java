package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static openscience.crowdsource.video.experiments.AppConfigService.COMMAND_CHMOD_744;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpensciencePath;
import static openscience.crowdsource.video.experiments.RecognitionScenarioService.loadScenariosJSONObjectFromFile;
import static openscience.crowdsource.video.experiments.RecognitionScenarioService.updateProgress;
import static openscience.crowdsource.video.experiments.Utils.downloadFileAndCheckMd5;

/**
 * Async process downloads scenario files from remote server
 *
 * @author Daniil Efremov
 */
public class LoadScenarioFilesAsyncTask extends AsyncTask<RecognitionScenario, String, String> {

    public void copy_bin_file(String src, String dst) throws IOException {
        File fin = new File(src);
        File fout = new File(dst);

        InputStream in = new FileInputStream(fin);
        OutputStream out = new FileOutputStream(fout);

        // Transfer bytes from in to out
        int l = 0;
        byte[] buf = new byte[16384];
        while ((l = in.read(buf)) > 0) out.write(buf, 0, l);

        in.close();
        out.close();
    }

    protected void onPostExecute(String x) {
        AppConfigService.AppConfig.State state = AppConfigService.getState();
        if (state == null || state.equals(AppConfigService.AppConfig.State.IN_PROGRESS)) {
            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        }
    }

    protected void onProgressUpdate(String... values) {
        if (values[0] != "") {
            AppLogger.logMessage(values[0]);
        } else if (values[1] != "") {
            AppLogger.logMessage("Error onProgressUpdate " + values[1]);
        }
    }

    @Override
    protected String doInBackground(RecognitionScenario... arg0) {

        final RecognitionScenario recognitionScenario = arg0[0];

        recognitionScenario.setState(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS);

        try {

            JSONObject scenariosJSON = loadScenariosJSONObjectFromFile();
            if (scenariosJSON == null) {
                publishProgress("\nUnfortunately, no scenarios found for your device ...");
                return null;
            }
            JSONArray scenarios = scenariosJSON.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                publishProgress("\nUnfortunately, no scenarios found for your device ...");

                return null;
            }


            File externalSDCardFile = new File(externalSDCardOpensciencePath);
            if (!externalSDCardFile.exists()) {
                if (!externalSDCardFile.mkdirs()) {
                    publishProgress("\nError creating dir (" + externalSDCardOpensciencePath + ") ...\n\n");
                    return null;
                }
            }

            if (scenarios.length() == 0) {
                publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
                return null;
            }

            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);

                scenario.getJSONObject("search_dict");
                scenario.getString("ignore_update");
                scenario.getString("search_string");
                JSONObject meta = scenario.getJSONObject("meta");
                String title = meta.getString("title");

                if (!recognitionScenario.getTitle().equalsIgnoreCase(title)) {
                    // skip other scenarios
                    continue;
                }

                JSONArray files = meta.getJSONArray("files");
                for (int j = 0; j < files.length(); j++) {
                    JSONObject file = files.getJSONObject(j);
                    String fileName = file.getString("filename");
                    String fileDir = externalSDCardOpensciencePath + file.getString("path");
                    File fp = new File(fileDir);
                    if (!fp.exists()) {
                        if (!fp.mkdirs()) {
                            publishProgress("\nError creating dir (" + fileDir + ") ...\n\n");
                            return null;
                        }
                    }

                    final String targetFilePath = fileDir + File.separator + fileName;
                    String finalTargetFilePath = targetFilePath;
                    String finalTargetFileDir = fileDir;
                    String url = file.getString("url");
                    String md5 = file.getString("md5");

                    if (downloadFileAndCheckMd5(
                            url,
                            targetFilePath,
                            md5,
                            new MainActivity.ProgressPublisher() {
                                @Override
                                public void setPercent(int percent) {
                                    String str="";

                                    if (percent<0) {
                                        str+="\n * Downloading file " + targetFilePath + " ...\n";
                                    } else {
                                        str += "  * " + percent + "%\n";
                                    }
                                    updateProgress(recognitionScenario);
                                    publishProgress(str);
                                }

                                @Override
                                public void addBytes(long bytes) {
                                    recognitionScenario.setDownloadedTotalFileSizeBytes(recognitionScenario.getDownloadedTotalFileSizeBytes() + bytes);
                                }

                                @Override
                                public void println(String text) {
                                    publishProgress("\n" + text + "\n");
                                }
                            })) {
                        String copyToAppSpace = null;
                        try {
                            copyToAppSpace = file.getString("copy_to_app_space");
                        } catch (JSONException e) {
                            // copyToAppSpace is not mandatory
                        }
                        if (copyToAppSpace != null && copyToAppSpace.equalsIgnoreCase("yes")) {
                            String fileAppDir = AppConfigService.getLocalAppPath() + file.getString("path");
                            File appfp = new File(fileAppDir);
                            if (!appfp.exists()) {
                                if (!appfp.mkdirs()) {
                                    publishProgress("\nError creating dir (" + fileAppDir + ") ...\n\n");
                                    return null;
                                }
                            }

                            final String targetAppFilePath = fileAppDir + File.separator + fileName;
                            try {
                                copy_bin_file(targetFilePath, targetAppFilePath);
                                finalTargetFileDir = fileAppDir;
                                finalTargetFilePath = targetAppFilePath;
                                publishProgress("\n * File " + targetFilePath + " sucessfully copied to " + targetAppFilePath + "\n\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                                publishProgress("\nError copying file " + targetFilePath + " to " + targetAppFilePath + " ...\n\n");
                                return null;
                            }

                        }

                        String executable = null;
                        try {
                            executable = file.getString("executable");
                        } catch (JSONException e) {
                            // executable is not mandatory
                        }
                        if (executable != null && executable.equalsIgnoreCase("yes")) {
                            String[] chmodResult = openme.openme_run_program(COMMAND_CHMOD_744 + " " + finalTargetFilePath, null, finalTargetFileDir);
                            if (chmodResult[0].isEmpty() && chmodResult[1].isEmpty() && chmodResult[2].isEmpty()) {
                                publishProgress(" * File " + finalTargetFilePath + " sucessfully set as executable ...\n");
                            } else {
                                publishProgress("\nError setting  file " + targetFilePath + " as executable ...\n\n");
                                return null;
                            }
                        }
                    }
                }

                publishProgress("\nPreloaded scenario info:  " + recognitionScenario.toString() + "\n\n");
            }

        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return null;
        }

        recognitionScenario.setState(RecognitionScenario.State.DOWNLOADED);
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        recognitionScenario.setLoadScenarioFilesAsyncTask(null);
        updateProgress(recognitionScenario);

        publishProgress("Finished downloading files for scenario " + recognitionScenario.getTitle() + " \n\n");
        return null;
    }
}