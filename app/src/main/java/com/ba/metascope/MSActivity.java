package com.ba.metascope;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ai.fritz.core.Fritz;

import static com.ba.metascope.ActivityManager.PredictionMode.*;

public class MSActivity extends AppCompatActivity {

    PredictorFritz predictor;
    ActivityManager manager;
    OverlayManager overlay;

    float touchX, touchY;
    String focusedObjectclass = "NONE";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fritz.configure(this, "7b3b8108f99443dc9d8c59109a6b909d");
        CameraView camera = findViewById(R.id.cameraView);
        camera.setLifecycleOwner(this);

        predictor = new PredictorFritz();
        manager = new ActivityManager();
        overlay = new OverlayManager( loadJSONFromAsset(),findViewById(R.id.fritzView), findViewById(R.id.itemName));

        overlay.displayText("MetaScope");

        camera.addFrameProcessor(
                frame -> StartPredictionProcess(frame)
        );

        //set Touch Listener
        camera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                touchX = event.getX();
                touchY = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!manager.isActiveMode()) {
                            manager.setActive(SCAN_AT_POSITION_THEN_TRACK_FROM_CLASS);
                            overlay.displayText("Scanning for Objects..");
                        }else {
                            clearOverlay();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }

                return false;
            }
        });

        //Track all object in camera view
        Button button = findViewById(R.id.btnScan);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manager.setActive(TRACK_ALL);
                overlay.displayText("Scanning for Objects..");
            }
        });
    }

    /**
     * Starts the prediction if active.
     * Converts the Frame to a Bitmap and calls 'runPrediction'
     * @param frame - Camera Frame from live feed
     */
    private void StartPredictionProcess(Frame frame) {
        if(!manager.isActiveMode() || manager.isPredicting()) {
            return;
        }
        manager.setPredicting(true);
        final byte[] data = frame.getData();
        final int width = frame.getSize().getWidth();
        final int height = frame.getSize().getHeight();
        Runnable func = () -> {
            final Bitmap image = convertToBitmap(data, width, height);

            runPrediction(image);
        };
        new Thread(func).start();
    }

    /**
     * Runs prediction on a given Bitmap.
     * Calls the 'modeSelector'
     * @param image - Bitmap of the image
     */
    private void runPrediction(final Bitmap image) {
        Runnable func = () -> {
            predictor.predictFromBitmap(image);

            modeSelector();
        };
        new Thread(func).start();
    }

    /**
     * Invokes app-function based on the ActivityManager
     */
    private void modeSelector() {
        switch (manager.getMode()) {
            case SCAN_ALL:
                displayAllObjects();
                break;
            case TRACK_ALL:
                trackAllObjects();
                break;
            case SCAN_AND_TRACK_AT_POSITION:
                trackObjectAtTouchPosition();
                break;
            case TRACK_ALL_FROM_CLASS:
                trackObjectWithClass(focusedObjectclass);
                break;
            case SCAN_AT_POSITION_THEN_TRACK_FROM_CLASS:
                setObjectToTrackAtTouchPosition();
                break;
            default:
                clearOverlay();
                break;
        }
    }

    //--------------------------Functions of the tracking behaviour---------------------------------

    /**
     * Clears the Overlay
     */
    private void clearOverlay() {
        Runnable func = () -> {
            overlay.drawBitmap( predictor.drawObjectsFoundWithClass("NONE"));
            overlay.displayText("MetaScope");
            focusedObjectclass = "NONE";

            manager.setInactive();
        };
        new Thread(func).start();
    }

    /**
     * Marks all objects found.
     * Does not track them.
     */
    private void displayAllObjects() {
        Runnable func = () -> {
            overlay.drawBitmap( predictor.drawAllObjectsFound());
            overlay.displayText("Done");

            manager.setInactive();
        };
        new Thread(func).start();
    }

    /**
     * Marks and tracks all objects found
     */
    private void trackAllObjects() {
        Runnable func = () -> {
            overlay.drawBitmap(predictor.drawAllObjectsFound());
            overlay.displayText("Tracking Object...");

            manager.setPredicting(false);
        };
        new Thread(func).start();
    }

    /**
     * Marks object at a given touch location.
     * Performs a continuous update.
     */
    private void trackObjectAtTouchPosition() {
        Runnable func = () -> {
            overlay.drawBitmap(predictor.drawObjectsFoundWithClass(predictor.getObjectclassAtPosition(touchX, touchY)));
            overlay.displayText("Tracking Object...");

            manager.setPredicting(false);
        };
        new Thread(func).start();
    }

    /**
     * Saves the object class found at the touch position.
     * Starts activly tracking the class found.
     */
    private void setObjectToTrackAtTouchPosition() {
        focusedObjectclass = predictor.getObjectclassAtPosition(touchX, touchY);
        if (overlay.displayTextFromDatabase(focusedObjectclass)) {
            manager.setActive(TRACK_ALL_FROM_CLASS);
            manager.setPredicting(false);
        } else {
            manager.setInactive();
        }
    }

    /**
     * Tracks all objects with a certain class
     * @param objectclass - object class to look for
     */
    private void trackObjectWithClass(String objectclass) {
        Runnable func = () -> {
            overlay.drawBitmap(predictor.drawObjectsFoundWithClass(objectclass));

            manager.setPredicting(false);
        };
        new Thread(func).start();
    }

    /**
     * Converts byte-data into a bitmap for further processing
     * @param data - byte-data
     * @param width - dimension
     * @param height - dimension
     * @return - orientated image
     */
    private Bitmap convertToBitmap(final byte[] data, final int width, final int height) {
                //Intensive computation to create a Bitmap
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, out);
                byte[] imageBytes = out.toByteArray();
                Bitmap temp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                //Rotation based on device orientation
                Matrix matrix = new Matrix();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    matrix.postRotate(0);
                } else {
                    matrix.postRotate(90);
                }

                return Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
    }

    /**
     * Loads the Database.json file as a JSON-String
     * @return JSON-String
     */
    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("Database.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
