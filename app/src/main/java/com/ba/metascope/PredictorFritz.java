package com.ba.metascope;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictorOptions;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.objectdetection.ObjectDetectionOnDeviceModel;

public class PredictorFritz {
    private static final float PREDICTIONSPACE_DIMENSION = 300.0f;

    private FritzVisionObjectPredictor predictor;

    private FritzVisionImage capturedScreen;
    private FritzVisionImage overlayScreen;

    private FritzVisionObjectResult result;

    public PredictorFritz() {
        ObjectDetectionOnDeviceModel onDeviceModel = FritzVisionModels.getObjectDetectionOnDeviceModel();
        FritzVisionObjectPredictorOptions options = new FritzVisionObjectPredictorOptions();
        options.confidenceThreshold = 0.59f;  // A lowered confidance
        predictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel, options);
    }

    /**
     * Extension with custom confidance
     * @param confidance
     */
    public PredictorFritz(float confidance) {
        ObjectDetectionOnDeviceModel onDeviceModel = FritzVisionModels.getObjectDetectionOnDeviceModel();
        FritzVisionObjectPredictorOptions options = new FritzVisionObjectPredictorOptions();
        options.confidenceThreshold = confidance;  // A lowered confidance
        predictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel, options);
    }

    /**
     * Predict Objectclasses from Bitmap
     * @param image - Used Bitmap Image
     * @return The results of the prediction
     */
    public FritzVisionObjectResult predictFromBitmap(Bitmap image) {
        createScreens(image);
        result = predictor.predict(capturedScreen);
        return  result;
    }

    /**
     * Returns the object at a given location.
     * Needs a prior 'predictFromBitmap' call
     * @param x - position
     * @param y - position
     * @return The object found
     */
    public FritzVisionObject getObjectAtPosition(float x, float y) {
        Position targetPosition = convertToPredictionspace(x, y);
        FritzVisionObject target = null;
        for (FritzVisionObject obj : result.getObjects()) {
            if (obj.getBoundingBox().contains(targetPosition.x, targetPosition.y)) {
                target = obj;
            }
        }
        return target;
    }

    /**
     * Returns the object class at a given location
     * Needs a prior 'predictFromBitmap' call
     * @param x - position
     * @param y - position
     * @return The object class found
     */
    public String getObjectclassAtPosition(float x, float y) {
        FritzVisionObject obj = getObjectAtPosition(x, y);
        return obj != null ? obj.getVisionLabel().getText() : "NONE";
    }

    /**
     * Draws all objects found onto a Bitmap
     * @return the Bitmap with full transparency and rectangles for objects
     */
    public Bitmap drawAllObjectsFound() {
        return overlayScreen.overlayBoundingBoxes(result.getObjects());
    }

    /**
     * Draws all objects found from a specific class onto a Bitmap
     * @param objectclass - String with the class to be drawn onto the Overlay
     * @return the Bitmap with full transparency and rectangles for objects
     */
    public Bitmap drawObjectsFoundWithClass(String objectclass) {
        return overlayScreen.overlayBoundingBoxes(result.getVisionObjectsByClass(objectclass));
    }

    //-------------------------------Private Methods------------------------------------------------

    /**
     * Creates screens for detection
     * @param image - image to run the prediction on
     */
    private void createScreens(final Bitmap image) {
        final Bitmap screen =  Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight());
        capturedScreen = FritzVisionImage.fromBitmap(screen);

        //create transparent Layer for Overlay
        int iWidth = image.getWidth();
        int iHeight = image.getHeight();
        Bitmap transBmp = Bitmap.createBitmap(iWidth,
                iHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(transBmp);
        final Paint paint = new Paint();
        paint.setAlpha(0);
        canvas.drawBitmap(image, 0, 0, paint);
        final Bitmap transparentImage =  Bitmap.createBitmap(transBmp, 0, 0, transBmp.getWidth(), transBmp.getHeight());

        overlayScreen = FritzVisionImage.fromBitmap(transparentImage);
    }

    /**
     * Converts a coordinate from screen to prediction space
     * @param x - position
     * @param y - position
     * @return Position instance in the prediction space
     */
    private Position convertToPredictionspace(float x, float y) {
        if (capturedScreen == null) {
            Log.e("PREDICTION", "No Screen givem");
            return new Position(x,y);
        }

        float wRatio = PREDICTIONSPACE_DIMENSION / capturedScreen.getWidth();
        float hRatio = PREDICTIONSPACE_DIMENSION / capturedScreen.getHeight();
        float xPos = x * wRatio;
        float yPos = y * hRatio;

        return new Position(xPos, yPos);
    }

    /**
     * Postion Struct
     */
    private class Position {
        float x;
        float y;

        public Position(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
