package com.levipayne.quizapp;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class QuizActivity extends AppCompatActivity {
    private ProgressBar mProgressBar;
    private TextView mTimeLeftView;
    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Instantiate View Objects
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mTimeLeftView = (TextView) findViewById(R.id.time_left);

        startCountdown();
    }

    public void startCountdown() {
        Log.d(TAG, "Countdown started");
        new CountDownTimer(60000, 100) {

            public void onTick(long millisUntilFinished) {
                mProgressBar.setProgress(6000 - ((int) (millisUntilFinished / 10)));
                String timeLeft = "00:" + String.format("%02d", millisUntilFinished/1000);
                mTimeLeftView.setText(timeLeft);
            }

            public void onFinish() {
                Toast.makeText(QuizActivity.this, "Time is up!", Toast.LENGTH_LONG).show();
                endQuiz();
            }
        }.start();
    }

    public void endQuiz() {
        Intent intent = new Intent(this, PostQuizActivity.class);
        startActivity(intent);
        finish();
    }
}
