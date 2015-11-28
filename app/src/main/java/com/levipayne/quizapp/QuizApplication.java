package com.levipayne.quizapp;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseUser;

/**
 * Created by levi on 11/27/15.
 */
public class QuizApplication extends Application {
    private ParseUser mUser;

    @Override
    public void onCreate() {
        super.onCreate();

        // Parse stuff
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "vrfg2fIXvX29e59YVdDU8PMUDpja1KUdHVkHHcaC", "wY7h6CximwyrDZ1uGGpSY2m0TBYu8O1cZ3maBX0o");
    }

    public ParseUser getUser() {
        return mUser;
    }

    public void setUser(ParseUser user) {
        this.mUser = user;
    }
}
