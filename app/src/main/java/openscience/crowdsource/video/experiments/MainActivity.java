/*

# video experiments using mobile devices
# provided by volunteers
#
# (C)opyright, dividiti
# 2016
# BSD 3-clause license
#
# Powered by Collective Knowledge
# http://github.com/ctuning/ck

# Developers: Daniil Efremov and Grigori Fursin

*/

package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static openscience.crowdsource.video.experiments.AppConfigService.COMMAND_CHMOD_744;
import static openscience.crowdsource.video.experiments.AppConfigService.cachedScenariosFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpensciencePath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpenscienceTmpPath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardPath;
import static openscience.crowdsource.video.experiments.AppConfigService.initAppConfig;
import static openscience.crowdsource.video.experiments.AppConfigService.url_cserver;
import static openscience.crowdsource.video.experiments.RecognitionScenarioService.PRELOADING_TEXT;
import static openscience.crowdsource.video.experiments.Utils.createDirIfNotExist;
import static openscience.crowdsource.video.experiments.Utils.validateReturnCode;

/**
 * Main screen with main feature: run recognition process
 *
 * @author Daniil Efremov
 */
public class MainActivity extends android.app.Activity implements GLSurfaceView.Renderer {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;


    private static final String welcome = "This application let you participate in experiment crowdsourcing " +
            "to collaboratively solve complex problems! " +
            "Please, press 'Update' button to obtain shared scenarios such as " +
            "collaborative benchmarking, optimization and tuning of a popular Caffe CNN image recognition library!\n" +
            "NOTE: you should have an unlimited Internet since some scenario may require to download 300Mb+ code and datasets! " +
            "Also some anonymized statistics will be collected about your platform and code execution " +
            "(performance, accuracy, power consumption, cost, etc) at cknowledge.org/repo " +
            "to let the community improve algorithms for diverse hardware!\n\n";

    private static final String s_line = "====================================\n";

    private Button btnOpenImage;

    private GLSurfaceView glSurfaceView;

    static String pf_gpu = "";
    static String pf_gpu_vendor = "";

    private GoogleApiClient client;

    String curlCached;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client2;

    Camera camera;
    boolean isCameraStarted = false;

    private Button startStopCam;

    private Button recognize;

    private ImageView imageView;
    private EditText consoleEditText;

    /**
     * @return absolute path to image
     */
    private void captureImageFromCameraPreviewAndPredict(final boolean isPredictionRequired) {
        synchronized (camera) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        createDirIfNotExist(externalSDCardOpenscienceTmpPath);
                        String takenPictureFilPath = String.format(externalSDCardOpenscienceTmpPath + File.separator + "%d.jpg", System.currentTimeMillis());
                        FileOutputStream fos = new FileOutputStream(takenPictureFilPath);
                        fos.write(data);
                        fos.close();
                        stopCameraPreview();

                        rotateImageAccoridingToOrientation(takenPictureFilPath);

                        AppConfigService.updateActualImagePath(takenPictureFilPath);
                        if (isPredictionRequired) {
                            predictImage(takenPictureFilPath);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        AppLogger.logMessage("Error on image capture " + e.getLocalizedMessage());

                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        AppLogger.logMessage("Error on image capture " + e.getLocalizedMessage());
                    }
                }
            });
        }
    }

    private void rotateImageAccoridingToOrientation(String takenPictureFilPath) {
        Bitmap bmp = BitmapFactory.decodeFile(takenPictureFilPath);
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(getImageDegree(takenPictureFilPath));

        int startX = 0;
        int startY = 0;
        int width = bmp.getWidth();
        int endX = width;
        int height = bmp.getHeight();
        int endY = height;

        if (height > width) {
            startY =  Math.round((height - width)/2);
            endY =  startY + width;
        } if (height < width) {
            startX =  Math.round((width - height)/2);
            endX =  startX + height;
        }
        Bitmap rbmp = Bitmap.createBitmap(bmp, startX, startY, endX, endY, rotationMatrix, true);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(takenPictureFilPath);
            rbmp.compress(Bitmap.CompressFormat.JPEG, 60, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
            AppLogger.logMessage("Error on picture taking " + e.getLocalizedMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                AppLogger.logMessage("Error on picture taking " + e.getLocalizedMessage());
            }
        }
    }

    private void stopCameraPreview() {
        if (isCameraStarted) {
            if (camera != null) {
                camera.release();
            }
            camera = null;
            isCameraStarted = false;
        }
    }

    private RecognitionScenario getSelectedRecognitionScenario() {
        return RecognitionScenarioService.getSelectedRecognitionScenario();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTaskBarColored(this);

        Button consoleButton = (Button) findViewById(R.id.btn_consoleMain);
        consoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent logIntent = new Intent(MainActivity.this, ConsoleActivity.class);
                startActivity(logIntent);
            }
        });


        Button infoButton = (Button) findViewById(R.id.btn_infoMain);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aboutIntent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(aboutIntent);
            }
        });

        initConsole();

        startStopCam = (Button) findViewById(R.id.btn_capture);
        startStopCam.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                createDirIfNotExist(externalSDCardOpenscienceTmpPath);
                String takenPictureFilPath = String.format(externalSDCardOpenscienceTmpPath + File.separator + "%d.jpg", System.currentTimeMillis());
                AppConfigService.updateActualImagePath(takenPictureFilPath);
                Intent aboutIntent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(aboutIntent);
            }
        });


        recognize = (Button) findViewById(R.id.suggest);
        recognize.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final RecognitionScenario recognitionScenario = RecognitionScenarioService.getSelectedRecognitionScenario();
                if (recognitionScenario == null) {
                    AppLogger.logMessage(" Scenarios was not selected! Please select recognitions scenario first! \n");
                    return;
                }

                if (recognitionScenario.getState() == RecognitionScenario.State.NEW) {
                    AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    clarifyDialogBuilder.setMessage(Html.fromHtml("You should download scenario files first or select another one"))
                            .setCancelable(false)
                            .setPositiveButton("continue",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            LoadScenarioFilesAsyncTask loadScenarioFilesAsyncTask = new LoadScenarioFilesAsyncTask();
                                            loadScenarioFilesAsyncTask.execute(recognitionScenario);
                                            recognitionScenario.setLoadScenarioFilesAsyncTask(loadScenarioFilesAsyncTask);
                                            Intent mainIntent = new Intent(MainActivity.this, ScenariosActivity.class);
                                            startActivity(mainIntent);
                                        }
                                    })
                            .setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
                    clarifyDialog.show();
                    return;
                }

                if (recognitionScenario.getState() == RecognitionScenario.State.DOWNLOADING_IN_PROGRESS) {
                    AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    clarifyDialogBuilder.setMessage(Html.fromHtml("Downloading is progress now, please wait"))
                            .setCancelable(false)
                            .setPositiveButton("continue",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                    ;
                    final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
                    clarifyDialog.show();
                    return;
                }


                if (isCameraStarted) {
                    captureImageFromCameraPreviewAndPredict(true);
                    return;
                }

                // Call prediction
                predictImage(AppConfigService.getActualImagePath());
            }
        });

        final View selectedScenarioTopBar = findViewById(R.id.selectedScenarioTopBar);
        selectedScenarioTopBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectScenario = new Intent(MainActivity.this, ScenariosActivity.class);
                startActivity(selectScenario);

            }
        });
        selectedScenarioTopBar.setEnabled(false);

        final TextView selectedScenarioText = (TextView)findViewById(R.id.selectedScenarioText);
        selectedScenarioText.setText(PRELOADING_TEXT);

        imageView = (ImageView) findViewById(R.id.imageView1);

        btnOpenImage = (Button) findViewById(R.id.btn_ImageOpen);
        btnOpenImage.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        // Lazy preload scenarios
        RecognitionScenarioService.initRecognitionScenariosAsync(new RecognitionScenarioService.ScenariosUpdater() {
            @Override
            public void update() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RecognitionScenario selectedRecognitionScenario = RecognitionScenarioService.getSelectedRecognitionScenario();
                        selectedScenarioText.setText(selectedRecognitionScenario.getTitle());
                        updateViewFromState();
                    }
                });
            }
        });



        SharedPreferences sharedPreferences = getSharedPreferences(AppConfigService.CROWDSOURCE_VIDEO_EXPERIMENTS_ON_ANDROID_PREFERENCES, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(AppConfigService.SHARED_PREFERENCES, true)) {
            AppLogger.logMessage(welcome);
            sharedPreferences.edit().putBoolean(AppConfigService.SHARED_PREFERENCES, false).apply();
        }

        this.glSurfaceView = new GLSurfaceView(this);
        this.glSurfaceView.setRenderer(this);


        initAppConfig(this);

        client2 = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        final TextView resultPreviewText = (TextView) findViewById(R.id.resultPreviewtText);
        resultPreviewText.setText(AppConfigService.getPreviewRecognitionText());
        AppConfigService.registerPreviewRecognitionText(new AppConfigService.Updater() {
            @Override
            public void update(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        View imageButtonsBar = (View) findViewById(R.id.imageButtonBar);
                        imageButtonsBar.setVisibility(View.VISIBLE);
                        imageButtonsBar.setEnabled(true);
                        resultPreviewText.setText(message);
                    }
                });

            }
        });

        updateViewFromState();
    }

    private void initConsole() {
        consoleEditText = (EditText) findViewById(R.id.consoleEditText);
        AppLogger.updateTextView(consoleEditText);
        registerLogerViewerUpdater();
        consoleEditText.setVisibility(View.GONE);
    }

    private void registerLogerViewerUpdater() {
        AppLogger.registerTextView(new AppLogger.Updater() {
            @Override
            public void update(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppLogger.updateTextView(consoleEditText);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViewFromState();
    }

    private void updateViewFromState() {
        registerLogerViewerUpdater();

        String actualImagePath = AppConfigService.getActualImagePath();
        if (actualImagePath != null) {
            updateImageView(actualImagePath);
        }
        AppConfigService.AppConfig.State state = AppConfigService.getState();
        if (state.equals(AppConfigService.AppConfig.State.IN_PROGRESS) || state.equals(AppConfigService.AppConfig.State.PRELOAD)) {
            updateControlStatusPreloading(false);
        } else if (state.equals(AppConfigService.AppConfig.State.READY)) {
            updateControlStatusPreloading(true);
        } else if (state.equals(AppConfigService.AppConfig.State.RESULT)) {
            updateControlStatusPreloading(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraPreview();
    }

    public static void setTaskBarColored(android.app.Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = context.getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // todo: resolve default top bar height issue or find enother way to change top bar color
//            int statusBarHeight = 50;
            View view = new View(context);
//            view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//            view.getLayoutParams().height = statusBarHeight;
//            ((ViewGroup) w.getDecorView()).addView(view);
            view.setBackgroundColor(context.getResources().getColor(R.color.colorStatusBar));
        }
    }

    private void updateControlStatusPreloading(boolean isEnable) {
        startStopCam.setEnabled(isEnable);
        recognize.setEnabled(isEnable);
        btnOpenImage.setEnabled(isEnable);
        View selectedScenarioTopBar = findViewById(R.id.selectedScenarioTopBar);
        selectedScenarioTopBar.setEnabled(isEnable);
        final TextView resultPreviewText = (TextView) findViewById(R.id.resultPreviewtText);
        if (!isEnable) {
            consoleEditText.setVisibility(View.VISIBLE);
            recognize.setVisibility(View.GONE);
            startStopCam.setVisibility(View.GONE);
            btnOpenImage.setVisibility(View.GONE);
            resultPreviewText.setText(AppConfigService.PLEASE_WAIT);
            AppConfigService.updatePreviewRecognitionText(AppConfigService.PLEASE_WAIT);
        } else {
            consoleEditText.setVisibility(View.GONE);
            recognize.setVisibility(View.VISIBLE);
            startStopCam.setVisibility(View.VISIBLE);
            btnOpenImage.setVisibility(View.VISIBLE);
            resultPreviewText.setText("");
            AppConfigService.updatePreviewRecognitionText(null);
        }
    }

    /**
     * get CPU frequencies JSON
     */
    private JSONObject getCPUFreqsJSON(List<Double[]> cpus) {
        Double[] cpu = null;
        int cpu_num = cpus.size();

        JSONObject freq_max = new JSONObject();
        for (int i = 0; i < cpu_num; i++) {
            String x = "    " + Integer.toString(i) + ") ";
            cpu = cpus.get(i);
            double x1 = cpu[1];
            if (x1 != 0) {
                try {
                    freq_max.put(Integer.toString(i), x1);
                } catch (JSONException e) {

                }
            }
        }
        return freq_max;
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        pf_gpu_vendor = gl10.glGetString(GL10.GL_VENDOR);
        if (pf_gpu_vendor.equals("null")) pf_gpu_vendor = "";

        String x = gl10.glGetString(GL10.GL_RENDERER);
        if (x.equals("null")) pf_gpu = "";
        else pf_gpu = pf_gpu_vendor + " " + x;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                glSurfaceView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        // no-op
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // no-op
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2.connect();
        AppIndex.AppIndexApi.start(client2, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client2, getIndexApiAction());
        client2.disconnect();
        AppLogger.unregisterTextView();
    }

    /*************************************************************************/
    private class RunRecognitionAsync extends AsyncTask<String, String, String> {

        /*************************************************************************/
        public String get_shared_computing_resource(String url) {
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
                publishProgress("Error shared computing resource is not reachable " + e.getLocalizedMessage() + "...\n\n");
                return null;
            }

            /* Trying to convert to dict from JSON */
            JSONObject a = null;

            try {
                a = new JSONObject(s);
            } catch (JSONException e) {
                publishProgress("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
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
                publishProgress("ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n");
                return null;
            }

            if (s == null)
                s = "";
            else if (!s.endsWith("?"))
                s += "/?";

            publishProgress("\n");

            if (s.startsWith("ERROR")) {
                publishProgress(s);
                publishProgress("\n");
                return null;
            } else {
                publishProgress("Public Collective Knowledge Server found:\n");
                publishProgress(s);
                publishProgress("\n");
            }

            return s;
        }

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
            updateControlStatusPreloading(true);
        }

        protected void onProgressUpdate(String... values) {
            if (values[0] != "") {
                AppLogger.logMessage(values[0]);
            } else if (values[1] != "") {
                AppLogger.logMessage("Error onProgressUpdate " + values[1]);
            }
        }

        @Override
        protected String doInBackground(String... arg0) {
            AppConfigService.updatePreviewRecognitionText("Recognizing ...");

            // Sending request to CK server to obtain available scenarios
            publishProgress("\n    Sending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...");

            JSONObject scenariosJSON = RecognitionScenarioService.loadScenariosJSONObjectFromFile();

            JSONObject r = scenariosJSON;
            if (scenariosJSON == null) {
                JSONObject availableScenariosRequest = new JSONObject();


                try {
                    if (getCurl() == null) {
                        publishProgress("\n Error we could not load scenarios from Collective Knowledge server: it's not reachible ...");
                        return null;
                    }
                    availableScenariosRequest.put("remote_server_url", getCurl());
                    availableScenariosRequest.put("action", "get");
                    availableScenariosRequest.put("module_uoa", "experiment.scenario.mobile");
                    availableScenariosRequest.put("email", AppConfigService.getEmail());
                    JSONObject platformFeatures = PlatformFeaturesService.loadPlatformFeatures();
                    availableScenariosRequest.put("platform_features", platformFeatures);
                    availableScenariosRequest.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...");
                    return null;
                }

                try {
                    r = openme.remote_access(availableScenariosRequest);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...");
                    return null;
                }

                if (validateReturnCode(r)) return null;
                scenariosJSON = r;
                try {
                    openme.openme_store_json_file(scenariosJSON, cachedScenariosFilePath);
                } catch (JSONException e) {
                    publishProgress("\nError writing preloaded scenarios to file (" + e.getMessage() + ") ...");
                }
            }

            if (scenariosJSON != null) {
                r = scenariosJSON;
            }

            try {
                JSONArray scenarios = r.getJSONArray("scenarios");
                if (scenarios.length() == 0) {
                    publishProgress("\nUnfortunately, no scenarios found for your device ...");
                    return null;
                }

                File externalSDCardFile = new File(externalSDCardOpensciencePath);
                if (!externalSDCardFile.exists()) {
                    if (!externalSDCardFile.mkdirs()) {
                        publishProgress("\nError creating dir (" + externalSDCardOpensciencePath + ") ...");
                        return null;
                    }
                }

                String libPath = null;
                String executablePath = null;
                String defaultImageFilePath = null;
                if (scenarios.length() == 0) {
                    publishProgress("\nUnfortunately, no scenarios found for your device ...");
                    return null;
                }

                for (int i = 0; i < scenarios.length(); i++) {
                    JSONObject scenario = scenarios.getJSONObject(i);
                    final String module_uoa = scenario.getString("module_uoa");
                    final String dataUID = scenario.getString("data_uid");
                    final String data_uoa = scenario.getString("data_uoa");

                    scenario.getJSONObject("search_dict");
                    scenario.getString("ignore_update");
                    scenario.getString("search_string");
                    JSONObject meta = scenario.getJSONObject("meta");
                    String title = meta.getString("title");
                    Long sizeBytes = Long.valueOf(0);
                    String sizeMB = "";
                    try {
                        String sizeB = scenario.getString("total_file_size");
                        sizeBytes = Long.valueOf(sizeB);
                        sizeMB = Utils.bytesIntoHumanReadable(Long.valueOf(sizeB));
                    } catch (JSONException e) {
                        publishProgress("Warn loading scenarios from file " + e.getLocalizedMessage());
                    }

                    final RecognitionScenario selectedRecognitionScenario = getSelectedRecognitionScenario();
                    if ((selectedRecognitionScenario == null || !selectedRecognitionScenario.getTitle().equalsIgnoreCase(title))) {
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
                                publishProgress("\nError creating dir (" + fileDir + ") ...");
                                return null;
                            }
                        }

                        final String targetFilePath = fileDir + File.separator + fileName;
                        String finalTargetFilePath = targetFilePath;
                        String finalTargetFileDir = fileDir;
                        String url = file.getString("url");
                        String md5 = file.getString("md5");
                        if (Utils.downloadFileAndCheckMd5(
                                url,
                                targetFilePath,
                                md5,
                                new ProgressPublisher() {
                                    @Override
                                    public void setPercent(int percent) {
                                        String str="";

                                        if (percent<0) str+="\n * Downloading file " + targetFilePath + " ...\n";
                                        else  str+="  * "+percent+"%\n";

                                        publishProgress(str);
                                    }

                                    @Override
                                    public void addBytes(long bytes) {
                                        selectedRecognitionScenario.setDownloadedTotalFileSizeBytes(selectedRecognitionScenario.getTotalFileSizeBytes() + bytes);
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
                                String localAppPath =  AppConfigService.getLocalAppPath() + File.separator + "openscience" + File.separator;
                                String fileAppDir = localAppPath + file.getString("path");
                                File appfp = new File(fileAppDir);
                                if (!appfp.exists()) {
                                    if (!appfp.mkdirs()) {
                                        publishProgress("\nError creating dir (" + fileAppDir + ") ...");
                                        return null;
                                    }
                                }

                                final String targetAppFilePath = fileAppDir + File.separator + fileName;
                                try {
                                    copy_bin_file(targetFilePath, targetAppFilePath);
                                    finalTargetFileDir = fileAppDir;
                                    finalTargetFilePath = targetAppFilePath;
                                    publishProgress("\n * File " + targetFilePath + " sucessfully copied to " + targetAppFilePath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    publishProgress("\nError copying file " + targetFilePath + " to " + targetAppFilePath + " ...");
                                    return null;
                                }

                            }

                            String executable = null;
                            String library = null;
                            try {
                                executable = file.getString("executable");
                                library = file.getString("library");
                            } catch (JSONException e) {
                                // executable is not mandatory
                            }
                            if (executable != null && executable.equalsIgnoreCase("yes")) {
                                if (library != null && library.equalsIgnoreCase("yes")) {
                                    if (libPath != null) {
                                        libPath = libPath + ":" + finalTargetFileDir;
                                    } else {
                                        libPath = finalTargetFileDir;
                                    }
                                } else {
                                    executablePath = finalTargetFileDir;
                                }
                                String[] chmodResult = openme.openme_run_program(COMMAND_CHMOD_744 + " " + finalTargetFilePath, null, finalTargetFileDir);
                                if (chmodResult[0].isEmpty() && chmodResult[1].isEmpty() && chmodResult[2].isEmpty()) {
                                    publishProgress(" * File " + finalTargetFilePath + " sucessfully set as executable ...\n");
                                } else {
                                    publishProgress("\nError setting  file " + targetFilePath + " as executable ...");
                                    return null;
                                }
                            }
                        } else {
                            publishProgress("\nError downloading  file " + targetFilePath + " from URL " + url);
                            return null;
                        }

                        String default_image = null;
                        try {
                            default_image = file.getString("default_image");
                        } catch (JSONException e) {
                            // executable is not mandatory
                        }
                        if (default_image != null && default_image.equalsIgnoreCase("yes")) {
                            defaultImageFilePath = finalTargetFilePath;
                        }
                    }

                    String actualImageFilePath = AppConfigService.getActualImagePath();
                    if (actualImageFilePath == null || !(new File(actualImageFilePath)).exists()) {
                        actualImageFilePath = defaultImageFilePath;
                        AppConfigService.updateActualImagePath(actualImageFilePath);
                    }

                    if (actualImageFilePath == null) {
                        publishProgress("\nError image file path was not initialized.");
                        return null;
                    }

                    if (libPath == null) {
                        publishProgress("\nError lib path was not initialized.");
                        return null;
                    }

                    String scenarioCmd = meta.getString("cmd");

                    String[] scenarioEnv = {
                            "CT_REPEAT_MAIN=" + String.valueOf(1),
                            "LD_LIBRARY_PATH=" + libPath + ":$LD_LIBRARY_PATH",
                    };
                    publishProgress("Prepared scenario env " +  scenarioEnv[0]);
                    publishProgress("Prepared scenario env " +  scenarioEnv[1]);
                    publishProgress("Scenario executable path " +  executablePath);

                    scenarioCmd = scenarioCmd.replace("$#local_path#$", externalSDCardPath + File.separator);
                    scenarioCmd = scenarioCmd.replace("$#image#$", actualImageFilePath);

                    publishProgress("Executing scenario command " +  scenarioCmd);

                    final ImageInfo imageInfo = getImageInfo(actualImageFilePath);
                    if (imageInfo == null) {
                        publishProgress("\n Error: Image was not found...");
                        return null;
                    } else {
                        publishProgress("\nProcessing image path: " + imageInfo.getPath() + "\n");
                        publishProgress("\nDetecting image height: " + imageInfo.getHeight() + "\n");
                        publishProgress("Detecting image width: " + imageInfo.getWidth() + "\n");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateImageView(imageInfo.getPath());
                            }
                        });
                    }

                    publishProgress("\nSelected scenario: " + title + "");

                    //In the future we may read json output and aggregate it too (openMe)
                    int iterationNum = 3; // todo it could be taken from loaded scenario
                    List<Long> processingTimes = new LinkedList<>();
                    List<List<Double[]>> cpuFreqs = new LinkedList<>();
                    JSONArray fineGrainTimerJSONArray = new JSONArray();
                    String recognitionResultText = null;
                    for (int it = 0; it <= iterationNum; it ++) {
                        if (it == 0) {
                            publishProgress("Recognition in process (mobile warms up) ...\n");
                        } else {
                            publishProgress("Recognition in process (statistical repetition: " + it + " out of " + iterationNum + ") ...\n");
                        }
                        long startTime = System.currentTimeMillis();
                        String[] recognitionResult = openme.openme_run_program(scenarioCmd, scenarioEnv, executablePath); //todo fix ck response cmd value: addRecognitionScenario appropriate path to executable from according to path value at "file" json
                        Long processingTime = System.currentTimeMillis() - startTime;
                        recognitionResultText = recognitionResult[1]; // todo it better to compare recognition results and print error
                        if (recognitionResultText == null || recognitionResultText.trim().equals("")) {
                            publishProgress("\nError Recognition result is empty ...\n");
                            if (recognitionResult.length>=1 && recognitionResult[0] != null && !recognitionResult[0].trim().equals("")) {
                                publishProgress("\nRecognition errors: " + recognitionResult[0]);
                            }
                            if (recognitionResult.length>=3 && recognitionResult[2] != null && !recognitionResult[2].trim().equals("")) {
                                publishProgress("\nRecognition errors: " + recognitionResult[2]);
                            }
                            return null;
                        }
                        if (it == 0) {
                            //  first iteration used for mobile warms up if it was in a low freq state
                            publishProgress(" * Recognition time  (warming up) " + processingTime + " ms \n");
                            publishProgress("\nRecognition result (warming up):\n " + recognitionResultText);
                            AppConfigService.updatePreviewRecognitionText(recognitionResultText);
                            continue;
                        }
                        publishProgress(" * Recognition time " + it + ": " + processingTime + " ms \n");
                        cpuFreqs.add(Utils.get_cpu_freqs());
                        processingTimes.add(processingTime);

                        try {
                            JSONObject fineGrainTimers = openme.openme_load_json_file(executablePath + File.separator + "tmp-ck-timer.json");
                            fineGrainTimerJSONArray.put(it - 1, fineGrainTimers.getJSONObject("dict"));
                        } catch (JSONException e) {
                            publishProgress("Error on reading fine-grain timers" + e.getLocalizedMessage());
                        }
                    }
                    publishProgress("\nRecognition result:" + recognitionResultText);

                    publishProgress("Submitting results and unexpected behavior (if any) to Collective Knowledge Aggregator ...\n");

                    if (getCurl() == null) {
                        publishProgress("\n Error we could not submit recognition results to Collective Knowledge server: it's not reachible ...");
                        return null;
                    }
                    JSONObject publishRequest = new JSONObject();
                    try {
                        JSONObject results = new JSONObject();
                        JSONArray processingTimesJSON = new JSONArray(processingTimes);
                        results.put("xopenme", fineGrainTimerJSONArray);
                        results.put("time", processingTimesJSON);
                        results.put("prediction", recognitionResultText);

                        results.put("image_width", imageInfo.getWidth());
                        results.put("image_height", imageInfo.getHeight());

                        publishRequest.put("remote_server_url", getCurl()); //
                        publishRequest.put("out", "json");
                        publishRequest.put("action", "process");
                        publishRequest.put("module_uoa", "experiment.bench.dnn.mobile");

                        publishRequest.put("email", AppConfigService.getEmail());
                        publishRequest.put("crowd_uid", dataUID);

                        JSONObject platformFeatures = PlatformFeaturesService.loadPlatformFeatures();
                        publishRequest.put("platform_features", platformFeatures);
                        publishRequest.put("raw_results", results);

                        publishRequest.put("cpu_freqs_before", getCPUFreqsJSON(cpuFreqs.get(0)));
                        publishRequest.put("cpu_freqs_after", getCPUFreqsJSON(cpuFreqs.get(cpuFreqs.size()-1)));
                    } catch (JSONException e) {
                        publishProgress("\nError with JSONObject ...");
                        return null;
                    }

                    publishProgress("Request to server " + publishRequest.toString(4));
                    JSONObject response;
                    try {
                        response = openme.remote_access(publishRequest);
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...");
                        return null;
                    }

                    int responseCode = 0;
                    if (!response.has("return")) {
                        publishProgress("\nError obtaining key 'return' from OpenME output ...");
                        return null;
                    }

                    try {
                        Object rx = response.get("return");
                        if (rx instanceof String) responseCode = Integer.parseInt((String) rx);
                        else responseCode = (Integer) rx;
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...");
                        return null;
                    }

                    if (responseCode > 0) {
                        String err = "";
                        try {
                            err = (String) response.get("error");
                        } catch (JSONException e) {
                            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...");
                            return null;
                        }

                        publishProgress("\nProblem accessing CK server: " + err + "\n");
                        return null;
                    }

                    String status = "";
                    String data_uid = "";
                    String behavior_uid = "";
                    String resultURL = AppConfigService.URL_CROWD_RESULTS;

                    try {
                        status = (String) response.get("status");
                        data_uid = (String) response.get("data_uid");
                        behavior_uid = (String) response.get("behavior_uid");
                        resultURL = (String) response.get("result_url");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'status' from OpenME output (" + e.getMessage() + ") ...");
                    }

                    AppConfigService.updateResultURL(resultURL);
                    publishProgress('\n' + status + '\n');

                    showIsThatCorrectDialog(recognitionResultText, actualImageFilePath, data_uid, behavior_uid, dataUID);
                }
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...");
                return null;
            }

            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
            return null;
        }

        private String getCurl() {
            /*********** Obtaining CK server **************/
            if (curlCached == null) {
                publishProgress("\n"); //s_line);
                publishProgress("Obtaining list of public Collective Knowledge servers from " + url_cserver + " ...\n");
                curlCached = get_shared_computing_resource(url_cserver);
                AppConfigService.updateRemoteServerURL(curlCached);
            }
            return curlCached;
        }

        private void showIsThatCorrectDialog(final String recognitionResultText, final String imageFilePath, final String data_uid,
                                             final String behavior_uid, final String crowd_uid) {
            String[] predictions = recognitionResultText.split("[\\r\\n]+");

            if (predictions.length < 2) {
                publishProgress("\nError incorrect result text format ");
                return;
            }

            AppConfigService.updateRecognitionResultText(recognitionResultText);
            AppConfigService.updateActualImagePath(imageFilePath);
            AppConfigService.updateDataUID(data_uid);
            AppConfigService.updateBehaviorUID(behavior_uid);
            AppConfigService.updateCrowdUID(crowd_uid);
            AppConfigService.updatePreviewRecognitionText(null);

            AppConfigService.updateState(AppConfigService.AppConfig.State.RESULT);
            openResultActivity();
        }
    }

    private void openResultActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
                Intent resultIntent = new Intent(MainActivity.this, ResultActivity.class);
                startActivity(resultIntent);
            }
        });
    }

    // Recognize image ********************************************************************************
    private void predictImage(String imgPath) {
        // TBD - for now added to true next, while should be preloading ...
        updateImageView(imgPath);
        updateControlStatusPreloading(false);
        AppConfigService.updateState(AppConfigService.AppConfig.State.IN_PROGRESS);
        new RunRecognitionAsync().execute("");
    }

    class ImageInfo {
        private int height;
        private int width;
        private String path;

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private void updateImageView(String imagePath) {
        if (imagePath != null) {
            if (updateImageViewFromFile(imagePath)) {
                return;
            }
        }
        RecognitionScenario selectedRecognitionScenario = RecognitionScenarioService.getSelectedRecognitionScenario();
        if (selectedRecognitionScenario != null && selectedRecognitionScenario.getDefaultImagePath() != null) {
            String defaultImagePath = selectedRecognitionScenario.getDefaultImagePath();
            if (updateImageViewFromFile(defaultImagePath)) {
                AppConfigService.updateActualImagePath(defaultImagePath);
            } else {
                AppConfigService.updateActualImagePath(null);
            }
        }
    }

    private boolean updateImageViewFromFile(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            try {
                Bitmap bmp = Utils.decodeSampledBitmapFromResource(imagePath, imageView.getMaxWidth(), imageView.getMaxHeight());
                imageView.setVisibility(View.VISIBLE);
                imageView.setEnabled(true);
                imageView.setImageBitmap(bmp);
                bmp = null;
                return true;
            } catch (Exception e) {
                AppLogger.logMessage("Error on drawing image " + e.getLocalizedMessage());
            }
        } else {
            AppLogger.logMessage("Warning image file does not exist " + imagePath);
        }
        return false;
    }

    private ImageInfo getImageInfo(String imagePath) {
        if (imagePath != null) {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(imagePath);
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.setPath(imagePath);
                imageInfo.setHeight(bmp.getHeight());
                imageInfo.setWidth(bmp.getWidth());
                return imageInfo;
            }
        }
        return null;

    }
    /**
     * Rotate an image if required.
     *
     * @param selectedImagePath Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static int getImageDegree(String selectedImagePath) {

        ExifInterface ei = null;
        try {
            ei = new ExifInterface(selectedImagePath);
        } catch (IOException e) {
            AppLogger.logMessage("Error image could not be rotated " + e.getLocalizedMessage());
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = AppConfigService.getActualImagePath();
                if (imgPath == null) {
                    AppLogger.logMessage("Error problem with captured image file");
                    return;
                }
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }
            rotateImageAccoridingToOrientation(imgPath);
            AppConfigService.updateActualImagePath(imgPath);
            updateImageView(imgPath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    interface ProgressPublisher {
        void setPercent(int percent);
        void addBytes(long bytes);
        void println(String text);
    }
}
