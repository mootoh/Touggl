package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

public final class NewTagActivity extends Activity {
    private String selectedColor = null;
    private String tagId = null;
    private int selectedTask = -1;

    final String[] colors = {
            "#ff7f00",
            "#ff007f",
            "#66ff66",
            "#ffff66",
            "#007fff"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tagId = getIntent().getStringExtra(TagTouchActivity.TAGID_KEY);

        setTitle("New Tag: " + tagId);
        setContentView(R.layout.tag_touch);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setupColors();
        setupTaskList();
        hideTaskSelection();
    }
    private void setupColors() {
        final RadioGroup radioGroup = (RadioGroup)findViewById(R.id.colorRadioGroup);

        for (int j = 0; j < radioGroup.getChildCount(); j++) {
            final ToggleButton toggleButton = (ToggleButton) radioGroup.getChildAt(j);
            toggleButton.setBackgroundColor(Color.parseColor(colors[j]));
            toggleButton.setAlpha(0.3f);
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int j = 0; j < radioGroup.getChildCount(); j++) {
                    final ToggleButton toggleButton = (ToggleButton) radioGroup.getChildAt(j);
                    boolean checked = toggleButton.getId() == checkedId;
                    toggleButton.setChecked(checked);
                    toggleButton.setAlpha(checked ? 1.0f : 0.3f);

                    if (checked)
                        selectedColor = colors[j];
                }
                showTaskSelection();
            }
        });
    }

    public void onToggle(View view) {
        RadioGroup radioGroup = (RadioGroup)view.getParent();
        radioGroup.check(view.getId());
    }

    private void setupTaskList() {
        final Task[] tasks = Task.getAll(this);
        ArrayList<Task> taskList = new ArrayList<Task>();
        for (Task task : tasks)
            taskList.add(task);

        ListView taskListView = (ListView)findViewById(R.id.tagTouchTaskList);
        TaskArrayAdapter taskAdapter = new TaskArrayAdapter(this, R.layout.task_list_item, R.id.task_list_item_label, taskList);
        taskListView.setAdapter(taskAdapter);

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedTask = position;

                ActionBar actionBar = getActionBar();
                actionBar.show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_done:
            decided();
            finish();
            break;
        case R.id.menu_start:
            Tag tag = decided();
            tag.onTouched(this);
            finish();
            break;
        default:
            break;
        }
        return true;
    }

    private Tag decided() {
        Tag tag = null;
        tag = new Tag(tagId, "a", selectedColor);
        try {
            tag.save(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ListView taskListView = (ListView)findViewById(R.id.tagTouchTaskList);
        Task task = (Task)taskListView.getItemAtPosition(selectedTask);
        tag.assignTask(task, this);
        return tag;
    }
    
    private void showTaskSelection() {
        View label = findViewById(R.id.tagTouchMessageLabel);
        label.setVisibility(View.VISIBLE);
        View list = findViewById(R.id.tagTouchTaskList);
        list.setVisibility(View.VISIBLE);
    }

    private void hideTaskSelection() {
        View label = findViewById(R.id.tagTouchMessageLabel);
        label.setVisibility(View.INVISIBLE);
        View list = findViewById(R.id.tagTouchTaskList);
        list.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_tag_activity, menu);
        return true;
    }
}