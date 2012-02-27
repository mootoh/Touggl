package net.mootoh.toggltouch;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public final class TaskArrayAdapter extends ArrayAdapter<Task> {
    static final int NONASSIGNED_COLOR = Color.WHITE;

    public TaskArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.task_list_item, null);
        }

        Task task = (Task)getItem(position);
        Tag tag = Tag.getForTaskId(task.getId(), getContext());

        View colorView = view.findViewById(R.id.task_list_item_color);
        int color = (tag == null) ? NONASSIGNED_COLOR : Color.parseColor(tag.color);
        colorView.setBackgroundColor(color);

        TextView label = (TextView)view.findViewById(R.id.task_list_item_label);
        label.setText(task.getDescription());

        return view;
    };
}