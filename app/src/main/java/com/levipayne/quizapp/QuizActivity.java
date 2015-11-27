package com.levipayne.quizapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.levipayne.quizapp.QuestionObjects.Answer;
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

public class QuizActivity extends Activity {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

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

        if (savedInstanceState == null) { // First time onCreate has been executed
            try {
                // Load questions from url and convert them into Question objects
                showProgressDialog();
                mJsonText = new URLtoJSONTask().execute(QUESTIONS_URL).get();
                mQuestions = getQuestionsFromJSON(mJsonText);
                setUpQuestionViews(null); // Set up ViewAnimator with question views
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

            // Prepare to track answers
            mResponses = new String[mViewAnimator.getChildCount()];
            mSelectedAnswerIds = new int[mViewAnimator.getChildCount()];
            for (int i = 0; i < mViewAnimator.getChildCount(); i++) { // Set default values
                mResponses[i] = "";
                mSelectedAnswerIds[i] = -1;
            }
        }
        else { // Restore data from savedInstanceState
            int[] selectedAnswers = savedInstanceState.getIntArray("mSelectedAnswerIds");

            // Prepare to track answers
            mResponses = savedInstanceState.getStringArray("mResponses");
            mSelectedAnswerIds = savedInstanceState.getIntArray("mSelectedAnswerIds");

            mJsonText = savedInstanceState.getString("mJsonText");
            mTimeElapsed = savedInstanceState.getLong("mTimeElapsed");
            try {
                mQuestions = getQuestionsFromJSON(mJsonText);
//                setUpQuestionViews(selectedAnswers);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Set animator to correct question and correct visibility for prev/next buttons
            int currentQuestion = savedInstanceState.getInt("currentQuestion");
            mViewAnimator.setDisplayedChild(currentQuestion);
            if (currentQuestion == 0)
                mPrevButton.setVisibility(View.INVISIBLE);
            else if (currentQuestion == mViewAnimator.getChildCount()-1)
                mNextButton.setVisibility(View.INVISIBLE);
        }

        mAnswers = new String[mQuestions.size()];
        for (int i = 0; i < mAnswers.length; i++) {
            mAnswers[i] = mQuestions.get(i).getAnswer();
        }
        mCountDownPaused = false;
        startCountdown(mTimeElapsed);
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
        mViewAnimator.showNext();
        if (mViewAnimator.getDisplayedChild() == mViewAnimator.getChildCount()-1) {
            mNextButton.setVisibility(View.INVISIBLE);
            mFinishButton.setVisibility(View.VISIBLE);
        }
        else if (mViewAnimator.getDisplayedChild() > 0)
            mPrevButton.setVisibility(View.VISIBLE);
    }

    public void showPrevQuestion() {
        mViewAnimator.showPrevious();
        if (mViewAnimator.getDisplayedChild() == 0)
            mPrevButton.setVisibility(View.INVISIBLE);
        else if (mViewAnimator.getDisplayedChild() < mViewAnimator.getChildCount()-1) {
            mNextButton.setVisibility(View.VISIBLE);
            mFinishButton.setVisibility(View.GONE);
        }
    }

    private void setRadioListeners() {
        mRadioA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadioB.setChecked(false);
                mRadioC.setChecked(false);
                mRadioD.setChecked(false);
            }
        });

        mRadioB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadioA.setChecked(false);
                mRadioC.setChecked(false);
                mRadioD.setChecked(false);
            }
        });

        mRadioC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadioB.setChecked(false);
                mRadioA.setChecked(false);
                mRadioD.setChecked(false);
            }
        });

        mRadioD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadioB.setChecked(false);
                mRadioC.setChecked(false);
                mRadioA.setChecked(false);
            }
        });
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
        Log.d(TAG, mResponses[0] + "," + mResponses[1] + "," + mResponses[2] + "," + mResponses[3]+ "," + mResponses[4]+ "," + mResponses[5]);
        Intent intent = new Intent(this, PostQuizActivity.class);
        intent.putExtra("responses", mResponses);
        intent.putExtra("answers", mAnswers);
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

    private void setUpQuestionViews(int[] selectedQuestions) {
        mQuestionContainer.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mQuestionContainer.getLayoutParams();
        for (int i = 0; i < mQuestions.size(); i++) {
            Question question = mQuestions.get(i);
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.question_container_layout, null, false);

            // Set views to data from questions
            ((TextView)layout.findViewById(R.id.question)).setText(question.getQuestion());
            ((TextView)layout.findViewById(R.id.question_index)).setText((i+1) + " of " + mQuestions.size());

            RadioButton[] buttons = { (RadioButton)layout.findViewById(R.id.answer_A),
                    (RadioButton)layout.findViewById(R.id.answer_B),
                    (RadioButton)layout.findViewById(R.id.answer_C),
                    (RadioButton)layout.findViewById(R.id.answer_D),
                    (RadioButton)layout.findViewById(R.id.answer_E),
                    (RadioButton)layout.findViewById(R.id.answer_F)};
            Answer[] answers = question.getMultiple_choice();
            for (int j = 0; j < answers.length; j++) {
                Answer answer = answers[j];
                String id = answer.getId().toLowerCase();
                switch (id) {
                    case "a":
                        buttons[0].setText(answer.getAnswer());
                        buttons[0].setTag("a");
                        break;
                    case "b":
                        buttons[1].setText(answer.getAnswer());
                        buttons[1].setTag("b");
                        break;
                    case "c":
                        buttons[2].setText(answer.getAnswer());
                        buttons[2].setTag("c");
                        break;
                    case "d":
                        buttons[3].setText(answer.getAnswer());
                        buttons[3].setTag("d");
                        break;
                    case "e":
                        buttons[4].setText(answer.getAnswer());
                        buttons[4].setTag("e");
                        break;
                    case "f":
                        buttons[5].setText(answer.getAnswer());
                        buttons[5].setTag("f");
                        break;
                    default:
                        // TODO: Make it so that more choices could appear
                }
            }

            // Mark answers as selected if there was an orientation change, etc.
//            if (selectedQuestions != null) {
//                if (selectedQuestions[i] != -1)
//                    buttons[selectedQuestions[i]].setChecked(true);
//            }
            RadioGroup rg = (RadioGroup) layout.findViewById(R.id.answer_choices);
            rg.setTag(i); // Set tag to question index for later use
            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() { // Keep up with what answer is selected
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int index = (int)group.getTag();
                    mResponses[index] = (String)((RadioButton)group.findViewById(group.getCheckedRadioButtonId())).getTag();
                    mSelectedAnswerIds[index] = group.getCheckedRadioButtonId();
                }
            });
            if (selectedQuestions != null && selectedQuestions[i] != -1)
                rg.check(selectedQuestions[i]);

            // Make unused choices become invisible
            for (int j = buttons.length-1; j > answers.length-1; j--) {
                buttons[j].setVisibility(View.GONE);
            }

            mViewAnimator.addView(layout, i, layoutParams);
        }

//        Animation leftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
//        Animation rightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
//        mViewAnimator.setInAnimation(rightAnimation);
//        mViewAnimator.setOutAnimation(leftAnimation);
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
        RadioButton answerA = (RadioButton) findViewById(R.id.answer_A);
        answerA.setChecked(true);
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
