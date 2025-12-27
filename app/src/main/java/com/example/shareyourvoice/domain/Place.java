package com.example.shareyourvoice.domain;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.HashMap;

public class Place {
    private String objectId;

    private String name;
    private final LatLng latLng;
    private String audioBase64;
    private String imageBase64;
    public Place(String  name, LatLng latLng) {
        this.name = name;
        this.latLng = latLng;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    // Загрузка из Cloud Function
    public static Place fromParseObject(ParseObject obj) {
        ParseGeoPoint geo = obj.getParseGeoPoint("latLng");

        LatLng latLng = null;
        if (geo != null) {
            latLng = new LatLng(geo.getLatitude(), geo.getLongitude());
        }

        Place place = new Place(
                obj.getString("name"),
                latLng

        );
        place.setObjectId(obj.getObjectId());
        return place;
    }

    // Сохранение через Cloud Function
    public void saveToParse(SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("latitude", latLng.latitude);
        params.put("longitude", latLng.longitude);

        if (audioBase64 != null && !audioBase64.isEmpty()) {
            params.put("audioBase64", audioBase64);
        }

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            params.put("imageBase64", imageBase64);
        }

        ParseCloud.callFunctionInBackground("savePlaceWithFiles", params,
                (object, e) -> {
                    if (e == null && object != null) {
                        HashMap<String, Object> result = (HashMap<String, Object>) object;

                        String objectId = (String) result.get("objectId");
                        this.objectId = objectId;
                        Log.d("SAVE", "Place saved via Cloud Code: " + objectId);
                        callback.done(null);
                    } else {
                        Log.e("SAVE", "Cloud Function error", e);
                        callback.done(e != null ? e : new ParseException(0, "Unknown error"));
                    }
                });
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setName(String name) {
        this.name = name;
    }
}
