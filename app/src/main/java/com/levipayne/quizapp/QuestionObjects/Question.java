package com.levipayne.quizapp.QuestionObjects;

/**
 * Created by levi on 11/25/15.
 */
public class Question {
    String question;
    String answer;
    Answer[] multiple_choice;

    @Override
    public String toString() {
        return "{ question: " + question + ", answer: " + answer + "}";
    }
}
