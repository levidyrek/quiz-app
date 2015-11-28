package com.levipayne.quizapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.google.gson.Gson;
import com.levipayne.quizapp.CustomObjects.CustomRadioGroup;
import com.levipayne.quizapp.CustomObjects.LetterPicker;
import com.levipayne.quizapp.QuestionObjects.Answer;
import com.levipayne.quizapp.QuestionObjects.Question;
import com.parse.ParseUser;

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

public class QuizActivity extends Activity implements GestureDetector.OnGestureListener {
    private final String TAG = getClass().getSimpleName();
    // Views
    private RelativeLayout mRootView;
    private ProgressBar mProgressBar;
    private ProgressDialog mProgressDialog;
    private ViewAnimator mViewAnimator;
    private RadioButton mRadioA;
    private RadioButton mRadioB;
    private RadioButton mRadioC;
    private RadioButton mRadioD;
    private Button mFinishButton;

    private TextView mTimeLeftView;
    private final String QUESTIONS_URL = "https://docs.google.com/document/u/0/export?format=txt&id=1MV7GHAvv4tgj98Hj6B_WZdeeEu7CRf1GwOfISjP4GT0";
    private String mJsonText;
    private boolean mStartWhenDoneLoading;
    private ArrayList<Question> mQuestions;
    private long mTimeElapsed;
    private CountDownTimer mCountDownTimer;
    private LinearLayout mQuestionContainer;
    private Button mNextButton;
    private Button mPrevButton;
    private boolean mCountDownPaused;
    private String[] mResponses;
    private int[] mSelectedAnswerIds;
    private String[] mAnswers;
    private int mOrientation;
    private GestureDetectorCompat mDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        ParseUser user = ((QuizApplication)getApplication()).getUser();
        boolean startCountdown = true;
        if (user != null && user.isNew() && savedInstanceState == null) {
            startCountdown = false;
            Dialog dialog = new Dialog(this);
            dialog.setTitle("Before you start...");
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.starting_dialog, null);
            dialog.setContentView(layout);
            dialog.setCancelable(true);
            dialog.show();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    startCountdown(0);
                }
            });
        }

        // Instantiate View Objects
        mRootView = (RelativeLayout) findViewById(R.id.main_container);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mTimeLeftView = (TextView) findViewById(R.id.time_left);
        mQuestionContainer = (LinearLayout) findViewById(R.id.question_container);
        mViewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
        mNextButton = (Button) findViewById(R.id.next_button);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mFinishButton = (Button) findViewById(R.id.finish_button);

//        setRadioListeners(); // This is necessary to support the alternate landscape layout
        setNavButtonListeners();

        mOrientation = getResources().getConfiguration().orientation;

        if (savedInstanceState == null) { // First time onCreate has been executed
            try {
                // Load questions from url and convert them into Question objects
                showProgressDialog();
                mJsonText = new URLtoJSONTask().execute(QUESTIONS_URL).get();
                mQuestions = getQuestionsFromJSON(mJsonText);

                // Prepare to track answers
                mResponses = new String[mQuestions.size()];
                mSelectedAnswerIds = new int[mQuestions.size()];
                for (int i = 0; i < mResponses.length; i++) { // Set default values
                    mResponses[i] = "";
                    mSelectedAnswerIds[i] = -1;
                }

                setUpQuestionViews(null, mOrientation); // Set up ViewAnimator with question views
                mPrevButton.setVisibility(View.INVISIBLE);
                dismissProgressDialog();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mTimeElapsed = 0;
        }
        else { // Restore data from savedInstanceState
            // Prepare to track answers
            mResponses = savedInstanceState.getStringArray("mResponses");
            mSelectedAnswerIds = savedInstanceState.getIntArray("mSelectedAnswerIds");
            Log.d(TAG, "Selected answers: {" + mSelectedAnswerIds[0] + "," + mSelectedAnswerIds[1] + "}");

            mJsonText = savedInstanceState.getString("mJsonText");
            mTimeElapsed = savedInstanceState.getLong("mTimeElapsed");
            try {
                mQuestions = getQuestionsFromJSON(mJsonText);
                setUpQuestionViews(mSelectedAnswerIds, mOrientation);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Set animator to correct question and correct visibility for prev/next buttons
            int currentQuestion = savedInstanceState.getInt("currentQuestion");
            mViewAnimator.setDisplayedChild(currentQuestion);
            if (currentQuestion == 0)
                mPrevButton.setVisibility(View.INVISIBLE);
            else if (currentQuestion == mViewAnimator.getChildCount()-1) {
                mNextButton.setVisibility(View.INVISIBLE);
                mFinishButton.setVisibility(View.VISIBLE);
            }
//            checkForPreviousSelection();
        }

        mAnswers = new String[mQuestions.size()];
        for (int i = 0; i < mAnswers.length; i++) {
            mAnswers[i] = mQuestions.get(i).getAnswer();
        }
        mCountDownPaused = false;

        if (startCountdown)
            startCountdown(mTimeElapsed);

        mDetector = new GestureDetectorCompat(this,this);
    }

    private void setNavButtonListeners() {
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextQuestion();
            }
        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrevQuestion();
            }
        });

        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endQuiz();
            }
        });
    }

    public void showNextQuestion() {
        // Set animation
        mViewAnimator.setInAnimation(this, R.anim.slide_in_right);
        mViewAnimator.setOutAnimation(this, R.anim.slide_out_left);

        mViewAnimator.showNext();
        if (mViewAnimator.getDisplayedChild() == mViewAnimator.getChildCount()-1) {
            mNextButton.setVisibility(View.INVISIBLE);
            mFinishButton.setVisibility(View.VISIBLE);
        }
        else if (mViewAnimator.getDisplayedChild() > 0)
            mPrevButton.setVisibility(View.VISIBLE);

    }

    public void showPrevQuestion() {
        // Set animation
        mViewAnimator.setInAnimation(this, R.anim.slide_in_left);
        mViewAnimator.setOutAnimation(this, R.anim.slide_out_right);

        mViewAnimator.showPrevious();
        if (mViewAnimator.getDisplayedChild() == 0)
            mPrevButton.setVisibility(View.INVISIBLE);
        else if (mViewAnimator.getDisplayedChild() < mViewAnimator.getChildCount()-1) {
            mNextButton.setVisibility(View.VISIBLE);
            mFinishButton.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mJsonText", mJsonText);
        outState.putLong("mTimeElapsed", mTimeElapsed);
        outState.putInt("currentQuestion", mViewAnimator.getDisplayedChild());
        outState.putStringArray("mResponses", mResponses);
        outState.putIntArray("mSelectedAnswerIds", mSelectedAnswerIds);

        mCountDownTimer.cancel();
    }

    public void startCountdown(final long start) {
        Log.d(TAG, "Countdown started");
        mProgressBar.setProgress((int) start);
        mCountDownTimer = new CountDownTimer(60000 - start, 100) {

            public void onTick(long millisUntilFinished) {
                mProgressBar.setProgress((int)(6000 - (millisUntilFinished / 10)));
                String timeLeft = "00:" + String.format("%02d", millisUntilFinished/1000);
                mTimeLeftView.setText(timeLeft);
                mTimeElapsed = 60000 - millisUntilFinished;
            }

            public void onFinish() {
                Toast.makeText(QuizActivity.this, "Time is up!", Toast.LENGTH_LONG).show();
                endQuiz();
            }
        }.start();
    }

    public void endQuiz() {
        Log.d(TAG, mResponses[0] + "," + mResponses[1] + "," + mResponses[2] + "," + mResponses[3] + "," + mResponses[4] + "," + mResponses[5]);
        Intent intent = new Intent(this, PostQuizActivity.class);
        intent.putExtra("responses", mResponses);
        intent.putExtra("answers", mAnswers);
        intent.putExtra("time", mTimeElapsed);
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

    private void setUpQuestionViews(int[] selectedQuestions, int orientation) {
        int baseId = 800000;

        int layoutId;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            layoutId = R.layout.question_container_land_layout;
        else
            layoutId = R.layout.question_container_layout;

        mQuestionContainer.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mQuestionContainer.getLayoutParams();
        for (int i = 0; i < mQuestions.size(); i++) {
            Question question = mQuestions.get(i);
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout layout = (LinearLayout) inflater.inflate(layoutId, null, false);
            CustomRadioGroup rg = (CustomRadioGroup) layout.findViewById(R.id.answer_choices);

            // Set views to data from questions
            ((TextView)layout.findViewById(R.id.question)).setText(question.getQuestion());
            ((TextView)layout.findViewById(R.id.question_index)).setText((i+1) + " of " + mQuestions.size());

            LetterPicker letterPicker = new LetterPicker(); // Used for picking letters for the multiple choices

            // These are the default, existing buttons
            ArrayList<RadioButton> buttons = new ArrayList<>();
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_A));
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_B));
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_C));
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_D));
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_E));
//            buttons.add((RadioButton) rg.findViewById(R.id.answer_F));

            Answer[] answers = question.getMultiple_choice();
            int nextId = baseId;
            for (int j = 0; j < answers.length; j++) {
                Answer answer = answers[j];
                String id = answer.getId().toLowerCase();

//                if (j <= 5) {
//                    buttons.get(j).setText(answer.getAnswer());
//                    buttons.get(j).setTag(letterPicker.getNextLetter());
//                }
//                else {
                    RadioButton newButton = new RadioButton(this);
                    newButton.setText(answer.getAnswer());
                    newButton.setTag(letterPicker.getNextLetter());
                    newButton.setId(nextId);
                    nextId++;
                    buttons.add(newButton);

                    // Add to layout
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        rg.addView(newButton);
                    }
                    else if (j % 2 == 0) {
                        ((LinearLayout) rg.findViewById(R.id.left_choice_container)).addView(newButton);
                    }
                    else {
                        ((LinearLayout) rg.findViewById(R.id.right_choice_container)).addView(newButton);
                    }
//                }
            }
            baseId += answers.length;

            rg.setTag(i); // Set tag to question index for later use

            // If using landscape layout, we need to add the radiobuttons manually
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                rg.addViews(buttons);
            }

            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() { // Keep up with what answer is selected
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    CustomRadioGroup customGroup = (CustomRadioGroup) group;
                    int index = (int)customGroup.getTag();
                    RadioButton button = ((RadioButton)customGroup.findViewById(customGroup.getCheckedRadioButtonId()));
                    mResponses[index] = (String)((RadioButton)customGroup.findViewById(customGroup.getCheckedRadioButtonId())).getTag();
                    mSelectedAnswerIds[index] = customGroup.getCheckedRadioButtonId();
                }
            });
//            if (selectedQuestions != null && selectedQuestions[i] != -1)
//                buttons.get(selectedQuestions[i]).toggle();

            mViewAnimator.addView(layout, i, layoutParams);
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

    // <------------- Lifecycle overrides --------------->

    @Override
    public void onPause() {
        super.onPause();
        mCountDownTimer.cancel();
        mCountDownPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCountDownPaused)
            startCountdown(mTimeElapsed);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    // <---------- Gesture methods -------------->

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "Fling happened");
        int sensitivity = 50;
        if (e1.getX() - e2.getX() > sensitivity) { // swipe left to right
            Log.d(TAG, "Swiped left to right");
            if (mViewAnimator.getDisplayedChild() < mViewAnimator.getChildCount()-1)
                showNextQuestion();
            return true;
        }
        else if (e2.getX() - e1.getX() > sensitivity) { // swipe right to left
            Log.d(TAG, "Swiped right to left");
            if (mViewAnimator.getDisplayedChild() > 0)
                showPrevQuestion();
            return true;
        }
        return false;
    }

    // <--------------------------------------------------->

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
