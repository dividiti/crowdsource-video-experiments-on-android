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
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends android.app.Activity implements GLSurfaceView.Renderer {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;


    String welcome = "This application let you participate in experiment crowdsourcing " +
            "to collaboratively solve complex problems! " +
            "Please, press 'Update' button to obtain shared scenarios such as " +
            "collaborative benchmarking, optimization and tuning of a popular Caffe CNN image recognition library!\n" +
            "NOTE: you should have an unlimited Internet since some scenario may require to download 300Mb+ code and datasets! " +
            "Also some anonymized statistics will be collected about your platform and code execution " +
            "(performance, accuracy, power consumption, cost, etc) at cknowledge.org/repo " +
            "to let the community improve algorithms for diverse hardware!\n\n";

    String problem = "maybe be overloaded or down! Please report this problem to Grigori.Fursin@cTuning.org!";

    String path_opencl = "/system/vendor/lib/libOpenCL.so";

    String s_line = "====================================\n";

    String url_sdk = "http://github.com/ctuning/ck";
    String url_about = "https://github.com/ctuning/ck/wiki/Advanced_usage_crowdsourcing";
    String url_stats = "http://cknowledge.org/repo/web.php?action=index&module_uoa=wfe&native_action=show&native_module_uoa=program.optimization&scenario=experiment.bench.dnn.mobile";
    String url_users = "http://cTuning.org/crowdtuning-timeline";

    String url_cserver = "http://cTuning.org/shared-computing-resources-json/ck.json";
    String repo_uoa = "upload";

    String BUTTON_NAME_UPDATE = "Update";

    String s_thanks = "Thank you for participation!\n";

//    static String email = "";

//    Button buttonUpdateExit = null;

    private Button btnOpenImage;

    private GLSurfaceView glSurfaceView;

//    String cemail = "email.txt";
    String path1 = "ck-crowd";

    static String externalSDCardPath = "";
    static String externalSDCardOpensciencePath = "";
    static String externalSDCardOpenscienceTmpPath = "";

//    static String pemail = "";

    private AsyncTask crowdTask = null;
    Boolean running = false;

    static String pf_gpu = "";
    static String pf_gpu_vendor = "";

    static String path = ""; // Path to local tmp files
    static String path0 = "";

    static Button b_clean;
//    TextView t_email;


    String chmod744 = "/system/bin/chmod 744";

    private GoogleApiClient client;

    PFInfo pfInfo;
    String curlCached;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client2;

    Camera camera;
    boolean isCameraStarted = false;
//    SurfaceView surfaceView;
//    SurfaceHolder surfaceHolder;

    Button startStopCam;

    Button recognize;

    private Boolean isPreloadRunning = false;
    private Boolean isPreloadMode = true;
    private Boolean isUpdateMode = false;
    private Spinner scenarioSpinner;
    private ArrayAdapter<RecognitionScenario> spinnerAdapter;
    private List<RecognitionScenario> recognitionScenarios = new LinkedList<>();
    private JSONObject scenariosJSON = null;
    private String cachedScenariosFilePath;
    private String cachedPlatformFeaturesFilePath;

    private JSONObject platformFeatures = null;

    int currentCameraSide = Camera.CameraInfo.CAMERA_FACING_BACK;
    private ImageView imageView;

//    private String actualImageFilePath;
//    private Uri takenPictureFilUri;

    EditText consoleEditText;

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

//                        actualImageFilePath = takenPictureFilPath;
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
//        if (currentCameraSide == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            rotationMatrix.postRotate(90);
//        } else {
//            rotationMatrix.postRotate(90);
//        }
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


//    private void startCameraPreview() {
//        if (!isCameraStarted) {
//            try {
//                imageView.setVisibility(View.INVISIBLE);
//                imageView.setEnabled(false);
//
//                surfaceView.setVisibility(View.VISIBLE);
//                surfaceView.setEnabled(true);
//
//                camera = Camera.open(currentCameraSide);
//                camera.setPreviewDisplay(surfaceHolder);
//                camera.setDisplayOrientation(90);
//                if (currentCameraSide != Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                    Camera.Parameters cameraParams = camera.getParameters();
//                    cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//                    camera.setParameters(cameraParams);
//                }
//                camera.startPreview();
//            } catch (Exception e) {
//                AppLogger.logMessage("Error starting camera preview " + e.getLocalizedMessage() + " \n");
//                e.printStackTrace();
//                return;
//            }
//            isCameraStarted = true;
//        }
//        return;
//    }

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
        if (scenarioSpinner.getSelectedItem() == null) {
            return null;
        }
        for (RecognitionScenario recognitionScenario : recognitionScenarios) {
            if (recognitionScenario.getTitle().equalsIgnoreCase(scenarioSpinner.getSelectedItem().toString())) {
                return recognitionScenario;
            }
        }
        return null;
    }

    /*************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTaskBarColored(this);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
//        startStopCam.setOnClickListener(new Button.OnClickListener() {
//            public void onClick(View arg0) {
//                startStopCam.setEnabled(false);
//                if (!isCameraStarted) {
//                    startCameraPreview();
//                } else {
//
//                    captureImageFromCameraPreviewAndPredict(false);
//                }
//                startStopCam.setEnabled(true);
//            }
//        });

        startStopCam.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                createDirIfNotExist(externalSDCardOpenscienceTmpPath);
                String takenPictureFilPath = String.format(externalSDCardOpenscienceTmpPath + File.separator + "%d.jpg", System.currentTimeMillis());
                File file = new File(takenPictureFilPath);
                Uri takenPictureFilUri = Uri.fromFile(file);
                AppConfigService.updateActualImagePath(takenPictureFilPath);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, takenPictureFilUri);
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        });



        recognize = (Button) findViewById(R.id.suggest);
        recognize.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                RecognitionScenario recognitionScenario = getSelectedRecognitionScenario();
                if (recognitionScenario == null) {
                    AppLogger.logMessage(" Scenarios was not selected! Please select recognitions scenario first! \n");
                    return;
                }

                if (isCameraStarted) {
                    captureImageFromCameraPreviewAndPredict(true);
                    return;
                }

                // Call prediction
                predictImage(AppConfigService.getActualImagePath()); //actualImageFilePath
            }
        });

//        surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
//        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format,
//                                       int width, int height) {
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//            }
//        });

//        buttonUpdateExit = (Button) findViewById(R.id.b_update_exit);
//        buttonUpdateExit.setText(BUTTON_NAME_UPDATE);

        scenarioSpinner = (Spinner) findViewById(R.id.s_scenario);
        spinnerAdapter = new SpinAdapter(this, R.layout.custom_spinner);
        scenarioSpinner.setAdapter(spinnerAdapter);
        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppConfigService.updateSelectedRecognitionScenario(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView1);

//        ImageButton switchCamera = (ImageButton) findViewById(R.id.btn_flip_cam);
//
//        switchCamera.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isCameraStarted) {
////NB: if you don't release the current camera before switching, you app will crash
//                    camera.release();
//
////swap the id of the camera to be used
//                    stopCameraPreview();
//                    if (currentCameraSide == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                        currentCameraSide = Camera.CameraInfo.CAMERA_FACING_FRONT;
//                    } else {
//                        currentCameraSide = Camera.CameraInfo.CAMERA_FACING_BACK;
//                    }
//                    startCameraPreview();
//                }
//            }
//        });

//        t_email = (TextView) findViewById(R.id.t_email);
//        t_email.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final EditText edittext = new EditText(MainActivity.this);
//                edittext.setText(email);
//                AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
//                clarifyDialogBuilder.setTitle("Please, enter email:")
//                        .setCancelable(false)
//                        .setPositiveButton("Update",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int id) {
//                                        dialog.cancel();
//                                        String newEmail = edittext.getText().toString();
//                                        updateEMail(newEmail);
//                                    }
//                                })
//                        .setNegativeButton("Cancel",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int id) {
//                                        dialog.cancel();
//                                    }
//                                });
//                final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
//
//                clarifyDialog.setTitle("");
//                clarifyDialog.setMessage(Html.fromHtml("(OPTIONAL) Please enter your email if you would like to acknowledge your contributions (will be publicly visible):"));
//
//                SpannableString spanString = new SpannableString(email.trim());
//                spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
//
//                clarifyDialog.setView(edittext);
//                clarifyDialog.show();
//            }
//        });

        btnOpenImage = (Button) findViewById(R.id.btn_ImageOpen);
        btnOpenImage.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        addListenersOnButtons();

//        log = (EditText) findViewById(R.id.log);
        AppLogger.logMessage(welcome);

        this.glSurfaceView = new GLSurfaceView(this);
        this.glSurfaceView.setRenderer(this);
//        ((ViewGroup) log.getParent()).addView(this.glSurfaceView);

        // Prepare dirs (possibly pre-load from config
        externalSDCardPath = File.separator + "sdcard";
        externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;
        externalSDCardOpenscienceTmpPath = externalSDCardOpensciencePath + File.separator + "tmp" + File.separator;
        cachedScenariosFilePath = externalSDCardOpensciencePath + "scenariosFile.json";
        cachedPlatformFeaturesFilePath = externalSDCardOpensciencePath + "platformFeaturesFile.json";

//        deleteFiles(externalSDCardOpenscienceTmpPath);

//        pemail = externalSDCardOpensciencePath + cemail;

        // Getting local tmp path (for this app)
        File fpath = getFilesDir();
        path0 = fpath.toString();
        path = path0 + File.separator + path1;

        File fp = new File(path);
        if (!fp.exists()) {
            if (!fp.mkdirs()) {
                AppLogger.logMessage("\nERROR: can't create directory for local tmp files!\n");
                return;
            }
        }

        /* Read email config */
//        createDirIfNotExist(externalSDCardOpensciencePath);

//        loadCachedEmail();

        isUpdateMode = false;
        preloadScenarioses(false);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2 = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

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

//    private void loadCachedEmail() {
//        email = read_one_string_file(pemail);
//        if (email == null) email = "";
//        if (!email.equals("")) {
//            SpannableString spanString = new SpannableString(email);
//            spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
////            t_email.setText(spanString);
//        } else {
//            SpannableString spanString = new SpannableString(ACKNOWLEDGE_YOUR_CONTRIBUTIONS);
//            spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
////            t_email.setText(spanString);
//        }
//
//    }

//    private boolean updateEMail(String newEmailValue) {
//        String emailTrimmed = newEmailValue.trim();
//        if (emailTrimmed.equals("")) {
//            emailTrimmed = openme.gen_uid();
//        }
//        if (!emailTrimmed.equals(email)) {
//            email = emailTrimmed;
//            if (!save_one_string_file(pemail, email)) {
//                AppLogger.logMessage("ERROR: can't write local configuration (" + pemail + "!");
//                return true;
//            }
//            SpannableString spanString = new SpannableString(email.trim());
//            spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
//            t_email.setText(spanString);
//        }
//        return false;
//    }

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
            //status bar height
            int statusBarHeight = 100; // todo remove hardcoded resource

            View view = new View(context);
//            view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//            view.getLayoutParams().height = statusBarHeight;
//            ((ViewGroup) w.getDecorView()).addView(view);
            view.setBackgroundColor(context.getResources().getColor(R.color.colorStatusBar));
        }
    }

    /*************************************************************************/
    public void addListenersOnButtons() {
//        Button b_sdk = (Button) findViewById(R.id.b_sdk);
//        Button b_about = (Button) findViewById(R.id.b_about);
//        b_clean = (Button) findViewById(R.id.b_clean);
//        Button b_stats = (Button) findViewById(R.id.b_stats);
//        Button b_users = (Button) findViewById(R.id.b_users);

//        /*************************************************************************/
//        b_sdk.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                AppLogger.logMessage("\nOpening " + url_sdk + " ...\n");
//
//                Intent browserIntent =
//                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_sdk));
//
//                startActivity(browserIntent);
//            }
//        });

//        /*************************************************************************/
//        b_about.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                AppLogger.logMessage("\nOpening " + url_about + " ...\n");
//
//                Intent browserIntent =
//                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_about));
//
//                startActivity(browserIntent);
//            }
//        });

//        /*************************************************************************/
//        b_clean.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View arg0) {
//                log.setText("");
//                log.setText("Cleaning local tmp files ...\n");
//                if (!clean_log_tmp())
//                    log.setText("  ERROR: Can't create directory " + path + " ...\n");
//            }
//        });

//        /*************************************************************************/
//        b_stats.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                AppLogger.logMessage("\nOpening " + url_stats + " ...\n");
//
//                Intent browserIntent =
//                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_stats));
//
//                startActivity(browserIntent);
//            }
//        });

        /*************************************************************************/
//        b_users.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                AppLogger.logMessage("\nOpening " + url_users + " ...\n");
//
//                Intent browserIntent =
//                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_users));
//
//                startActivity(browserIntent);
//            }
//        });

        /*************************************************************************/
//        buttonUpdateExit.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                if (running) {
//                    running = false;
//
//                    buttonUpdateExit.setEnabled(false);
//
//                    AppLogger.logMessage("\n");
//                    AppLogger.logMessage(s_thanks);
//                    AppLogger.logMessage("Interrupting crowd-tuning and quitting program ...");
//
//                    Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        public void run() {
//                            finish();
//                            System.exit(0);
//                        }
//                    }, 1500);
//
//                } else {
//                    platformFeatures = null; // force reload features
//                    isUpdateMode = true;
//                    preloadScenarioses(true);
//                }
//            }
//        });
    }

    void preloadScenarioses(boolean forsePreload) {
        preloadPlatformFeature(forsePreload);
        File scenariosFile = new File(cachedScenariosFilePath);
        if (scenariosFile.exists() && !forsePreload) {
            try {
                JSONObject dict = openme.openme_load_json_file(cachedScenariosFilePath);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                scenariosJSON = dict.getJSONObject("dict");
                updateScenarioDropdown(scenariosJSON, new ProgressPublisher() {
                    @Override
                    public void publish(int percent) {
                    }

                    @Override
                    public void println(String text) {
                        AppLogger.logMessage(text + "\n");
                    }
                });

            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read preloaded file " + cachedScenariosFilePath);
                return;
            }
            scenarioSpinner.setSelection(AppConfigService.getSelectedRecognitionScenario());
        } else {
            isPreloadRunning = true;
            RecognitionScenario emptyRecognitionScenario = new RecognitionScenario();
            emptyRecognitionScenario.setTitle("Preloading...");
            emptyRecognitionScenario.setTotalFileSize("");
            emptyRecognitionScenario.setTotalFileSizeBytes(Long.valueOf(0));
            spinnerAdapter.add(emptyRecognitionScenario);
            isPreloadMode = true;
            spinnerAdapter.clear();
            spinnerAdapter.notifyDataSetChanged();
            AppConfigService.updateState(AppConfigService.AppConfig.State.PRELOAD);
            updateControlStatusPreloading(false);
            crowdTask = new RunCodeAsync().execute("");
        }
    }

    private void preloadPlatformFeature(boolean forsePreload) {
        if (!forsePreload) {
            File file = new File(cachedPlatformFeaturesFilePath);
            if (file.exists() && !forsePreload) {
                try {
                    JSONObject dict = openme.openme_load_json_file(cachedPlatformFeaturesFilePath);
                    // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                    platformFeatures = dict.getJSONObject("dict");
                } catch (JSONException e) {
                    AppLogger.logMessage("ERROR could not read preloaded file " + cachedPlatformFeaturesFilePath);
                    return;
                }
            }
        }
    }

    private void updateScenarioDropdown(JSONObject scenariosJSON, ProgressPublisher progressPublisher) {
        try {

            JSONArray scenarios = scenariosJSON.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                progressPublisher.println("Unfortunately, no scenarios found for your device ...");
                return;
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
                String sizeMB = "";
                Long sizeBytes = Long.valueOf(0);
                try {
                    String sizeB = scenario.getString("total_file_size");
                    sizeBytes = Long.valueOf(sizeB);
                    sizeMB = Utils.bytesIntoHumanReadable(sizeBytes);
                } catch (JSONException e) {
                    progressPublisher.println("Warn loading scenarios from file " + e.getLocalizedMessage());
                }

                final RecognitionScenario recognitionScenario = new RecognitionScenario();
                recognitionScenario.setModuleUOA(module_uoa);
                recognitionScenario.setDataUOA(data_uoa);
                recognitionScenario.setRawJSON(scenario);
                recognitionScenario.setTitle(title);
                recognitionScenario.setTotalFileSize(sizeMB);
                recognitionScenario.setTotalFileSizeBytes(sizeBytes);
                recognitionScenarios.add(recognitionScenario);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //stuff that updates ui
                        spinnerAdapter.add(recognitionScenario);
                        updateControlStatusPreloading(true);
                        spinnerAdapter.notifyDataSetChanged();
                    }
                });
            }
            spinnerAdapter.sort(SpinAdapter.COMPARATOR);
            scenarioSpinner.setSelection(AppConfigService.getSelectedRecognitionScenario());
            spinnerAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            progressPublisher.println("Error loading scenarios from file " + e.getLocalizedMessage());
        }
    }

    private void updateControlStatusPreloading(boolean isEnable) {
        scenarioSpinner.setEnabled(isEnable);
        startStopCam.setEnabled(isEnable);
        recognize.setEnabled(isEnable);

        if (!isEnable) {
            consoleEditText.setVisibility(View.VISIBLE);
            recognize.setVisibility(View.GONE);
            startStopCam.setVisibility(View.GONE);
        } else {
            consoleEditText.setVisibility(View.GONE);
            recognize.setVisibility(View.VISIBLE);
            startStopCam.setVisibility(View.VISIBLE);
        }

//        btnOpenImage.setEnabled(isEnable);
//        buttonUpdateExit.setEnabled(isEnable);
    }

    /*************************************************************************/
    private void alertbox(String title, String mymessage) {
        // TODO Auto-generated method stub
        new AlertDialog.Builder(this)
                .setMessage(mymessage)
                .setTitle(title)
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .show();
    }

    /*************************************************************************/
    /* delete files and directories recursively */
    private void rmdirs(File start) {
        if (start.isDirectory()) {
            for (File leaf : start.listFiles())
                rmdirs(leaf);
        }
        start.delete();
    }


    /*************************************************************************/
    /* exchange info with CK server */
    private JSONObject exchange_info_with_ck_server(JSONObject ii) {
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

    /*************************************************************************/
    boolean clean_log_tmp() {
        File fp = new File(path);
        rmdirs(fp);
        if (!fp.mkdirs()) {
            return false;
        }
        return true;
    }

    /*************************************************************************/
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
    private class RunCodeAsync extends AsyncTask<String, String, String> {

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

            /* For now just take default one, later add random or balancing */
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

        /*************************************************************************/
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

        /*************************************************************************/
        protected void onPostExecute(String x) {
//            buttonUpdateExit.setText(BUTTON_NAME_UPDATE);
//            b_clean.setEnabled(true);
            running = false;
            isPreloadRunning = false;
            isUpdateMode = false;
            AppConfigService.AppConfig.State state = AppConfigService.getState();
            if (state == null || state.equals(AppConfigService.AppConfig.State.IN_PROGRESS)) {
                AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
            }
            updateControlStatusPreloading(true);
        }

        /*************************************************************************/
        protected void onProgressUpdate(String... values) {
            if (values[0] != "") {
                AppLogger.logMessage(values[0]);
            } else if (values[1] != "") {
                alertbox(values[1], values[2]);
            }
        }

        /*************************************************************************/
        @Override
        protected String doInBackground(String... arg0) {
            String pf_system = "";
            String pf_system_vendor = "";
            String pf_system_model = "";
            String pf_cpu = "";
            String pf_cpu_subname = "";
            String pf_cpu_features = "";
            String pf_cpu_abi = "";
            String pf_cpu_num = "";
            String pf_gpu_opencl = "";
            String pf_gpu_openclx = "";
            String pf_memory = "";
            String pf_os = "";
            String pf_os_short = "";
            String pf_os_long = "";
            String pf_os_bits = "32";

            JSONObject r = null;
            JSONObject requestObject = null;
            JSONObject ft_cpu = null;
            JSONObject ft_os = null;
            JSONObject ft_gpu = null;
            JSONObject ft_plat = null;
            JSONObject ftuoa = null;

            publishProgress("\n"); //s_line);
            publishProgress(s_line);
            publishProgress("Local tmp directory: " + path + "\n");
            publishProgress("User ID: " + AppConfigService.getEmail() + "\n");

            if (isUpdateMode) {
                publishProgress("\n");
                publishProgress("Testing Collective Knowledge server ...\n");
                if (getCurl() == null) {
                    publishProgress("\n Error Collective Knowledge server is not reachible ...\n\n");
                    return null;
                }
                requestObject = new JSONObject();
                try {
                    requestObject.put("remote_server_url", getCurl());
                    requestObject.put("action", "test");
                    requestObject.put("module_uoa", "program.optimization");
                    requestObject.put("email", AppConfigService.getEmail());
                    requestObject.put("type", "mobile-crowdtuning");
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                try {
                    r = openme.remote_access(requestObject);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (validateReturnCode(r)) return null;

                String status = "";
                try {
                    status = (String) r.get("status");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'string' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("    " + status + "\n");
            }

            if (pfInfo != null) {

                pf_system = pfInfo.getPf_system();
                pf_system_vendor = pfInfo.getPf_system_vendor();
                pf_system_model = pfInfo.getPf_system_model();
                pf_cpu = pfInfo.getPf_cpu();
                pf_cpu_subname = pfInfo.getPf_cpu_subname();
                pf_cpu_features = pfInfo.getPf_cpu_features();
                pf_cpu_abi = pfInfo.getPf_cpu_abi();
                pf_cpu_num = pfInfo.getPf_cpu_num();
                pf_gpu_opencl = pfInfo.getPf_gpu_opencl();
                pf_gpu_openclx = pfInfo.getPf_gpu_openclx();
                pf_memory = pfInfo.getPf_memory();
                pf_os = pfInfo.getPf_os();
                pf_os_short = pfInfo.getPf_os_short();
                pf_os_long = pfInfo.getPf_os_long();
                pf_os_bits = pfInfo.getPf_os_bits();
            }


            DeviceInfo deviceInfo = new DeviceInfo();
            if (isUpdateMode || platformFeatures == null) {

                /*********** Getting local information about platform **************/
                publishProgress(s_line);
                publishProgress("Detecting some of your platform features ...\n");

                //Get system info **************************************************
                try {
                    r = openme.read_text_file_and_convert_to_json("/system/build.prop", "=", false, false);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    if ((Long) r.get("return") > 0)
                        publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                String model = "";
                String manu = "";

                JSONObject dict = null;

                try {
                    dict = (JSONObject) r.get("dict");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (dict != null) {
                    try {
                        model = (String) dict.get("ro.product.model");
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    try {
                        manu = (String) dict.get("ro.product.manufacturer");
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    if (!model.equals("") && !manu.equals(""))
                        if (model.toLowerCase().startsWith(manu.toLowerCase()))
                            model = model.substring(manu.length() + 1, model.length());

                    if (manu.equals("") && !model.equals("")) manu = model;

                    manu = manu.toUpperCase();
                    model = model.toUpperCase();

                    pf_system = manu;
                    if (!model.equals("")) pf_system += ' ' + model;
                    pf_system_model = model;
                    pf_system_vendor = manu;
                }

                //Get processor info **************************************************
                //It's not yet working properly on heterogeneous CPU, like big/little
                //So results can't be trusted and this part should be improved!
                try {
                    r = openme.read_text_file_and_convert_to_json("/proc/cpuinfo", ":", false, false);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    if ((Long) r.get("return") > 0)
                        publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                String processor_file = null;

                try {
                    processor_file = (String) r.get("file_as_string");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }
                if (processor_file == null) processor_file = "";

                try {
                    dict = (JSONObject) r.get("dict");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (dict != null) {
                    String processor = null;
                    String modelName = null;
                    String hardWare = null;
                    String features = null;

                    try {
                        processor = (String) dict.get("Processor");
                    } catch (JSONException e) {
                    }
                    try {
                        modelName = (String) dict.get("model_name");
                    } catch (JSONException e) {
                    }
                    try {
                        hardWare = (String) dict.get("Hardware");
                    } catch (JSONException e) {
                    }
                    try {
                        features = (String) dict.get("Features");
                    } catch (JSONException e) {
                    }

                    if (modelName != null && !modelName.equals("")) pf_cpu_subname = modelName;
                    else if (processor != null && !processor.equals("")) pf_cpu_subname = processor;

                    if (hardWare != null) pf_cpu = hardWare;
                    if (features != null) pf_cpu_features = features;

                    // On Intel-based Android
                    if (pf_cpu.equals("") && !pf_cpu_subname.equals(""))
                        pf_cpu = pf_cpu_subname;

                    // If CPU is empty, possibly new format
                    if (pf_cpu.equals("")) {
                        String cpuImplementer = "";
                        String cpuArchitecture = "";
                        String cpuVariant = "";
                        String cpuPart = "";
                        String cpuRevision = "";

                        try {
                            cpuImplementer = (String) dict.get("CPU implementer");
                        } catch (JSONException e) {
                        }

                        try {
                            cpuArchitecture = (String) dict.get("CPU architecture");
                        } catch (JSONException e) {
                        }

                        try {
                            cpuVariant = (String) dict.get("CPU variant");
                        } catch (JSONException e) {
                        }

                        try {
                            cpuPart = (String) dict.get("CPU part");
                        } catch (JSONException e) {
                        }

                        try {
                            cpuRevision = (String) dict.get("CPU revision");
                        } catch (JSONException e) {
                        }

                        pf_cpu += cpuImplementer + "-" + cpuArchitecture + "-" + cpuVariant + "-" + cpuPart + "-" + cpuRevision;
                    }

                    // If CPU is still empty, send report to CK to fix ...
                    if (pf_cpu.equals("")) {
                        publishProgress("\nPROBLEM: we could not detect CPU name and features on your device :( ! Please, report to authors!\n\n");
                        if (getCurl() == null) {
                            publishProgress("\n Error we could not report about CPU name and feature detection problem to Collective Knowledge server: it's not reachible ...\n\n");
                            return null;
                        }
                        requestObject = new JSONObject();
                        try {
                            requestObject.put("remote_server_url", getCurl());//
                            requestObject.put("action", "problem");
                            requestObject.put("module_uoa", "program.optimization");
                            requestObject.put("email", AppConfigService.getEmail());
                            requestObject.put("problem", "mobile_crowdtuning_cpu_name_empty");
                            requestObject.put("problem_data", processor_file);
                            requestObject.put("out", "json");
                        } catch (JSONException e) {
                            publishProgress("\nError with JSONObject ...\n\n");
                            return null;
                        }

                        try {
                            r = openme.remote_access(requestObject);
                        } catch (JSONException e) {
                            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                            return null;
                        }

                        if (validateReturnCode(r)) return null;

                        return null;
                    }
                }

                //Get memory info **************************************************
                try {
                    r = openme.read_text_file_and_convert_to_json("/proc/meminfo", ":", false, false);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    if ((Long) r.get("return") > 0 && (Long) r.get("return") != 16)
                        publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    dict = (JSONObject) r.get("dict");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (dict != null) {
                    String mem_tot = "";

                    try {
                        mem_tot = (String) dict.get("memtotal");
                    } catch (JSONException e) {
                    }

                    if (mem_tot == null || mem_tot.equals(""))
                        try {
                            mem_tot = (String) dict.get("MemTotal");
                        } catch (JSONException e) {
                        }

                    if (mem_tot != null && !mem_tot.equals("")) {
                        int i = mem_tot.indexOf(' ');
                        if (i > 0) {
                            String mem1 = mem_tot.substring(0, i).trim();
                            String mem2 = "1";
                            if (mem1.length() > 3) mem2 = mem1.substring(0, mem1.length() - 3);
                            pf_memory = mem2 + " MB";
                        }
                    }
                }

                //Get available processors and frequencies **************************************************
                List<Double[]> cpus = Utils.get_cpu_freqs();
                Double[] cpu = null;

                int cpu_num = cpus.size();
                pf_cpu_num = Integer.toString(cpu_num);

                pf_cpu_abi = Build.CPU_ABI; //System.getProperty("os.arch"); - not exactly the same!

                //Get OS info **************************************************
                pf_os = "Android " + Build.VERSION.RELEASE;

                try {
                    r = openme.read_text_file_and_convert_to_json("/proc/version", ":", false, false);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    if ((Long) r.get("return") > 0)
                        publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    pf_os_long = (String) r.get("file_as_string");
                } catch (JSONException e) {
                }
                if (pf_os_long == null) pf_os_long = "";
                else {
                    pf_os_long = pf_os_long.trim();

                    pf_os_short = pf_os_long;

                    int ix = pf_os_long.indexOf(" (");

                    if (ix >= 0) {
                        int ix1 = pf_os_long.indexOf("-");
                        if (ix1 >= 0 && ix1 < ix) ix = ix1;
                        pf_os_short = pf_os_long.substring(0, ix).trim();
                    }
                }

                int ix = pf_os_long.indexOf("_64");
                if (ix > 0) pf_os_bits = "64";

                //Get OpenCL info **************************************************
                File fopencl = new File(path_opencl);
                long lfopencl = fopencl.length();

                if (lfopencl > 0) {
                    pf_gpu_opencl = "libOpenCL.so - found (" + Long.toString(lfopencl) + " bytes)";
                    pf_gpu_openclx = "yes";
                } else {
                    pf_gpu_opencl = "libOpenCL.so - not found";
                    pf_gpu_openclx = "no";
                }

                //Print **************************************************
                publishProgress("\n");
                publishProgress("PLATFORM:   " + pf_system + "\n");
                publishProgress("* VENDOR:   " + pf_system_vendor + "\n");
                publishProgress("* MODEL:   " + pf_system_model + "\n");
                publishProgress("OS:   " + pf_os + "\n");
                publishProgress("* SHORT:   " + pf_os_short + "\n");
                publishProgress("* LONG:   " + pf_os_long + "\n");
                publishProgress("* BITS:   " + pf_os_bits + "\n");
                publishProgress("MEMORY:   " + pf_memory + "\n");
                publishProgress("CPU:   " + pf_cpu + "\n");
                publishProgress("* SUBNAME:   " + pf_cpu_subname + "\n");
                publishProgress("* ABI:   " + pf_cpu_abi + "\n");
                publishProgress("* FEATURES:   " + pf_cpu_features + "\n");
                publishProgress("* CORES:   " + pf_cpu_num + "\n");
                for (int i = 0; i < cpu_num; i++) {
                    String x = "    " + Integer.toString(i) + ") ";
                    cpu = cpus.get(i);
                    double x0 = cpu[0];
                    ;
                    double x1 = cpu[1];
                    double x2 = cpu[2];

                    if (x0 == 0) x += "offline";
                    else
                        x += "online; " + Double.toString(x2) + " of " + Double.toString(x1) + " MHz";

                    publishProgress(x + "\n");
                }
                publishProgress("GPU:   " + pf_gpu + "\n");
                publishProgress("* VENDOR:   " + pf_gpu_vendor + "\n");
                publishProgress("* OPENCL:   " + pf_gpu_opencl + "\n");

                //Delay program for 1 sec
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }

                //Communicate with CK **************************************************
                JSONObject j_os, j_cpu, j_gpu, j_sys;

                String j_os_uid = "";
                String j_cpu_uid = "";
                String j_gpu_uid = "";
                String j_sys_uid = "";

                publishProgress(s_line);
                publishProgress("Exchanging info about your platform with CK server to retrieve latest meta for crowdtuning ...");

                requestObject = new JSONObject();
                platformFeatures = new JSONObject();

                // OS ******
                publishProgress("\n    Exchanging OS info ...\n");

                try {
                    ft_os = new JSONObject();

                    ft_os.put("name", pf_os);
                    ft_os.put("name_long", pf_os_long);
                    ft_os.put("name_short", pf_os_short);
                    ft_os.put("bits", pf_os_bits);

                    platformFeatures.put("features", ft_os);

                    if (getCurl() == null) {
                        publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                        return null;
                    }
                    requestObject.put("remote_server_url", getCurl());//
                    requestObject.put("action", "exchange");
                    requestObject.put("module_uoa", "platform");
                    requestObject.put("sub_module_uoa", "platform.os");
                    requestObject.put("data_name", pf_os);
                    requestObject.put("repo_uoa", repo_uoa);
                    requestObject.put("all", "yes");
                    requestObject.put("dict", platformFeatures);
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                j_os = exchange_info_with_ck_server(requestObject);
                int rr = 0;
                try {
                    Object rx = j_os.get("return");
                    if (rx instanceof String) rr = Integer.parseInt((String) rx);
                    else rr = (Integer) rx;
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (rr > 0) {
                    String err = "";
                    try {
                        err = (String) j_os.get("error");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    publishProgress("\nProblem accessing CK server: " + err + "\n");
                    return null;
                } else {
                    String found = "";
                    try {
                        found = (String) j_os.get("found");
                    } catch (JSONException e) {
                    }

                    if (found.equals("yes")) {
                        try {
                            j_os_uid = (String) j_os.get("data_uid");
                        } catch (JSONException e) {
                        }

                        String x = "         Already exists";
                        if (!j_os_uid.equals("")) x += " (CK UOA=" + j_os_uid + ")";
                        x += "!\n";
                        publishProgress(x);
                    }
                }

                // GPU ******
                if (!pf_gpu.equals("") && getCurl() != null) {
                    publishProgress("    Exchanging GPU info ...\n");

                    try {
                        ft_gpu = new JSONObject();

                        ft_gpu.put("name", pf_gpu);
                        ft_gpu.put("vendor", pf_gpu_vendor);

                        platformFeatures.put("features", ft_gpu);

                        requestObject.put("remote_server_url", getCurl());//
                        requestObject.put("action", "exchange");
                        requestObject.put("module_uoa", "platform");
                        requestObject.put("sub_module_uoa", "platform.gpu");
                        requestObject.put("data_name", pf_gpu);
                        requestObject.put("repo_uoa", repo_uoa);
                        requestObject.put("all", "yes");
                        requestObject.put("dict", platformFeatures);
                        requestObject.put("out", "json");
                    } catch (JSONException e) {
                        publishProgress("\nError with JSONObject ...\n\n");
                        return null;
                    }

                    j_gpu = exchange_info_with_ck_server(requestObject);
                    try {
                        Object rx = j_gpu.get("return");
                        if (rx instanceof String) rr = Integer.parseInt((String) rx);
                        else rr = (Integer) rx;
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    if (rr > 0) {
                        String err = "";
                        try {
                            err = (String) j_gpu.get("error");
                        } catch (JSONException e) {
                            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                            return null;
                        }

                        publishProgress("\nProblem accessing CK server: " + err + "\n");
                        return null;
                    } else {
                        String found = "";
                        try {
                            found = (String) j_gpu.get("found");
                        } catch (JSONException e) {
                        }

                        if (found.equals("yes")) {
                            try {
                                j_gpu_uid = (String) j_gpu.get("data_uid");
                            } catch (JSONException e) {
                            }

                            String x = "         Already exists";
                            if (!j_gpu_uid.equals("")) x += " (CK UOA=" + j_gpu_uid + ")";
                            x += "!\n";
                            publishProgress(x);
                        }
                    }
                }

                // CPU ******
                publishProgress("    Exchanging CPU info ...\n");

                try {
                    ft_cpu = new JSONObject();

                    ft_cpu.put("name", pf_cpu);
                    ft_cpu.put("sub_name", pf_cpu_subname);
                    ft_cpu.put("cpu_features", pf_cpu_features);
                    ft_cpu.put("cpu_abi", pf_cpu_abi);
                    ft_cpu.put("num_proc", pf_cpu_num);

                    JSONObject freq_max = new JSONObject();
                    for (int i = 0; i < cpu_num; i++) {
                        String x = "    " + Integer.toString(i) + ") ";
                        cpu = cpus.get(i);
                        double x1 = cpu[1];
                        if (x1 != 0)
                            freq_max.put(Integer.toString(i), x1);
                    }
                    ft_cpu.put("max_freq", freq_max);

                    platformFeatures.put("features", ft_cpu);

                    if (getCurl() == null) {
                        publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                        return null;
                    }
                    requestObject.put("remote_server_url", getCurl());//
                    requestObject.put("action", "exchange");
                    requestObject.put("module_uoa", "platform");
                    requestObject.put("sub_module_uoa", "platform.cpu");
                    requestObject.put("data_name", pf_cpu);
                    requestObject.put("repo_uoa", repo_uoa);
                    requestObject.put("all", "yes");
                    requestObject.put("dict", platformFeatures);
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                j_cpu = exchange_info_with_ck_server(requestObject);
                try {
                    Object rx = j_cpu.get("return");
                    if (rx instanceof String) rr = Integer.parseInt((String) rx);
                    else rr = (Integer) rx;
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (rr > 0) {
                    String err = "";
                    try {
                        err = (String) j_cpu.get("error");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    publishProgress("\nProblem accessing CK server: " + err + "\n");
                    return null;
                } else {
                    String found = "";
                    try {
                        found = (String) j_cpu.get("found");
                    } catch (JSONException e) {
                    }

                    if (found.equals("yes")) {
                        try {
                            j_cpu_uid = (String) j_cpu.get("data_uid");
                        } catch (JSONException e) {
                        }

                        String x = "         Already exists";
                        if (!j_cpu_uid.equals("")) x += " (CK UOA=" + j_cpu_uid + ")";
                        x += "!\n";
                        publishProgress(x);
                    }
                }

                // Platform ******
                publishProgress("    Exchanging platform info ...\n");

                try {
                    ft_plat = new JSONObject();

                    ft_plat.put("name", pf_system);
                    ft_plat.put("vendor", pf_system_vendor);
                    ft_plat.put("model", pf_system_model);

                    platformFeatures.put("features", ft_plat);

                    if (getCurl() == null) {
                        publishProgress("\n Error we could not exchange platform info with Collective Knowledge server: it's not reachible ...\n\n");
                        return null;
                    }
                    requestObject.put("remote_server_url", getCurl());//
                    requestObject.put("action", "exchange");
                    requestObject.put("module_uoa", "platform");
                    requestObject.put("sub_module_uoa", "platform");
                    requestObject.put("data_name", pf_system);
                    requestObject.put("repo_uoa", repo_uoa);
                    requestObject.put("all", "yes");
                    requestObject.put("dict", platformFeatures);
                    requestObject.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                j_sys = exchange_info_with_ck_server(requestObject);
                try {
                    Object rx = j_sys.get("return");
                    if (rx instanceof String) rr = Integer.parseInt((String) rx);
                    else rr = (Integer) rx;
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (rr > 0) {
                    String err = "";
                    try {
                        err = (String) j_sys.get("error");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    publishProgress("\nProblem accessing CK server: " + err + "\n");
                    return null;
                } else {
                    String found = "";
                    try {
                        found = (String) j_sys.get("found");
                    } catch (JSONException e) {
                    }

                    if (found.equals("yes")) {
                        try {
                            j_sys_uid = (String) j_sys.get("data_uid");
                        } catch (JSONException e) {
                        }

                        String x = "         Already exists";
                        if (!j_sys_uid.equals("")) x += " (CK UOA=" + j_sys_uid + ")";
                        x += "!\n";
                        publishProgress(x);
                    }
                }

                //Delay program for 1 sec
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }

                deviceInfo.setJ_os_uid(j_os_uid);
                deviceInfo.setJ_cpu_uid(j_cpu_uid);
                deviceInfo.setJ_gpu_uid(j_gpu_uid);
                deviceInfo.setJ_sys_uid(j_sys_uid);


                pfInfo = new PFInfo();
                pfInfo.setPf_system(pf_system);
                pfInfo.setPf_system_vendor(pf_system_vendor);
                pfInfo.setPf_system_model(pf_system_model);
                pfInfo.setPf_cpu(pf_cpu);
                pfInfo.setPf_cpu_subname(pf_cpu_subname);
                pfInfo.setPf_cpu_features(pf_cpu_features);
                pfInfo.setPf_cpu_abi(pf_cpu_abi);
                pfInfo.setPf_cpu_num(pf_cpu_num);
                pfInfo.setPf_gpu_opencl(pf_gpu_opencl);
                pfInfo.setPf_gpu_openclx(pf_gpu_openclx);
                pfInfo.setPf_memory(pf_memory);
                pfInfo.setPf_os(pf_os);
                pfInfo.setPf_os_short(pf_os_short);
                pfInfo.setPf_os_long(pf_os_long);
                pfInfo.setPf_os_bits(pf_os_bits);

                try {
                    platformFeatures = getPlatformFeaturesJSONObject(pf_gpu_openclx, ft_cpu, ft_os, ft_gpu, ft_plat, deviceInfo);
                    openme.openme_store_json_file(platformFeatures, cachedPlatformFeaturesFilePath);
                } catch (JSONException e) {
                    publishProgress("\nError with platformFeatures ...\n\n");
                    return null;
                }
            }

            // Sending request to CK server to obtain available scenarios
             /*######################################################################################################*/
            publishProgress("\n    Sending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...\n\n");


            if (isUpdateMode || scenariosJSON == null) {
                JSONObject availableScenariosRequest = new JSONObject();


                try {
                    if (getCurl() == null) {
                        publishProgress("\n Error we could not load scenarios from Collective Knowledge server: it's not reachible ...\n\n");
                        return null;
                    }
                    availableScenariosRequest.put("remote_server_url", getCurl());
                    availableScenariosRequest.put("action", "get");
                    availableScenariosRequest.put("module_uoa", "experiment.scenario.mobile");
                    availableScenariosRequest.put("email", AppConfigService.getEmail());
                    availableScenariosRequest.put("platform_features", platformFeatures);
                    availableScenariosRequest.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                try {
                    r = openme.remote_access(availableScenariosRequest);
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (validateReturnCode(r)) return null;
                scenariosJSON = r;
                try {
                    openme.openme_store_json_file(scenariosJSON, cachedScenariosFilePath);
                } catch (JSONException e) {
                    publishProgress("\nError writing preloaded scenarios to file (" + e.getMessage() + ") ...\n\n");
                }
            }

            if (!isUpdateMode && scenariosJSON != null) {
                r = scenariosJSON;
            }

            try {
                JSONArray scenarios = r.getJSONArray("scenarios");
                if (scenarios.length() == 0) {
                    publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
                    return null;
                }

                String localAppPath = path + File.separator + "openscience" + File.separator;

                File externalSDCardFile = new File(externalSDCardOpensciencePath);
                if (!externalSDCardFile.exists()) {
                    if (!externalSDCardFile.mkdirs()) {
                        publishProgress("\nError creating dir (" + externalSDCardOpensciencePath + ") ...\n\n");
                        return null;
                    }
                }

                String libPath = null;
                String executablePath = null;
                String defaultImageFilePath = null;
                if (scenarios.length() == 0) {
                    publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
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

                    if (!isPreloadRunning &&
                            (getSelectedRecognitionScenario() == null || !getSelectedRecognitionScenario().getTitle().equalsIgnoreCase(title))) {
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
                        if (!isPreloadMode && downloadFileAndCheckMd5(
                                url,
                                targetFilePath,
                                md5,
                                new ProgressPublisher() {
                                    @Override
                                    public void publish(int percent) {
                                        String str="";

                                        if (percent<0) str+="\n * Downloading file " + targetFilePath + " ...\n";
                                        else  str+="  * "+percent+"%\n";

                                        publishProgress(str);
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
                                String fileAppDir = localAppPath + file.getString("path");
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
                            String library = null;
                            try {
                                executable = file.getString("executable");
                                library = file.getString("library");
                            } catch (JSONException e) {
                                // executable is not mandatory
                            }
                            if (executable != null && executable.equalsIgnoreCase("yes")) {
                                if (library != null && library.equalsIgnoreCase("yes")) {
                                    libPath = finalTargetFileDir;
                                } else {
                                    executablePath = finalTargetFileDir;
                                }
                                String[] chmodResult = openme.openme_run_program(chmod744 + " " + finalTargetFilePath, null, finalTargetFileDir);
                                if (chmodResult[0].isEmpty() && chmodResult[1].isEmpty() && chmodResult[2].isEmpty()) {
                                    publishProgress(" * File " + finalTargetFilePath + " sucessfully set as executable ...\n");
                                } else {
                                    publishProgress("\nError setting  file " + targetFilePath + " as executable ...\n\n");
                                    return null;
                                }
                            }
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
                    if (actualImageFilePath == null) {
                        actualImageFilePath = defaultImageFilePath;
                        AppConfigService.updateActualImagePath(actualImageFilePath);
                    }

                    if (isPreloadMode) {
                        final RecognitionScenario recognitionScenario = new RecognitionScenario();
                        recognitionScenario.setModuleUOA(module_uoa);
                        recognitionScenario.setDataUOA(data_uoa);
                        recognitionScenario.setRawJSON(scenario);
                        recognitionScenario.setDefaultImagePath(defaultImageFilePath);
                        recognitionScenario.setTitle(title);
                        recognitionScenario.setTotalFileSize(sizeMB);
                        recognitionScenario.setTotalFileSizeBytes(sizeBytes);
                        recognitionScenarios.add(recognitionScenario);

                        publishProgress("\nPreloaded scenario info:  " + recognitionScenario.toString() + "\n\n");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //stuff that updates ui
                                spinnerAdapter.add(recognitionScenario);
                                scenarioSpinner.setSelection(0);
                                spinnerAdapter.notifyDataSetChanged();
                            }
                        });
                        continue;
                    }

                    if (actualImageFilePath == null) {
                        publishProgress("\nError image file path was not initialized.\n\n");
                        return null;
                    }

                    if (libPath == null) {
                        publishProgress("\nError lib path was not initialized.\n\n");
                        return null;
                    }

                    String scenarioCmd = meta.getString("cmd");

                    String[] scenarioEnv = {
                            "CT_REPEAT_MAIN=" + String.valueOf(1),
                            "LD_LIBRARY_PATH=" + libPath + ":$LD_LIBRARY_PATH",
                    };

                    scenarioCmd = scenarioCmd.replace("$#local_path#$", externalSDCardPath + File.separator);
                    scenarioCmd = scenarioCmd.replace("$#image#$", actualImageFilePath);

                    final ImageInfo imageInfo = getImageInfo(actualImageFilePath);
                    if (imageInfo == null) {
                        publishProgress("\n Error: Image was not found...\n\n");
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

                    publishProgress("\nSelected scenario: " + title + "\n\n");

                    //In the future we may read json output and aggregate it too (openMe)
                    int iterationNum = 3; // todo it could be taken from loaded scenario
                    List<Long> processingTimes = new LinkedList<>();
                    List<List<Double[]>> cpuFreqs = new LinkedList<>();
                    String recognitionResultText = null;
                    for (int it = 0; it <= iterationNum; it ++) {
                        if (it == 0) {
                            publishProgress("Recognition in process (mobile warms up) ...\n");
                        } else {
                            publishProgress("Recognition in process (statistical repetition: " + it + " out of " + iterationNum + ") ...\n");
                        }
                        long startTime = System.currentTimeMillis();
                        String[] recognitionResult = openme.openme_run_program(scenarioCmd, scenarioEnv, executablePath); //todo fix ck response cmd value: add appropriate path to executable from according to path value at "file" json
                        Long processingTime = System.currentTimeMillis() - startTime;
                        recognitionResultText = recognitionResult[1]; // todo it better to compare recognition results and print error
                        if (recognitionResultText == null || recognitionResultText.trim().equals("")) {
                            publishProgress("\nError Recognition result is empty ...\n");
                            if (recognitionResult.length>=1 && recognitionResult[0] != null && !recognitionResult[0].trim().equals("")) {
                                publishProgress("\nRecognition errors: " + recognitionResult[0] + "\n\n");
                            }
                            if (recognitionResult.length>=3 && recognitionResult[2] != null && !recognitionResult[2].trim().equals("")) {
                                publishProgress("\nRecognition errors: " + recognitionResult[2] + "\n\n");
                            }
                            return null;
                        }
                        if (it == 0) {
                            //  first iteration used for mobile warms up if it was in a low freq state
                            publishProgress(" * Recognition time  (warming up) " + processingTime + " ms \n");
                            publishProgress("\nRecognition result (warming up):\n " + recognitionResultText + "\n\n");
                            continue;
                        }
                        publishProgress(" * Recognition time " + it + ": " + processingTime + " ms \n");
                        cpuFreqs.add(Utils.get_cpu_freqs());
                        processingTimes.add(processingTime);
                    }
                    publishProgress("\nRecognition result:\n\n" + recognitionResultText + "\n\n");

                    publishProgress("Submitting results and unexpected behavior (if any) to Collective Knowledge Aggregator ...\n");

                    if (getCurl() == null) {
                        publishProgress("\n Error we could not submit recognition results to Collective Knowledge server: it's not reachible ...\n\n");
                        return null;
                    }
                    JSONObject publishRequest = new JSONObject();
                    try {
                        JSONObject results = new JSONObject();
                        JSONArray processingTimesJSON = new JSONArray(processingTimes);
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

                        publishRequest.put("platform_features", platformFeatures);
                        publishRequest.put("raw_results", results);

                        publishRequest.put("cpu_freqs_before", getCPUFreqsJSON(cpuFreqs.get(0)));
                        publishRequest.put("cpu_freqs_after", getCPUFreqsJSON(cpuFreqs.get(cpuFreqs.size()-1)));
                    } catch (JSONException e) {
                        publishProgress("\nError with JSONObject ...\n\n");
                        return null;
                    }

                    JSONObject response;
                    try {
                        response = openme.remote_access(publishRequest);
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    int responseCode = 0;
                    if (!response.has("return")) {
                        publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                        return null;
                    }

                    try {
                        Object rx = response.get("return");
                        if (rx instanceof String) responseCode = Integer.parseInt((String) rx);
                        else responseCode = (Integer) rx;
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    if (responseCode > 0) {
                        String err = "";
                        try {
                            err = (String) response.get("error");
                        } catch (JSONException e) {
                            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                            return null;
                        }

                        publishProgress("\nProblem accessing CK server: " + err + "\n");
                        return null;
                    }

                    String status = "";
                    String data_uid = "";
                    String behavior_uid = "";

                    try {
                        status = (String) response.get("status");
                        data_uid = (String) response.get("data_uid");
                        behavior_uid = (String) response.get("behavior_uid");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'status' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    }

                    publishProgress('\n' + status + '\n');


                    showIsThatCorrectDialog(recognitionResultText, actualImageFilePath, data_uid, behavior_uid, dataUID);


                    //Delay program for 1 sec
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }

            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            //Delay program for 1 sec
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            if (isPreloadRunning) {
                publishProgress(s_line);
                publishProgress("Finished pre-loading shared scenarios for crowdsourcing!\n\n");
                publishProgress("Crowd engine is READY!\n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        spinnerAdapter.sort(SpinAdapter.COMPARATOR);
                        spinnerAdapter.notifyDataSetChanged();
                    }
                });
                AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
            } else {
                AppConfigService.updateState(AppConfigService.AppConfig.State.RESULT);
            }
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
                publishProgress("\nError incorrect result text format \n\n");
                return;
            }

            AppConfigService.updateRecognitionResultText(recognitionResultText);
            AppConfigService.updateActualImagePath(imageFilePath);
            AppConfigService.updateDataUID(data_uid);
            AppConfigService.updateBehaviorUID(behavior_uid);
            AppConfigService.updateCrowdUID(crowd_uid);

//            final String firstPrediction = predictions[1];
//            StringBuilder otherPredictionsBuilder = new StringBuilder();
//            for (int p = 2; p < predictions.length; p++) {
//                otherPredictionsBuilder.append(predictions[p]).append("<br>");
//            }
//            final String otherPredictions = otherPredictionsBuilder.toString();
            AppConfigService.updateState(AppConfigService.AppConfig.State.RESULT);
            openResultActivity();
        }

        public void sendCorrectAnswer(String recognitionResultText,
                                      String correctAnswer,
                                      String imageFilePath,
                                      String data_uid,
                                      String behavior_uid,
                                      String crowd_uid) {

            publishProgress("\nAdding correct answer to Collective Knowledge ...\n\n");

            JSONObject request = new JSONObject();
            try {
                request.put("raw_results", recognitionResultText);
                request.put("correct_answer", correctAnswer);
                String base64content = "";
                if (imageFilePath != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream); // todo we can reduce image size sending image in low quality
                    byte[] byteFormat = stream.toByteArray();
                    base64content = Base64.encodeToString(byteFormat, Base64.URL_SAFE); //todo fix hanging up on Galaxy Note4
                    request.put("file_base64", base64content);
                }
                request.put("data_uid", data_uid);
                request.put("behavior_uid", behavior_uid);
                request.put("remote_server_url", getCurl());
                request.put("action", "process_unexpected_behavior");
                request.put("module_uoa", "experiment.bench.dnn.mobile");
                request.put("crowd_uid", crowd_uid);

                new RemoteCallTask().execute(request);
            } catch (JSONException e) {
                publishProgress("\nError send correct answer to server (" + e.getMessage() + ") ...\n\n");
                return;
            }
        }

        private boolean validateReturnCode(JSONObject r) {
            int rr = 0;
            if (!r.has("return")) {
                publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                return true;
            }

            try {
                Object rx = r.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return true;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) r.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return true;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                publishProgress("\nPossible reason: " + problem + "\n");
                return true;
            }
            return false;
        }

        @NonNull
        private JSONObject getPlatformFeaturesJSONObject(String pf_gpu_openclx, JSONObject ft_cpu, JSONObject ft_os, JSONObject ft_gpu, JSONObject ft_plat, DeviceInfo deviceInfo) throws JSONException {
            JSONObject ft;
            ft = new JSONObject();

            ft.put("cpu", ft_cpu);
            ft.put("cpu_uid", deviceInfo.getJ_cpu_uid());
            ft.put("cpu_uoa", deviceInfo.getJ_cpu_uid());

            ft.put("gpu", ft_gpu);
            ft.put("gpu_uid", deviceInfo.getJ_gpu_uid());
            ft.put("gpu_uoa", deviceInfo.getJ_gpu_uid());

            // Need to tell CK server if OpenCL present
            // for collaborative OpenCL optimization using mobile devices
            JSONObject ft_gpu_misc = new JSONObject();
            ft_gpu_misc.put("opencl_lib_present", pf_gpu_openclx);
            ft.put("gpu_misc", ft_gpu_misc);

            ft.put("os", ft_os);
            ft.put("os_uid", deviceInfo.getJ_os_uid());
            ft.put("os_uoa", deviceInfo.getJ_os_uid());

            ft.put("platform", ft_plat);
            ft.put("platform_uid", deviceInfo.getJ_sys_uid());
            ft.put("platform_uoa", deviceInfo.getJ_sys_uid());
            return ft;
        }


    }

    private void openResultActivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
                Intent resultIntent = new Intent(MainActivity.this, ResultActivity.class);
                startActivity(resultIntent);



//                    final EditText edittext = new EditText(MainActivity.this);
//                    AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(MainActivity.this);
//                    clarifyDialogBuilder.setTitle("Please, enter correct answer:")
//                            .setCancelable(false)
//                            .setPositiveButton("Send",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            dialog.cancel();
//                                            String correctAnswer = edittext.getText().toString();
//                                            sendCorrectAnswer(recognitionResultText, correctAnswer, imageFilePath, data_uid, behavior_uid, crowd_uid);
//                                        }
//                                    })
//                            .setNegativeButton("Cancel",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            dialog.cancel();
//                                        }
//                                    });
//                    final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
//
//
//                    clarifyDialog.setMessage("");
//                    clarifyDialog.setTitle("Please, enter correct answer:");
//                    clarifyDialog.setView(edittext);
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                    builder.setTitle("Is that correct result:")
//                            .setMessage(Html.fromHtml("<font color='red'><b>" + firstPrediction + "</b></font><br>" + otherPredictions))
//                            .setCancelable(false)
//                            .setPositiveButton("Yes",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            dialog.cancel();
//                                        }
//                                    })
//                            .setNegativeButton("No",
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            dialog.cancel();
//                                            clarifyDialog.show();
//                                        }
//                                    });
//                    AlertDialog alert = builder.create();
//                    alert.show();
            }
        });
    }

    // Recognize image ********************************************************************************
    private void predictImage(String imgPath) {
        isPreloadMode = false;
        // TBD - for now added to true next, while should be preloading ...
        updateImageView(imgPath);
        updateControlStatusPreloading(false);
        AppConfigService.updateState(AppConfigService.AppConfig.State.IN_PROGRESS);
        crowdTask = new RunCodeAsync().execute("");
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
            try {
                Bitmap bmp = decodeSampledBitmapFromResource(imagePath, imageView.getMaxWidth(), imageView.getMaxHeight());
                imageView.setVisibility(View.VISIBLE);
                imageView.setEnabled(true);

//                surfaceView.setVisibility(View.INVISIBLE);
//                surfaceView.setEnabled(false);
                imageView.setImageBitmap(bmp);
                bmp = null;
            } catch (Exception e) {
                AppLogger.logMessage("Error on drawing image " + e.getLocalizedMessage());
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(String imagePath,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private ImageInfo getImageInfo(String imagePath) {
        if (imagePath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setPath(imagePath);
            imageInfo.setHeight(bmp.getHeight());
            imageInfo.setWidth(bmp.getWidth());
            return imageInfo;
        } else {
            return null;
        }
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
//                imgPath = takenPictureFilUri .getPath();
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
//            actualImageFilePath = imgPath;
            AppConfigService.updateActualImagePath(imgPath);
//            predictImage(imgPath);
            updateImageView(imgPath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean downloadFileAndCheckMd5(String urlString, String localPath, String md5, ProgressPublisher progressPublisher) {
        try {
            String existedlocalPathMD5 = fileToMD5(localPath);
            if (existedlocalPathMD5 != null && existedlocalPathMD5.equalsIgnoreCase(md5)) {
                return true;
            }

            int BUFFER_SIZE = 1024;
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

            progressPublisher.publish(-1); // Print only text

            while ((count = input.read(data)) != -1) {
                total += count;
                if (lenghtOfFile > 0) {
                    progressPercent = (int) ((total * 100) / lenghtOfFile);
                }
                if (progressPercent != prevProgressPercent) {
                    progressPublisher.publish(progressPercent);
                    prevProgressPercent = progressPercent;
                }
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();


            String gotMD5 = fileToMD5(localPath);
            if (!gotMD5.equalsIgnoreCase(md5)) {
                progressPublisher.println("ERROR: MD5 is not satisfied, please try again.");
                return false;
            } else {
                progressPublisher.println("File succesfully downloaded from " + urlString + " to local files " + localPath);
                return true;
            }
        } catch (FileNotFoundException e) {
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        } catch (Throwable e) {
            e.printStackTrace();
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        }
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
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
            return convertHashToString(md5Bytes);
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

    interface ProgressPublisher {
        void publish(int percent);

        void println(String text);
    }


    void createDirIfNotExist(String dirPath) {
        File externalSDCardFile = new File(dirPath);
        if (!externalSDCardFile.exists()) {
            if (!externalSDCardFile.mkdirs()) {
                AppLogger.logMessage("\nError creating dir (" + dirPath + ") ...\n\n");
                return;
            }
        }
    }


    //    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
////        // Checks the orientation of the screen
////        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
////            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
////        }
//    }

}
