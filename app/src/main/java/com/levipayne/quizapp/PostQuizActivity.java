package com.levipayne.quizapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class PostQuizActivity extends Activity {
    private final String TAG = getClass().getSimpleName();

    private TextView mResponseTextView;
    private TextView mCorrectTextView;
    private TextView mIncorrectTextView;
    private TextView mPercentageTextView;
    private TextView mTimeTakenTextView;

    private int mNumCorrect;
    private int mNumIncorrect;
    private int mNumTotal;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mSharedEditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_quiz);

        // View element instantiation
        mResponseTextView = (TextView) findViewById(R.id.response_text);
        mCorrectTextView = (TextView) findViewById(R.id.num_correct);
        mIncorrectTextView = (TextView) findViewById(R.id.num_incorrect);
        mPercentageTextView = (TextView) findViewById(R.id.percentage);
        mTimeTakenTextView = (TextView) findViewById(R.id.time_text);

        // Variable instantiation
        mNumCorrect = 0;
        mNumIncorrect = 0;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedEditor = mSharedPrefs.edit();

        checkAnswers(getIntent().getExtras().getStringArray("answers"), getIntent().getExtras().getStringArray("responses"));
        Log.d(TAG, "Correct: " + mNumCorrect + " out of " + mNumTotal);

        // Update UI based on quiz results
        int percentCorrect = (int)(((double)mNumCorrect/(double)mNumTotal) * 100);
        mPercentageTextView.setText(percentCorrect + "%");
        if (percentCorrect == 100)
            mResponseTextView.setText(R.string.perfect_job_text);
        else if (percentCorrect < 50)
            mResponseTextView.setText(R.string.bad_job_text);

        mCorrectTextView.setText("You got " + mNumCorrect + " correct");
        mIncorrectTextView.setText("You got " + mNumIncorrect + " incorrect");

        int timeTaken = (int)(getIntent().getExtras().getLong("time"))/1000;
        mTimeTakenTextView.setText("in " + timeTaken + " seconds");

        boolean isHighScore = saveScoreIfBest(percentCorrect, timeTaken);
        if (!isHighScore) {
            findViewById(R.id.high_score).setVisibility(View.INVISIBLE);
        }
    }

    public void checkAnswers(String[] answers, String[] responses) {
        mNumTotal = answers.length;
        for (int i = 0; i < responses.length; i++) {
            if (responses[i].equalsIgnoreCase(answers[i]))
                mNumCorrect++;
            else
                mNumIncorrect++;
        }
    }

    public void tryQuizAgain(View view) {
        Intent intent = new Intent(this, QuizActivity.class);
        startActivity(intent);
        finish();
    }

    public boolean saveScoreIfBest(int percentage, int seconds) {
        int prevPercentage = mSharedPrefs.getInt("percentage", -1);
        int prevSeconds = mSharedPrefs.getInt("seconds", -1);

        if ((prevPercentage == -1 || prevSeconds == -1) || (percentage > prevPercentage) || (percentage == prevPercentage && seconds < prevSeconds)) { // New high score
            mSharedEditor.putInt("percentage", percentage);
            mSharedEditor.putInt("seconds", seconds);
            mSharedEditor.commit();
            return true;
        }
        return false;
    }
}
