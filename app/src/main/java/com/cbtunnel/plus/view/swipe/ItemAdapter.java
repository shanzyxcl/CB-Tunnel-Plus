package com.cbtunnel.plus.view.swipe;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.Pair;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import com.cbtunnel.plus.config.ConfigDataBase;
import com.cbtunnel.plus.harliesApplication;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.utils.util;
import com.cbtunnel.plus.view.PayloadDialog;
import com.cbtunnel.plus.view.SSLDialog;
import com.cbtunnel.plus.view.ServerDialog;

public class ItemAdapter extends DragItemAdapter<Pair<Long, String>, ItemAdapter.ViewHolder> implements SettingsConstants {

    private final int mLayoutId;
    private final int ConfigType;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private boolean isConfigSearch = false;
    private final Context mContext;
    private final ArrayList<Pair<Long, String>> data;
    private final ArrayList<Pair<Long, String>> newData;
    private final ConfigUtil mConfig;
    private ConfigDataBase serverData,networkData;
    private final SharedPreferences mPref;
    private final SharedPreferences.Editor mEditor;
    private OnSelectedSerListener mListener;
    public interface OnSelectedSerListener {
        void onSelectSer(String name);
        void onReloadConfig(int position);
    }
    public void setOnSelectedSerListener(OnSelectedSerListener listener) {
        this.mListener = listener;
    }

    public ItemAdapter(Context mContext,ArrayList<Pair<Long, String>> list, int layoutId, int grabHandleId, int ConfigType) {
        this.mContext = mContext;
        serverData = new ConfigDataBase(mContext, "mServerData");
        networkData = new ConfigDataBase(mContext, "mNetwrokData");
        mPref = harliesApplication.getPrivateSharedPreferences();
        mEditor = mPref.edit();
        mConfig = ConfigUtil.getInstance(mContext);
        this.newData = new ArrayList<>();
        this.ConfigType = ConfigType;
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = false;
        this.data = list;
        this.newData.addAll(list);
        setItemList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        InputStream open;
        try {
            JSONObject js =  new JSONObject(mItemList.get(position).second);
            holder.mName1.setText(js.getString("Name"));
            holder.itemView.setTag(mItemList.get(position));
            if (ConfigType==0){
                holder.mName2.setText(getServerType(js));
                open = mContext.getAssets().open("flags/" + "flag_" + js.getString("FLAG") + ".png");
            }else{
                holder.mName2.setText(getNetworkType(js));
                if (js.has("serverType")){
                    open = mContext.getAssets().open("flags/" + "flag_" + js.getString("FLAG") + ".png");
                }else{
                    open = mContext.getAssets().open("networks/" + "icon_" + js.getString("FLAG") + ".png");
                }
            }
            if (position==mPref.getInt(SERVER_POSITION,0)&&ConfigType==0){
                holder.mSelectedItem.setImageResource(R.drawable.ic_item_selected);
                holder.mSelectedItem.setColorFilter(mContext.getResources().getColor(R.color.connect_color), PorterDuff.Mode.SRC_IN);
            }else if (position==mPref.getInt(NETWORK_POSITION,0)&&ConfigType==1){
                holder.mSelectedItem.setImageResource(R.drawable.ic_item_selected);
                holder.mSelectedItem.setColorFilter(mContext.getResources().getColor(R.color.connect_color), PorterDuff.Mode.SRC_IN);
            }else{
                holder.mSelectedItem.setImageResource(R.drawable.ic_item_unselected);
                holder.mSelectedItem.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            }
            if (ConfigType==0 && mPref.getBoolean("isRandom",false)){
                holder.mSelectedItem.setImageResource(R.drawable.ic_item_unselected);
                holder.mSelectedItem.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            }
            holder.mIcon.setImageDrawable(Drawable.createFromStream(open, null));
            open.close();
        } catch (Exception e) {
            holder.mName1.setText(e.getMessage());
        }
    }

    public String getJS(int position){
        try {
            JSONObject js =  new JSONObject(mItemList.get(position).second);
            return js.getString("Name");
        } catch (Exception e) {
           return null;
        }
    }

    public void mDelete(int position){
        try {
            ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();
            JSONArray ja = new JSONArray(getNewJS());
            ja.remove(position);
            newData.clear();
            clearItemList();
            for (int i=0;i < ja.length();i++) {
                mItemArray.add(new Pair<>((long) i, ja.getJSONObject(i).toString()));
            }
            newData.addAll(mItemArray);
            setItemList(mItemArray);
            notifyDataSetChanged();
            mListener.onReloadConfig(0);
        } catch (Exception e) {
            util.showToast("Error!", e.getMessage());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addServer() {
        final ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();
        ServerDialog a = new ServerDialog(mContext);
        a.add();
        a.onServerAdd(json -> {
            try {
                JSONArray ja = new JSONArray(getNewJS());
                ja.put(json);
                newData.clear();
                clearItemList();
                for (int i=0;i < ja.length();i++) {
                    mItemArray.add(new Pair<>((long) i, ja.getJSONObject(i).toString()));
                }
                newData.addAll(mItemArray);
                setItemList(mItemArray);
                notifyDataSetChanged();
                mListener.onReloadConfig(ja.length()-1);
            } catch (Exception e) {
                util.showToast("showManualServer Error!", e.getMessage());
            }
        });
        a.init();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void addPayload() {
        final ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();
        PayloadDialog a=new PayloadDialog(mContext);
        a.add();
        a.onPayloadAdd(json -> {
            try {
                JSONArray ja=new JSONArray(getNewJS());
                ja.put(json);
                newData.clear();
                clearItemList();
                for (int i=0;i < ja.length();i++) {
                    mItemArray.add(new Pair<>((long) i, ja.getJSONObject(i).toString()));
                }
                newData.addAll(mItemArray);
                setItemList(mItemArray);
                notifyDataSetChanged();
                mListener.onReloadConfig(ja.length()-1);
            } catch (JSONException e) {
                util.showToast("Error!", e.getMessage());
            }
        });
        a.init();
    }
    @SuppressLint("NotifyDataSetChanged")
    public void addSSL() {
        final ArrayList<Pair<Long, String>> mItemArray = new ArrayList<>();
        SSLDialog a=new SSLDialog(mContext);
        a.add();
        a.onPayloadAdd(json -> {
            try {
                JSONArray ja=new JSONArray(getNewJS());
                ja.put(json);
                newData.clear();
                clearItemList();
                for (int i=0;i < ja.length();i++) {
                    mItemArray.add(new Pair<>((long) i, ja.getJSONObject(i).toString()));
                }
                newData.addAll(mItemArray);
                setItemList(mItemArray);
                notifyDataSetChanged();
                mListener.onReloadConfig(ja.length()-1);
            } catch (JSONException e) {
                util.showToast("Error!", e.getMessage());
            }
        });
        a.init();
    }

    private String mSType() throws JSONException {
        JSONObject js = getNetworkArrayDragaPosition().getJSONObject(mPref.getInt(NETWORK_POSITION, 0));
        if (js.has("serverType")){
            return "V2RAY";
        } else if (js.getInt("proto_spin") == 0) {
            return "OVPN_SSH_KEY";
        } else if (js.getInt("proto_spin") == 1) {
            return "UDP_KEY";
        } else if (js.getInt("proto_spin") == 2) {
            return "DNS_KEY";
        } else if (js.getInt("proto_spin") == 3) {
            return "OVPN_SSH_KEY";
        } else if (js.getInt("proto_spin") == 4) {
            return "OVPN_SSH_KEY";
        } else if (js.getInt("proto_spin") == 5) {
            return "OVPN_SSH_KEY";
        }
        return "OVPN_SSH_KEY";
    }

    private JSONArray getNetworkArrayDragaPosition(){
        try {
            JSONArray jar = new JSONArray();
            JSONArray jar1 = new JSONArray(networkData.getData());
            JSONArray jar2 = new JSONArray(serverData.getData());
            for (int i=0;i < jar1.length();i++) {
                jar.put(jar1.getJSONObject(i));
            }
            for (int i=0;i < jar2.length();i++) {
                if (jar2.getJSONObject(i).getInt("serverType") == 3) {
                    jar.put(jar2.getJSONObject(i));
                }
            }
            return jar;
        } catch (JSONException e) {
            util.showToast("Error!", e.toString());
        }
        return null;
    }

    public boolean getServerDialog(){
        try {
            String serType = mSType();
            if (ConfigType==0){
                return true;
            }else{
                if (serType.equals("OVPN_SSH_KEY")){
                    mEditor.putInt(network_spin_mSelection_key,0).apply();
                } else if (serType.equals("DNS_KEY")){
                    mEditor.putInt(network_spin_mSelection_key,2).apply();
                } else if (serType.equals("UDP_KEY")){
                    mEditor.putInt(network_spin_mSelection_key,1).apply();
                }
                return false;
            }
        } catch (JSONException ignored) {
            return false;
        }
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    public class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView mName1,mName2;
        AppCompatImageView mSelectedItem,mIcon;
        RelativeLayout mDragBtn;
        LinearLayout item_layout;
        @SuppressLint("NotifyDataSetChanged")
        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            item_layout = itemView.findViewById(R.id.item_layout);
            mName1 = itemView.findViewById(R.id.server_item_title);
            mName2 = itemView.findViewById(R.id.server_description);
            mSelectedItem = itemView.findViewById(R.id.mSelectedItem);
            mIcon = itemView.findViewById(R.id.server_item_icon);
            mDragBtn = itemView.findViewById(R.id.image);
            mDragBtn.setVisibility(View.GONE);
            item_layout.setOnClickListener(v -> mListener.onSelectSer(getJS(getAdapterPosition())));
        }

        @Override
        public void onItemClicked(View view) {
            mListener.onSelectSer(getJS(getAdapterPosition()));
        }

        @Override
        public boolean onItemLongClicked(View view) {
            return true;
        }
    }

    public String getNewJS(){
        try {
            JSONArray jarr = new JSONArray();
            for (Pair<Long, String> str : getItemList()){
                JSONObject js =  new JSONObject(str.second);
                jarr.put(js);
            }
            return jarr.toString();
        } catch (Exception e) {
            util.showToast("Error!", e.getMessage());
            return null;
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    public void filter(String charText){
        try {
            charText = charText.toLowerCase(Locale.getDefault());
            data.clear();
            clearItemList();
            if (charText.isEmpty()){
                isConfigSearch = false;
                data.addAll(newData);
            }
            else {
                isConfigSearch = true;
                for (Pair<Long, String> model : newData){
                    String name =  new JSONObject(model.second).getString("Name");
                    if (name.toLowerCase(Locale.getDefault()).contains(charText)){
                        data.add(model);
                    }
                }
            }
            setItemList(data);
            notifyDataSetChanged();
        } catch (Exception e) {
            util.showToast("Error!", e.getMessage());
        }
    }

    private String getNetworkType(JSONObject js) throws JSONException {
        if (js.has("Info")){
            if(js.getString("Info").isEmpty()){
                if (js.has("serverType")){
                    return "v2ray";
                }else{
                    boolean is = (js.getString("Name").contains("Direct")||js.getString("Name").contains("direct"));
                    if (js.getInt("proto_spin") == 0) {
                        if (is){
                            if (js.getString("NetworkPayload").isEmpty()){
                                return "Direct";
                            }else{
                                return "Direct Payload";
                            }
                        }
                        return "HTTP PROXY";
                    } else if (js.getInt("proto_spin") == 1) {
                        return "UDP HYSTERIA";
                    } else if (js.getInt("proto_spin") == 2) {
                        return "SLOWDNS";
                    } else if (js.getInt("proto_spin") == 3) {
                        return "SSL/SNI";
                    } else if (js.getInt("proto_spin") == 4) {
                        return "SSL+PAYLOAD";
                    } else if (js.getInt("proto_spin") == 5) {
                        return "SSL+PROXY";
                    }
                }
            } else{
                return js.getString("Info");    
            }           
        }else if (js.has("serverType")){
            return "v2ray";
        }else{
            boolean is = (js.getString("Name").contains("Direct")||js.getString("Name").contains("direct"));
            if (js.getInt("proto_spin") == 0) {
                if (is){
                    if (js.getString("NetworkPayload").isEmpty()){
                        return "Direct";
                    }else{
                        return "Direct Payload";
                    }
                }
                return "HTTP PROXY";
            } else if (js.getInt("proto_spin") == 1) {
                return "UDP HYSTERIA";
            } else if (js.getInt("proto_spin") == 2) {
                return "SLOWDNS";
            } else if (js.getInt("proto_spin") == 3) {
                return "SSL/SNI";
            } else if (js.getInt("proto_spin") == 4) {
                return "SSL+PAYLOAD";
            } else if (js.getInt("proto_spin") == 5) {
                return "SSL+PROXY";
            }
        }
        return "Unknown!";
    }


    private String getServerType(JSONObject js) throws JSONException {
        if (js.has("Server_msg")){
            if(js.getString("Server_msg").isEmpty()){
                if (js.getInt("serverType")==0){
                    return "OpenVPN";
                }else if (js.getInt("serverType")==1){
                    return "SSH Tunnel";
                } else if (js.getInt("serverType")==2){
                    return "DNSTT";
                } else if (js.getInt("serverType")==3){
                    return "v2ray";
                } else if (js.getInt("serverType")==4){
                    return "Hysteria UDP";
                }
            }else{
                return js.getString("Server_msg");    
            }
        }else if (js.getInt("serverType")==0){
            return "OpenVPN";
        }else if (js.getInt("serverType")==1){
            return "SSH Tunnel";
        } else if (js.getInt("serverType")==2){
            return "DNSTT";
        } else if (js.getInt("serverType")==3){
            return "v2ray";
        } else if (js.getInt("serverType")==4){
            return "Hysteria UDP";
        }
        return "Random";
    }

}
