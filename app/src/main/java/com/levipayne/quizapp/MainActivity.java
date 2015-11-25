package com.levipayne.quizapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private final String QUESTIONS_URL = "https://docs.google.com/document/u/0/export?format=txt&id=1MV7GHAvv4tgj98Hj6B_WZdeeEu7CRf1GwOfISjP4GT0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getJSONFromURL(QUESTIONS_URL);
    }

    public void goToQuiz(View view) {
        Intent intent = new Intent(this, QuizActivity.class);
        startActivity(intent);
    }

    public JSONObject getJSONFromURL(String url) {
        return null;
    }
}
