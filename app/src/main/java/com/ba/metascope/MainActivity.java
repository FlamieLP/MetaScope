package com.ba.metascope;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.otaliastudios.cameraview.*;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;

import java.io.ByteArrayOutputStream;
import java.util.List;

import ai.fritz.core.*;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.*;


/**
 * Deprecated Test class
 */
public class MainActivity extends AppCompatActivity {


    private FritzVisionObjectPredictor predictor;

    FritzVisionObjectResult result;
    FritzVisionImage visionImage;
    FritzVisionImage blankImage;
    Boolean hasTaskRequest = false;
    Boolean processFinished = true;
    Boolean hasObject = false;

    ImageView fritzView;
    TextView itemLabel;

    float touchX,touchY;
    String selectedClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fritz.configure(this, "7b3b8108f99443dc9d8c59109a6b909d");
        CameraView camera = findViewById(R.id.cameraView);
        camera.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        camera.mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.EXPOSURE_CORRECTION);
        camera.setLifecycleOwner(this);


        itemLabel = findViewById(R.id.itemName);
        fritzView = findViewById(R.id.fritzView);
        itemLabel.setText(R.string.app_name);

        ObjectDetectionOnDeviceModel onDeviceModel = FritzVisionModels.getObjectDetectionOnDeviceModel();
        /*Start Options*/
        FritzVisionObjectPredictorOptions options = new FritzVisionObjectPredictorOptions();
        options.confidenceThreshold = 0.59f;  // A low threshold
        /*End Options*/
        predictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel, options);

        camera.addFrameProcessor(new FrameProcessor() {
                                     @Override
                                     @WorkerThread
                                     public void process(@NonNull Frame frame) {
                                         extractDataFromFrameFritz(frame);
                                     }
                                 }
        );

        camera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                touchX = event.getX();
                touchY = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        /*itemLabel.post(new Runnable() {
                            @Override
                            public void run() {
                                itemLabel.setText(infoProvider(getObjectTouched(touchX, touchY)));
                            }
                        });*/
                        if (!hasTaskRequest) {
                            hasObject = false;
                            itemLabel.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!hasObject)
                                    itemLabel.setText("Initializing...");
                                }
                            });
                        }else {
                            itemLabel.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!hasObject)
                                        itemLabel.setText("Stopped Tracking: " + selectedClass);
                                }
                            });
                            selectedClass = "QUIT";
                        }
                        hasTaskRequest = !hasTaskRequest;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }

                return false;
            }
        });

        Button button = findViewById(R.id.btnScan);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (processFinished) {
                    processFinished = false;
                    hasTaskRequest = true;
                    itemLabel.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!hasObject)
                            itemLabel.setText("Initializing...");
                        }
                    });
                }
            }
        });
    }

    public String getObjectTouched(float x, float y) {
        if (blankImage != null) {
            if(!hasObject) {
                float wRatio = 300f / blankImage.getWidth();
                float hRatio = 300f / blankImage.getHeight();
                float xPos = x * wRatio;
                float yPos = y * hRatio;

                Log.i("TOUCHTAG", "Processing Touch");
                FritzVisionObject target = null;
                for (FritzVisionObject obj : result.getObjects()) {
                    if (obj.getBoundingBox().contains(xPos, yPos)) {
                        target = obj;
                    }
                }

                if (target != null) {
                    hasObject = true;
                    selectedClass = target.getVisionLabel().getText();
                    final Bitmap boundingImage = blankImage.overlayBoundingBox(target);
                    fritzView.post(new Runnable() {
                        @Override
                        public void run() {
                            fritzView.setImageBitmap(boundingImage.copy(boundingImage.getConfig(), true));
                        }
                    });
                    return target.getVisionLabel().getText();
                } else {
                    hasTaskRequest = false;
                    return "No Object clicked";
                }
            } else {
                final Bitmap boundingImage = blankImage.overlayBoundingBoxes(result.getVisionObjectsByClass(selectedClass));
                fritzView.post(new Runnable() {
                    @Override
                    public void run() {
                        fritzView.setImageBitmap(boundingImage.copy(boundingImage.getConfig(), true));
                    }
                });
                return selectedClass;
            }
        }

        return "No Object detected";
    }

    private void extractDataFromFrameFritz(Frame frame) {
        if(hasTaskRequest && processFinished) {
            processFinished = false;
            convertToBitmap((byte[])frame.getData(),frame.getSize().getWidth(), frame.getSize().getHeight());
        }
    }

    public void convertToBitmap(final byte[] data, final int width, final int height) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                itemLabel.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!hasObject)
                        itemLabel.setText("Processing Screen");
                    }
                });
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, out);
                byte[] imageBytes = out.toByteArray();
                Bitmap temp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                Matrix matrix = new Matrix();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    matrix.postRotate(0);
                } else {
                    matrix.postRotate(90);
                }

                final Bitmap image =  Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);

                int iWidth = temp.getWidth();
                int iHeight = temp.getHeight();
                Bitmap transBmp = Bitmap.createBitmap(iWidth,
                        iHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(transBmp);
                final Paint paint = new Paint();
                paint.setAlpha(0);
                canvas.drawBitmap(temp, 0, 0, paint);
                final Bitmap emptyImage =  Bitmap.createBitmap(transBmp, 0, 0, transBmp.getWidth(), transBmp.getHeight(), matrix, true);

                visionImage = FritzVisionImage.fromBitmap(image);
                blankImage = FritzVisionImage.fromBitmap(emptyImage);

                /*fritzView.post(new Runnable() {
                    @Override
                    public void run() {
                        fritzView.setImageBitmap(image.copy(image.getConfig(), true));
                    }
                });*/

                startPrediction();
            }
        };
        new Thread(runnable).start();
    }

    public void startPrediction() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                itemLabel.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!hasObject)
                        itemLabel.setText("Searching for Objects...");
                    }
                });
                result = predictor.predict(visionImage);
                displayResult();
            }
        };
        new Thread(runnable).start();
    }

    public void displayResult() {
        Runnable runnable = () -> {

                /*final Bitmap boundingImage = blankImage.overlayBoundingBoxes(result.getObjects());
                fritzView.post(new Runnable() {
                    @Override
                    public void run() {
                        fritzView.setImageBitmap(boundingImage.copy(boundingImage.getConfig(), true));
                    }
                });*/

                /*String temp = "";
                for (FritzVisionObject obj : result.getObjects()) {
                    temp += obj.getVisionLabel().getText() + ": " + obj.getVisionLabel().getConfidence() + ", ";
                }
                final String label = temp;
                Log.i("PREDICTION", label);*/
                itemLabel.post(new Runnable() {
                    @Override
                    public void run() {
                        //itemLabel.setText("Found -> " + label);
                        itemLabel.setText(infoProvider(getObjectTouched(touchX, touchY)));
                    }
                });
                processFinished = true;
        };
        new Thread(runnable).start();
    }


    private String infoProvider(String label) {
        switch(label) {
            case "keyboard":
                return
                    String.format("%1s : \n A standard keyboard was used throughout the 21st century", label);
            case "person":
                return
                    String.format("%1s : \n Not an exhibit, but still nice to look at :)", label);
            case "QUIT":
                return "Stopped Tracking";
            case "No Object clicked":
                return String.format("%1s : \n Please try again", label);
            default:
                return
                    String.format("%1s : \n We currently do not provide additional Information", label);
        }
    }
}
