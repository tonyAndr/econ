package org.osmdroid.views.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.osmdroid.R;
import org.osmdroid.Stage;

import java.util.ArrayList;

/**
 * Created by Tony on 20-Jan-15.
 */
public class StagesAdapter extends ArrayAdapter<Stage> {
//    THIS WORKS!
//    public StagesAdapter(Context context, ArrayList<Stage> users) {
//        super(context, 0, users);
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        // Get the data item for this position
//        Stage stage = getItem(position);
//        // Check if an existing view is being reused, otherwise inflate the view
//        if (convertView == null) {
//            convertView = LayoutInflater.from(getContext()).inflate(R.layout.stage_list_item, parent, false);
//        }
//        // Lookup view for data population
//        TextView tvStage = (TextView) convertView.findViewById(R.id.tv_stagenumber);
//        TextView tvFromTo = (TextView) convertView.findViewById(R.id.tv_from_to);
//        // Populate the data into the template view using the data object
//        tvStage.setText("Stage "+(stage.number < 10 ? "0"+stage.number : stage.number));
//        tvFromTo.setText(stage.start_point + " - " + stage.end_point);
//        // Return the completed view to render on screen
//        return convertView;
//    }
//    THIS WORKS!


    //   BUT THIS MUST BE BETTER (let's just trust the smarter guy from github)
    // View lookup cache
    private static class ViewHolder {
        TextView tvStage;
        TextView tvFromTo;
    }

    public StagesAdapter(Context context, ArrayList<Stage> stages) {
        super(context, R.layout.stage_list_item, stages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Stage stage = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.stage_list_item, parent, false);
            viewHolder.tvStage = (TextView) convertView.findViewById(R.id.tv_stagenumber);
            viewHolder.tvFromTo = (TextView) convertView.findViewById(R.id.tv_from_to);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        viewHolder.tvStage.setText("Stage "+(stage.number < 10 ? "0"+stage.number : stage.number));
        viewHolder.tvFromTo.setText(stage.start_point + " - " + stage.end_point);
        // Return the completed view to render on screen
        return convertView;
    }
}