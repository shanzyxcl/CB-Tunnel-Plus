package com.cbtunnel.plus.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;

public class portAdapter extends BaseAdapter {
    ConfigUtil mConfig;
    Context context;
    private TextView tv;
    public String[] data;

    public portAdapter(Context context, String[] list) {
        this.context = context;
        this.data = list;
        mConfig = ConfigUtil.getInstance(context);
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View c, ViewGroup parent) {
        c=LayoutInflater.from(context).inflate(R.layout.custom_port_spinner_dropdown,parent,false);
        tv=c.findViewById(R.id.portName1);
        tv.setTextColor(mConfig.gettextColor());
        tv.setText(data[position]);
        return c;
    }

    @Override
    @SuppressLint("ViewHolder")
    public View getView(final int position, View c, ViewGroup parent) {
        c=LayoutInflater.from(context).inflate(R.layout.custom_port_spinner,parent,false);
        tv=c.findViewById(R.id.portName);
        tv.setTextColor(mConfig.gettextColor());
        tv.setText((data[position]).equals("Default")?"Default":"Port:"+data[position]);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,9);
        return c;
    }
    
}