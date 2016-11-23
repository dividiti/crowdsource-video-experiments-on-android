package openscience.crowdsource.video.experiments;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class CaptureActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private int currentCameraSide = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean isCameraStarted = false;

    private Button switchCamera;
    private Button btnCapture;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        switchCamera = (Button) findViewById(R.id.btn_rotate);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.release();
                stopCameraPreview();
                if (currentCameraSide == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    currentCameraSide = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    currentCameraSide = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                startCameraPreview();
            }
        });

        btnCapture = (Button) findViewById(R.id.btn_Capture);
        btnCapture.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                captureImageFromCameraPreviewAndReturn();
            }
        });

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                stopCameraPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                startCameraPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stopCameraPreview();
            }
        });
    }

    private void startCameraPreview() {
        if (!isCameraStarted) {
            try {

                surfaceView.setVisibility(View.VISIBLE);
                surfaceView.setEnabled(true);

                camera = Camera.open(currentCameraSide);
                camera.setPreviewDisplay(surfaceHolder);
                if (CaptureActivity.this.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
                    camera.setDisplayOrientation(90);
                } else {
                    camera.setDisplayOrientation(180);
                }
                if (currentCameraSide != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Camera.Parameters cameraParams = camera.getParameters();
                    cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    camera.setParameters(cameraParams);
                }
                camera.startPreview();
                isCameraStarted = true;
            } catch (Exception e) {
                AppLogger.logMessage("Error starting camera preview " + e.getLocalizedMessage() + " \n");
                e.printStackTrace();
                isCameraStarted = false;
                return;
            }
        }
        switchCamera.setEnabled(true);
        btnCapture.setEnabled(true);
    }


    private void stopCameraPreview() {
        if (camera != null) {
            camera.release();
        }
        camera = null;
        isCameraStarted = false;
        switchCamera.setEnabled(false);
        btnCapture.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
    }

    /**
     * @return absolute path to image
     */
    private void captureImageFromCameraPreviewAndReturn() {
        synchronized (camera) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        FileOutputStream fos = new FileOutputStream(AppConfigService.getActualImagePath());
                        fos.write(data);
                        fos.close();
                        stopCameraPreview();

                        Bitmap bmp = BitmapFactory.decodeFile(AppConfigService.getActualImagePath());
                        Matrix rotationMatrix = new Matrix();

                        if (CaptureActivity.this.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
                            if (currentCameraSide == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                rotationMatrix.postRotate(-90);
                            } else {
                                rotationMatrix.postRotate(90);
                            }
                        } else {
                            if (currentCameraSide == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                rotationMatrix.postRotate(-180);
                            } else {
                                rotationMatrix.postRotate(180);
                            }
                        }

                        Bitmap rbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), rotationMatrix, true);

                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(AppConfigService.getActualImagePath());
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


                        Intent aboutIntent = new Intent(CaptureActivity.this, MainActivity.class);
                        startActivity(aboutIntent);

                    } catch (OutOfMemoryError e) {
                        AppLogger.logMessage("Error on image capture " + e.getLocalizedMessage());
                    } catch (FileNotFoundException e) {
                        AppLogger.logMessage("Error on image capture " + e.getLocalizedMessage());
                    } catch (IOException e) {
                        AppLogger.logMessage("Error on image capture " + e.getLocalizedMessage());
                    }
                }
            });
        }
    }
}
