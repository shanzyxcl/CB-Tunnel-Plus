package com.cbtunnel.plus.adapter;

import java.util.Collections;
import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.widget.TextView;
import android.content.Context;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import java.util.Vector;
import java.util.Date;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.os.Message;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import android.text.Html;
import android.view.MotionEvent;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.logger.LogItem;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.utils.FileUtils;
import com.cbtunnel.plus.utils.util;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.logViewHolder> implements hLogStatus.LogListener, Handler.Callback, View.OnTouchListener
{
    private static final int MESSAGE_NEWLOG = 0;
    private static final int MESSAGE_CLEARLOG = 1;
    private static final int MESSAGE_NEWTS = 2;
    private static final int MESSAGE_NEWLOGLEVEL = 3;
    public static final int TIME_FORMAT_NONE = 0;
    public static final int TIME_FORMAT_SHORT = 1;
    public static final int TIME_FORMAT_ISO = 2;
    private static final int MAX_STORED_LOG_ENTRIES = 1000;
    private Vector<LogItem> allEntries = new Vector<>();
    private final Vector<LogItem> currentLevelEntries = new Vector<>();
    private Handler mHandler;
    private final Context mContext;
    private final LinearLayoutManager mLinearLayoutManager;
    private final Vector<AdapterDataObserver> observers = new Vector<>();
    private final int mTimeFormat = -100;
    private int mLogLevel = 2;
    private final boolean mLockAutoScroll = false;

    public static class logViewHolder extends RecyclerView.ViewHolder
    {
        TextView textLog,textime;
        logViewHolder(View itemView)
        {
            super(itemView);
            this.textime = itemView.findViewById(R.id.Textime);
            this.textLog = itemView.findViewById(R.id.textLog);
            this.textime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
            this.textLog.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        }
    }


    public LogsAdapter(LinearLayoutManager layoutManager, Context context) {
        this.mContext = context;
        this.mLinearLayoutManager = layoutManager;
        initLogBuffer();
        if (mHandler == null)
        {
            mHandler = new Handler(this);
        }
        hLogStatus.addLogListener(this);
    }

    private void initLogBuffer()
    {
        allEntries.clear();
        Collections.addAll(allEntries, hLogStatus.getlogbuffer());
        initCurrentMessages();
    }

    private void initCurrentMessages() {
        try {
            currentLevelEntries.clear();
            for (LogItem li : allEntries)
            {
                if (li.getLogLevel().getInt() <= mLogLevel)
                    currentLevelEntries.add(li);
            }
        }catch (Exception ignored){
        }
    }

    @NonNull
    @Override
    public logViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View logView = inflater.inflate(R.layout.list_item_log_drawer, parent, false);
        logView.setOnTouchListener(this);
        return new logViewHolder(logView);
    }

    @Override
    @SuppressLint("RecyclerView")
    public void onBindViewHolder(@NonNull final logViewHolder viewHolder, final int position) {
        final String text;
        try {
            ConfigUtil mConfig = ConfigUtil.getInstance(mContext);
            viewHolder.textime.setTextColor(mConfig.getColorAccent());
            LogItem logItem = currentLevelEntries.get(position);
            String msg = logItem.getString(mContext);
            String time = getTime(logItem, mTimeFormat);
            text = (!time.isEmpty() ? String.format("[%s] ", time) : "") + msg;
            viewHolder.textime.setText(String.format("[%s] ", time));
            viewHolder.textLog.setText(Html.fromHtml(msg));
        }
        catch (Exception e)
        {
            hLogStatus.logException(e);
            return;
        }
        viewHolder.textLog.setOnLongClickListener(v -> {
            if(FileUtils.copyToClipboard(mContext,text)){
                util.showToast(mContext.getResources().getString(R.string.app_name), "Logs copy to clipboard");
            }
            return true;
        });
    }

    @Override
    public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer)
    {
        super.registerAdapterDataObserver(observer);
        observers.add(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer)
    {
        super.unregisterAdapterDataObserver(observer);
        observers.remove(observer);
    }

    @Override
    public int getItemCount()
    {
        return currentLevelEntries.size();
    }

    @Override
    public long getItemId(int position)
    {
        return ((Object) currentLevelEntries.get(position)).hashCode();
    }

    public boolean isEmpty()
    {
        return currentLevelEntries.isEmpty();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View p1, MotionEvent event)
    {
        // aqui deveria pausar autoscroll
		/*int action = event.getAction();

		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE){
            mLockAutoScroll = true;

            return true;
        }

		mLockAutoScroll = false;*/

        return false;
    }

    @SuppressLint("SimpleDateFormat")
    private String getTime(LogItem le, int time)
    {
        if (time != TIME_FORMAT_NONE)
        {
            Date d = new Date(le.getLogtime());
            java.text.DateFormat timeformat;
            if (time == TIME_FORMAT_ISO)
                timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            else if (time == TIME_FORMAT_SHORT)
                timeformat = new SimpleDateFormat("HH:mm");
            else
                timeformat = DateFormat.getTimeFormat(mContext);

            return timeformat.format(d);

        }
        else
        {
            return "";
        }
    }


    /**
     * Handler implementação
     */

    @Override
    public boolean handleMessage(Message msg)
    {
        // We have been called
        if (msg.what == MESSAGE_NEWLOG)
        {
            LogItem logMessage = msg.getData().getParcelable("logmessage");
            if (addLogMessage(logMessage))
            {

                for (AdapterDataObserver observer : observers)
                {
                    observer.onChanged();
                }

                if (!mLockAutoScroll)
                    scrollToLastPosition();
            }
        }
        else if (msg.what == MESSAGE_CLEARLOG)
        {
            for (AdapterDataObserver observer : observers)
            {
                observer.onChanged();
            }
            initLogBuffer();
        }
        else if (msg.what == MESSAGE_NEWTS)
        {
            for (AdapterDataObserver observer : observers)
            {
                observer.onChanged();
            }
        }
        else if (msg.what == MESSAGE_NEWLOGLEVEL)
        {
            initCurrentMessages();

            for (AdapterDataObserver observer : observers)
            {
                observer.onChanged();
            }

        }

        return true;
    }


    private boolean addLogMessage(LogItem logmessage)
    {
        allEntries.add(logmessage);

        if (allEntries.size() > MAX_STORED_LOG_ENTRIES)
        {
            Vector<LogItem> oldAllEntries = allEntries;
            allEntries = new Vector<LogItem>(allEntries.size());
            for (int i = 50; i < oldAllEntries.size(); i++)
            {
                allEntries.add(oldAllEntries.elementAt(i));
            }
            initCurrentMessages();
            return true;
        } else {
            try {
                if (logmessage.getLogLevel().getInt() <= mLogLevel) {
                    currentLevelEntries.add(logmessage);
                    return true;
                } else {
                    return false;
                }
            }catch (Exception ignored){
                return false;
            }
        }
    }

    public LogItem getItem(int position)
    {
        return currentLevelEntries.get(position);
    }

    public void clearLog()
    {
        // Actually is probably called from GUI Thread as result of the user
        // pressing a button. But better safe than sorry
        hLogStatus.clearLog();
    }

    public void scrollToLastPosition()
    {
        // scroll para ultima mensagem
        mLinearLayoutManager.scrollToPosition(mLinearLayoutManager.getItemCount() - 1);
    }

    public int getScrollToLastPosition()
    {
       return mLinearLayoutManager.getItemCount();
    }    
        
    public void setLogLevel(int level) {
        mLogLevel = level;
    }

    @Override
    public void newLog(LogItem logMessage)
    {
        Message msg = Message.obtain();

        assert (msg != null);
        msg.what = MESSAGE_NEWLOG;

        Bundle bundle = new Bundle();
        bundle.putParcelable("logmessage", logMessage);

        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onClear()
    {
        mHandler.sendEmptyMessage(MESSAGE_CLEARLOG);
    }

}