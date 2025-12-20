package com.example.shareyourvoice.domain;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.Objects;

public class Place {
    private String objectId;

    private String name;
    private ParseFile imageFile;
    private ParseFile audioFile;
    private final LatLng latLng;

    public Place(String  name, LatLng latLng, ParseFile imageFile, ParseFile audioFile) {
        this.name = name;
        this.latLng = latLng;
        this.imageFile = imageFile;
        this.audioFile = audioFile;
    }

    public static Place fromParseObject(ParseObject obj) {
        ParseGeoPoint geo = obj.getParseGeoPoint("latLng");

        LatLng latLng = null;
        if (geo != null) {
            latLng = new LatLng(geo.getLatitude(), geo.getLongitude());
        }

        Place place = new Place(
                obj.getString("name"),
                latLng,
                obj.getParseFile("image"),
                obj.getParseFile("audio")
        );
        place.setObjectId(obj.getObjectId());
        return place;
    }

    public void saveToParse(SaveCallback callback) {
        ParseObject obj = new ParseObject("Place");

        if (objectId != null) {
            obj.setObjectId(objectId);
        }

        obj.put("name", name);

        if (latLng != null) {
            obj.put("latLng", new ParseGeoPoint(latLng.latitude, latLng.longitude));
        }

        if (imageFile != null) {
            obj.put("image", imageFile);
        }

        if (audioFile != null) {
            obj.put("audio", audioFile);
        }

        obj.saveInBackground(e -> {
            if (e == null) {
                this.objectId = obj.getObjectId();
            }
            if (callback != null) {
                callback.done(e);
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

    public ParseFile getImageFile() {
        return imageFile;
    }

    public ParseFile getAudioFile() {
        return audioFile;
    }
    public void setAudioFile(ParseFile file) {
        this.audioFile = file;
    }
    public void setImageFile(ParseFile file) {
        this.imageFile = file;
    }

    public void setName(String name) {
        this.name = name;
    }
}
