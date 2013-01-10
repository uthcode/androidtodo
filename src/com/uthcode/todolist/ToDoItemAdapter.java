package com.uthcode.todolist;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ToDoItemAdapter extends ArrayAdapter<ToDoItem> {
    private int mResource;
    private LayoutInflater mInflator;

    public ToDoItemAdapter(Context context, int resource, List<ToDoItem> items) {
        super(context, resource, items);
        mResource = resource;
        mInflator = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = (LinearLayout) mInflator.inflate(mResource, null);
        }
        final LinearLayout todoView = (LinearLayout) convertView;

        final ToDoItem item = getItem(position);
        final ToDoListItemView taskView = (ToDoListItemView) todoView.findViewById(R.id.row);
        taskView.setText(item.getTask());
        taskView.setItem(item.getItem());

        return todoView;
    }
}
