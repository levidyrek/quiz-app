package com.levipayne.quizapp.QuestionObjects;

/**
 * Created by levi on 11/25/15.
 */
public class Question {
    String question;
    String answer;
    Answer[] multiple_choice;

    public Answer[] getMultiple_choice() {
        return multiple_choice;
    }

    public String getAnswer() {
        return answer;
    }

    public String getQuestion() {
        return question;
    }

    @Override

    public String toString() {
        return "{ question: " + question + ", answer: " + answer + "}";
    }
}
