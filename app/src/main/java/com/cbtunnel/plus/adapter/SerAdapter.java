package com.cbtunnel.plus.adapter;

import android.content.Context;
import android.widget.BaseAdapter;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import java.util.ArrayList;
import android.widget.TextView;
import android.widget.ImageView;
import java.io.InputStream;
import android.graphics.drawable.Drawable;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;

public class SerAdapter extends BaseAdapter {
    ConfigUtil mConfig;
    Context context;
    ImageView im;
    TextView tv;
    ArrayList<String> data;
    public SerAdapter(Context context, ArrayList<String> list) {
        this.context = context;
        this.data = list;
        mConfig = ConfigUtil.getInstance(context);
    }

    @Override
    public int getCount() {
        return data.size();
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
    public View getView(final int position, View c, ViewGroup parent) {
        c=LayoutInflater.from(context).inflate(R.layout.dialog_server_spinner,parent,false);
        im=c.findViewById(R.id.sIcon);
        tv=c.findViewById(R.id.sName);
        tv.setTextColor(mConfig.gettextColor());
        try
        {
            tv.setText(data.get(position).replace("flag_","").replace(".png","").replace("_"," "));
            InputStream open = context.getAssets().open(new StringBuffer().append("flags/").append(data.get(position).toString()).toString());
            im.setImageDrawable(Drawable.createFromStream(open, (String) null));
            if (open != null) {
                open.close();
            }
        }
        catch (Exception e)
        {
            tv.setText(e.getMessage());
        }
        return c;
    }
}