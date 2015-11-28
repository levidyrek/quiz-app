package com.levipayne.quizapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private TextView mDescription;

    private final String TAG = getClass().getSimpleName();
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mSignUpButton;
    private boolean loggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mDescription = (TextView) findViewById(R.id.description);

        mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn(mUsernameView.getText().toString(), mPasswordView.getText().toString());
            }
        });

        mSignUpButton = (Button) findViewById(R.id.register);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signUp(mUsernameView.getText().toString(), mPasswordView.getText().toString());
            }
        });

        mSignOutButton = (Button) findViewById(R.id.sign_out);
        mSignOutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        if (savedInstanceState == null)
            loggedIn = getIntent().getExtras().getBoolean("loggedIn");
        else
            loggedIn = savedInstanceState.getBoolean("loggedIn");
        if (loggedIn) {
            mDescription.setText(R.string.logout_description);
            mSignOutButton.setVisibility(View.VISIBLE);
            mUsernameView.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.GONE);
            mSignInButton.setVisibility(View.GONE);
            mSignUpButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("loggedIn", loggedIn);
    }

    public void signOut() {
        loggedIn = false;
        ((QuizApplication)getApplication()).getUser().logOutInBackground();
        ((QuizApplication)getApplication()).setUser(null);

        mSignOutButton.setVisibility(View.GONE);
        mUsernameView.setVisibility(View.VISIBLE);
        mPasswordView.setVisibility(View.VISIBLE);
        mSignInButton.setVisibility(View.VISIBLE);
        mSignUpButton.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
    }

    public void signUp(final String username, final String password) {
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(password);

        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    ParseUser user = ParseUser.getCurrentUser();
                    ((QuizApplication) getApplication()).setUser(user);
                    Toast.makeText(LoginActivity.this, "Registered!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Log.d(TAG, "Sign up failed: " + e.toString());
                    switch (e.getCode()) {
                        case ParseException.USERNAME_TAKEN:
                            Toast.makeText(LoginActivity.this, "Username already taken", Toast.LENGTH_LONG).show();
                            break;
                        case ParseException.ACCOUNT_ALREADY_LINKED:
                            Toast.makeText(LoginActivity.this, "Account already exists. Try signing in", Toast.LENGTH_LONG).show();
                            break;
                        case ParseException.PASSWORD_MISSING:
                            Toast.makeText(LoginActivity.this, "Must provide password", Toast.LENGTH_LONG).show();
                            break;
                        case ParseException.USERNAME_MISSING:
                            Toast.makeText(LoginActivity.this, "Must provide username", Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(LoginActivity.this, "Something went wrong. Please try again", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    public void signIn(final String username, final String password) {
        Log.d(TAG, "logging in...");
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    checkParseForScores(user);
                    Log.d(TAG, "logged in!");
                    Toast.makeText(LoginActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                    ParseUser u = ParseUser.getCurrentUser();
                    u.setACL(new ParseACL(user));
                    ((QuizApplication) getApplication()).setUser(user);
                    finish();
                } else {
                    Log.d(TAG, "Error logging in");
                    Toast.makeText(LoginActivity.this, "An error occured. Not logged in", Toast.LENGTH_LONG).show();
                }
            }
        });
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

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
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
}

