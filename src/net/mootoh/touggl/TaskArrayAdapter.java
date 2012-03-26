package net.mootoh.touggl;

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

        View colorView = view.findViewById(R.id.task_list_item_color);
        TextView label = (TextView)view.findViewById(R.id.task_list_item_label);

        Task task = (Task)getItem(position);
        Tag tag = Tag.getForTaskId(task.getId(), getContext());

        if (tag == null) {
            colorView.setBackgroundColor(NONASSIGNED_COLOR);
            label.setTextColor(Color.BLACK);
        } else {
            colorView.setBackgroundColor(Color.parseColor(tag.color));
            label.setTextColor(Color.GRAY);
        }

        label.setText(task.getDescription());

        return view;
    };
}