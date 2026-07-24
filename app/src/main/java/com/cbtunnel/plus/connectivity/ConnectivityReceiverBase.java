package com.cbtunnel.plus.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Build.VERSION;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.core.net.ConnectivityManagerCompat;

public class ConnectivityReceiverBase {
    private static String TAG = "service.ConnectivityReceiver";
    private Object _refHandle;
    protected Context context;
    private ConnectivityManager manager = null;

    public void onAvailable(Object obj) {
    }

    public void onLosing(Object obj) {
    }

    public void onLost(Object obj) {
    }

    public ConnectivityReceiverBase(Context context) {
        this.context = context;
    }

    public void register() {
        registerFor21AndUp();
        registerFor20AndDown();
    }

    private void registerFor21AndUp() {
        if (VERSION.SDK_INT >= 21) {
            ConnectivityManager manager = getManager();
            NetworkRequest build = new Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
            NetworkCallback anonymousClass1 = new NetworkCallback() {
                public void onAvailable(Network network) {
                    Log.i(ConnectivityReceiverBase.TAG, "onAvailable");
                    ConnectivityReceiverBase.this.onAvailable(network);
                }
                public void onLosing(Network network, int i) {
                    Log.i(ConnectivityReceiverBase.TAG, "onLosing");
                    ConnectivityReceiverBase.this.onLosing(network);
                }
                public void onLost(Network network) {
                    Log.i(ConnectivityReceiverBase.TAG, "onLost");
                    ConnectivityReceiverBase.this.onLost(network);
                }
            };
            this._refHandle = anonymousClass1;
            manager.registerNetworkCallback(build, anonymousClass1);
        }
    }

    private void registerFor20AndDown() {
        if (VERSION.SDK_INT < 21) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            BroadcastReceiver anonymousClass2 = new BroadcastReceiver() {
                private boolean isOnline() {
                    NetworkInfo activeNetworkInfo = ConnectivityReceiverBase.this.getManager().getActiveNetworkInfo();
                    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
                }

                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Object obj = (action.hashCode() == -1172645946 && action.equals("android.net.conn.CONNECTIVITY_CHANGE")) ? null : -1;
                    if (obj == null) {
                        Object stringBuilder;
                        boolean booleanExtra = intent.getBooleanExtra("noConnectivity", false);
                        boolean booleanExtra2 = intent.getBooleanExtra("isFailover", false);
                        boolean isOnline = isOnline();
                        NetworkInfo networkInfoFromBroadcast = ConnectivityManagerCompat.getNetworkInfoFromBroadcast(ConnectivityReceiverBase.this.getManager(), intent);
                        StringBuilder stringBuilder2;
                        try {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(networkInfoFromBroadcast.getTypeName());
                            stringBuilder2.append(networkInfoFromBroadcast.getSubtypeName());
                            stringBuilder = stringBuilder2.toString();
                        } catch (NullPointerException unused) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("---");
                            stringBuilder2.append(networkInfoFromBroadcast.getSubtypeName());
                            stringBuilder = stringBuilder2.toString();
                        }
                        Log.i(ConnectivityReceiverBase.TAG, String.format("ConnectivityReceiver: CONNECTIVITY_ACTION conn=%b fo=%b", new Object[]{Boolean.valueOf(isOnline), Boolean.valueOf(booleanExtra2)}));
                        if (booleanExtra2) {
                            ConnectivityReceiverBase.this.onLosing(stringBuilder);
                        }
                        if (booleanExtra && !isOnline) {
                            ConnectivityReceiverBase.this.onLost(stringBuilder);
                        }
                        if (!booleanExtra && isOnline) {
                            ConnectivityReceiverBase.this.onAvailable(stringBuilder);
                        }
                    }
                }
            };
            this._refHandle = anonymousClass2;
            ContextCompat.registerReceiver(context,anonymousClass2, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    public void unregister() {
        unregisterFor21AndUp();
        unregisterFor20AndDown();
    }

    private void unregisterFor21AndUp() {
        if (VERSION.SDK_INT >= 21) {
            getManager().unregisterNetworkCallback((NetworkCallback) this._refHandle);
        }
    }

    private void unregisterFor20AndDown() {
        if (VERSION.SDK_INT < 21) {
            this.context.unregisterReceiver((BroadcastReceiver) this._refHandle);
        }
    }

    protected ConnectivityManager getManager() {
        if (this.manager == null) {
            this.manager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return this.manager;
    }
}
