package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        holder.logNotes.setText(myLog.getNotes());
        holder.logInvokedBy.setText(myLog.getInvokedBy());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String firstLine = myLog.getNotes().split("\n")[0];
        holder.logDate.setText(dateFormat.format(myLog.getDate()));
        holder.logNotes.setText(firstLine);
        collapseTextView(holder.logNotes, 1);
        myLog.setCollapsed(true);

        holder.itemView.setOnClickListener(v -> {
            if (myLog.isCollapsed()) {
                holder.logNotes.setText(myLog.getNotes());
                expandTextView(holder.logNotes);
            } else {
                collapseTextView(holder.logNotes, 1);
                holder.logNotes.setText(firstLine);
            }
            myLog.setCollapsed(!myLog.isCollapsed());
        });
    }

    private void expandTextView(MaterialTextView textView) {
        int initialHeight = textView.getMeasuredHeight();
        textView.setMaxLines(Integer.MAX_VALUE);
        textView.measure(View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int targetHeight = textView.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration((long) (Math.max(targetHeight - initialHeight, 0) * 1.5));
        animator.start();
    }

    private void collapseTextView(MaterialTextView textView, int maxLines) {
        int initialHeight = textView.getMeasuredHeight();
        textView.setMaxLines(maxLines);
        textView.measure(View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int targetHeight = textView.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration((long) (Math.max(initialHeight - targetHeight, 0) * 1.5));
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
