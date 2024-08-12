package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<MyLog> myLogs;

    public LogAdapter(List<MyLog> myLogs) {
        this.myLogs = myLogs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        MyLog myLog = myLogs.get(position);
        holder.logTitle.setText(myLog.getTitle());
        holder.logInvokedBy.setText(myLog.getInvokedBy());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        holder.logDate.setText(dateFormat.format(myLog.getDate()));

        String firstLine = myLog.getNotes().split("\n")[0];
        holder.logNotes.setText(firstLine);
        myLog.setCollapsed(true);

        holder.itemView.setOnClickListener(v -> {
            if (myLog.isCollapsed()) {
                expandTextView(holder.logNotes, myLog.getNotes());
            } else {
                collapseTextView(holder.logNotes, firstLine);
            }
            myLog.setCollapsed(!myLog.isCollapsed());
        });
    }

    private void expandTextView(MaterialTextView textView, String fullText) {
        textView.setText(fullText);
        textView.measure(
                View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int targetHeight = textView.getMeasuredHeight();

        // Ensure the height of the TextView is properly updated before animation
        ValueAnimator animator = ValueAnimator.ofInt(textView.getHeight(), targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration(100);
        animator.start();
    }

    private void collapseTextView(MaterialTextView textView, String firstLine) {
        int initialHeight = textView.getHeight();
        textView.setText(firstLine);
        textView.measure(
                View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int targetHeight = textView.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration(20);
        animator.start();
    }

    @Override
    public int getItemCount() {
        return myLogs.size();
    }

    public void setLogList(List<MyLog> myLogs) {
        this.myLogs = myLogs;
        notifyDataSetChanged();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView logIcon, invokedByIcon, dateIcon;
        MaterialTextView logTitle, logNotes, logInvokedBy, logDate;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logIcon = itemView.findViewById(R.id.log_icon);
            logTitle = itemView.findViewById(R.id.text_title);
            logNotes = itemView.findViewById(R.id.text_notes);
            logInvokedBy = itemView.findViewById(R.id.text_invoked_by);
            logDate = itemView.findViewById(R.id.text_date);
            dateIcon = itemView.findViewById(R.id.date_icon);
        }
    }
}
