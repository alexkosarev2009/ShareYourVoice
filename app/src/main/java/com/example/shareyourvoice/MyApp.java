package com.example.shareyourvoice;

import android.app.Application;

import com.example.shareyourvoice.domain.Place;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseUser;


public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("Npn6Vcwu01GSXuNaRiJd0j2e3xXwgbtVW7WoFAex")
                .clientKey("viJ21da23t77TX11Ciyg9ohMLstajw0BwRdtYJjv")
                .server("https://parseapi.back4app.com/")
                .build()
        );
        ParseUser.enableAutomaticUser();

    }
}
