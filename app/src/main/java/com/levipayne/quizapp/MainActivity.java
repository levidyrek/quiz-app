package com.levipayne.quizapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private boolean mLoggedIn;
    private ParseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkForExistingUser();

        if (mLoggedIn) {
            checkParseForScores(mUser);
        }
    }

    public void checkParseForScores(ParseUser user) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("QuizScore");
        query.whereEqualTo("username", user.getUsername());
        query.addDescendingOrder("percentage");
        query.addAscendingOrder("seconds");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (objects.size() > 1) {
                    ParseObject score = objects.get(0);
                    int percentage = score.getInt("percentage");
                    int seconds = score.getInt("seconds");

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    int storedPercentage = sharedPrefs.getInt("percentage", -1);
                    int storedSeconds = sharedPrefs.getInt("seconds", -1);
                    if ((storedPercentage < percentage) || (storedPercentage == percentage && storedSeconds > seconds)) {
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putInt("percentage", percentage);
                        editor.putInt("seconds", seconds);
                        editor.commit();
                    }
                }
            }
        });
    }

    public void goToQuiz(View view) {
        if (isNetworkAvailable()) {
            Intent intent = new Intent(this, QuizActivity.class);
            startActivity(intent);
        }
    }

    public void goToLogin(View v) {
        if (isNetworkAvailable()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("loggedIn", mLoggedIn);
            startActivity(intent);
        }
    }

    public void goToLeaderboard(View v) {
        if (isNetworkAvailable()) {
            Intent intent = new Intent(this, LeaderboardActivity.class);
            startActivity(intent);
        }
    }

    public void checkForExistingUser() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            mLoggedIn = true;
            Log.d("Parse", "Existing user found");
            ((QuizApplication) getApplication()).setUser(currentUser);
            mUser = currentUser;
        } else {
            mLoggedIn = false;
            Log.d("Parse", "No existing user found");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (!available) {
            Toast.makeText(this, "No internet connection. Please connect to use this feature.", Toast.LENGTH_LONG).show();
        }
        return available;
    }
}
