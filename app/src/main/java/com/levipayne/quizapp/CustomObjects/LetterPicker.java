package com.levipayne.quizapp.CustomObjects;

/**
 * This class is used to generate an indefinite number of letters for multiple choices questions
 */
public class LetterPicker {
    private String[] mBaseLetters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private String[] mCurrentLetters = mBaseLetters;
    private int mNextIndex = 0;
    private String mNextLetter = mCurrentLetters[mNextIndex];


    public String getNextLetter() {
        String next = mNextLetter;
        if (mNextIndex == mCurrentLetters.length - 1) {
            generateNewList();
        }
        else {
            mNextIndex++;
            mNextLetter = mCurrentLetters[mNextIndex];
        }
        return next;
    }

    private void generateNewList() {
        String[] newList = mCurrentLetters;
        for (int i = 0; i < newList.length; i++) {
            newList[i] += mBaseLetters[i];
        }
        mCurrentLetters = newList;
        mNextIndex = 0;
        mNextLetter = mCurrentLetters[mNextIndex];
    }
}
