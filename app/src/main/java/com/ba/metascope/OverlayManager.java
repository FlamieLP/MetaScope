package com.ba.metascope;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class OverlayManager {
    ImageView overlayView;
    TextView itemLabel;
    String jsonString;

    /**
     * constructor for the overlay
     * @param jsonDatabaseString - JSON-String used for object-metadata matching
     * @param overlay - ImageView of the overlay
     * @param label - TextView to display the metadata onto
     */
    public OverlayManager(String jsonDatabaseString, ImageView overlay, TextView label) {
        this.overlayView = overlay;
        this.itemLabel = label;
        this.jsonString = jsonDatabaseString;
    }

    /**
     * Updates the overlay
     * @param overlay - New Bitmap to display
     */
    public void drawBitmap(final Bitmap overlay) {
        overlayView.post(() -> overlayView.setImageBitmap(overlay.copy(overlay.getConfig(), true)));
    }

    /**
     * Displays the given Text
     * @param text - New Text to display
     */
    public void displayText(final String text) {
        itemLabel.post(() -> {
            itemLabel.setText(text);
        });
    }

    /**
     * Displays metadata from JSON-String.
     * Returns false if the object class given is 'NONE'
     * @param objClass - object class to look for.
     * @return false if object class is 'NONE'
     */
    public boolean displayTextFromDatabase(String objClass) {
        this.displayText(this.grabFromJSONDatabase(objClass));
        return objClass != "NONE";
    }

    /**
     * Searches JSON-String for given object class
     * @param objClass - object class to look for
     * @return defaults on 'NONE' otherwise return JSON entry
     */
    private String grabFromJSONDatabase(String objClass) {
        if (objClass == "NONE") {
            return "No Object found!";
        }
        String information = String.format("%1s : \n We currently do not provide additional Information", objClass);

        try {
            // get JSONObject from JSON file
            JSONObject database = new JSONObject(jsonString);
            JSONObject object = database.getJSONObject(objClass);
            if (object != null) {
                information = String.format("%1s : \n %2s", objClass, object.getString("text"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return information;
    }
}
