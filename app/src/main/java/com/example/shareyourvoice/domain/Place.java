package com.example.shareyourvoice.domain;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.Objects;

public class Place {
    private final String objectId;

    private final String name;
    private final ParseFile imageFile;
    private final ParseFile audioFile;
    private final String address;
    private final LatLng latLng;

    public Place(String objectId, String  name, String address, LatLng latLng, ParseFile imageFile, ParseFile audioFile) {
        this.objectId = objectId;
        this.name = name;
        this.address = address;
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
        return new Place(
                obj.getObjectId(),
                obj.getString("name"),
                obj.getString("address"),
                latLng,
                obj.getParseFile("image"),
                obj.getParseFile("audio")
        );
    }


    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }


    public String getAddress() {
        return address;
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
}
