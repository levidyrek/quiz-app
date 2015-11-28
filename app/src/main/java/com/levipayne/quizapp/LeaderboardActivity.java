package com.levipayne.quizapp;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.zip.Inflater;

public class LeaderboardActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private ListView mListView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        getSupportActionBar().setTitle("Leaderboard");

        showProgressDialog();

        mListView = (ListView) findViewById(android.R.id.list);

        getTopScores();
    }

    public void getTopScores() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("QuizScore");
        query.addDescendingOrder("percentage");
        query.addAscendingOrder("seconds");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                List<ParseObject> topScores;
                if (objects.size() > 10) {
                    topScores = objects.subList(0, 10);
                }
                else {
                    topScores = objects;
                }

                ScoreListAdapter adapter = new ScoreListAdapter(LeaderboardActivity.this, 0, topScores);
                mListView.setAdapter(adapter);
                dismissProgressDialog();
            }
        });
    }

    // <------ Progress Dialog ------------->

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading...");
        mProgressDialog.show();
    }

    public void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }
}
