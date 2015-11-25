package com.levipayne.quizapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.google.gson.Gson;
import com.levipayne.quizapp.QuestionObjects.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class QuizActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    // Views
    private ProgressBar mProgressBar;
    private ProgressDialog mProgressDialog;
    private ViewAnimator mViewAnimator;

    private TextView mTimeLeftView;
    private final String QUESTIONS_URL = "https://docs.google.com/document/u/0/export?format=txt&id=1MV7GHAvv4tgj98Hj6B_WZdeeEu7CRf1GwOfISjP4GT0";
    private String mJsonText;
    private boolean mStartWhenDoneLoading;
    private ArrayList<Question> questions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Instantiate View Objects
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mTimeLeftView = (TextView) findViewById(R.id.time_left);
        mViewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);

        if (mJsonText == null) {
            try {
                showProgressDialog();
                mJsonText = new URLtoJSONTask().execute(QUESTIONS_URL).get();
                questions = getQuestionsFromJSON(mJsonText);
                for (Question question : questions)
                        Log.d(TAG, "Question: " + question.toString());
                setUpQuestionViews();
                dismissProgressDialog();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

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

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading...");
        mProgressDialog.show();
    }

    public void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }

    private void setUpQuestionViews() {
        
        for (Question question : questions) {

        }
    }

    public ArrayList<Question> getQuestionsFromJSON(String jsonText) throws JSONException {
        ArrayList<Question> questions = new ArrayList<>();
        JSONObject json = new JSONObject(jsonText);
        JSONArray jsonQuestions = json.getJSONArray("questions");
        Gson gson = new Gson();
        for (int i = 0; i < jsonQuestions.length(); i++) {
            Question question = gson.fromJson(jsonQuestions.get(i).toString(), Question.class);
            questions.add(question);
        }
        return questions;
    }

    private class URLtoJSONTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            try {
                String result;
                URL url = new URL(urls[0]);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(url.openStream()));

                String inputLine;
                StringBuilder sb = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    sb.append(inputLine + '\n');
                in.close();
                return sb.toString();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(String jsonText) {

        }
    }
}
