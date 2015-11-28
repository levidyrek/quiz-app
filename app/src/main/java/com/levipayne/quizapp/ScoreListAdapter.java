package com.levipayne.quizapp;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.parse.ParseObject;

import java.util.List;

/**
 * Created by levi on 11/27/15.
 */
public class ScoreListAdapter extends ArrayAdapter<ParseObject> {

    public ScoreListAdapter(Context context, int resource, List<ParseObject> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ParseObject object = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.leaderboard_list_item, parent, false);
        }

        TextView tvUsername = (TextView) convertView.findViewById(R.id.username);
        TextView tvPercent = (TextView) convertView.findViewById(R.id.percentage);
        TextView tvSeconds = (TextView) convertView.findViewById(R.id.seconds);

        tvUsername.setText(object.getString("username"));
        tvPercent.setText(String.valueOf(object.getInt("percentage")));
        tvSeconds.setText(String.valueOf(object.getInt("seconds")));

        return convertView;
    }
}
