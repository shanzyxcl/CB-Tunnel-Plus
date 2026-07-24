package com.cbtunnel.plus.connectivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build.VERSION;

public abstract class ConnectionState {

    public static class ConnectionStateV20AndLower extends ConnectionState {
        private boolean hasLte = false;
        private boolean hasWifi = false;

        public ConnectionStateV20AndLower(ConnectivityManager connectivityManager) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(1);
            NetworkInfo networkInfo2 = connectivityManager.getNetworkInfo(0);
            if (networkInfo != null) {
                this.hasWifi = networkInfo.isConnectedOrConnecting();
            }
            if (networkInfo2 != null) {
                this.hasLte = networkInfo2.isConnectedOrConnecting();
            }
        }

        public boolean hasChanged(ConnectionState connectionState) {
            return hasChanged((ConnectionStateV20AndLower) connectionState);
        }

        public boolean hasChanged(ConnectionStateV20AndLower connectionStateV20AndLower) {
            boolean z = false;
            if (this.hasWifi && connectionStateV20AndLower.hasWifi) {
                return false;
            }
            if (this.hasWifi != connectionStateV20AndLower.hasWifi) {
                return true;
            }
            if (this.hasLte != connectionStateV20AndLower.hasLte) {
                z = true;
            }
            return z;
        }

        public boolean isConnected() {
            return this.hasWifi || this.hasLte;
        }

        public boolean isDisconnected() {
            return !isConnected();
        }
    }

    public static class ConnectionStateV21AndHigher extends ConnectionState {
        private int mLteActiveNetworks = 0;
        private int mWifiActiveNetworks = 0;

        public ConnectionStateV21AndHigher(ConnectivityManager connectivityManager) {
            for (Network networkInfo : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo2 = connectivityManager.getNetworkInfo(networkInfo);
                if (networkInfo2 != null && networkInfo2.isConnectedOrConnecting()) {
                    int i = 1;
                    this.mWifiActiveNetworks += networkInfo2.getType() == 1 ? 1 : 0;
                    int i2 = this.mLteActiveNetworks;
                    if (networkInfo2.getType() != 0) {
                        i = 0;
                    }
                    this.mLteActiveNetworks = i2 + i;
                }
            }
        }

        public boolean hasChanged(ConnectionState connectionState) {
            return hasChanged((ConnectionStateV21AndHigher) connectionState);
        }

        public boolean hasChanged(ConnectionStateV21AndHigher connectionStateV21AndHigher) {
            boolean z = false;
            if (this.mWifiActiveNetworks > 0 && connectionStateV21AndHigher.mWifiActiveNetworks > 0) {
                return false;
            }
            if (this.mWifiActiveNetworks != connectionStateV21AndHigher.mWifiActiveNetworks) {
                return true;
            }
            if (this.mLteActiveNetworks != connectionStateV21AndHigher.mLteActiveNetworks) {
                z = true;
            }
            return z;
        }

        public boolean isConnected() {
            return this.mWifiActiveNetworks > 0 || this.mLteActiveNetworks > 0;
        }

        public boolean isDisconnected() {
            return !isConnected();
        }
    }

    public abstract boolean hasChanged(ConnectionState connectionState);

    public abstract boolean isConnected();

    public abstract boolean isDisconnected();

    public static ConnectionState getInstance(ConnectivityManager connectivityManager) {
        if (VERSION.SDK_INT >= 21) {
            return new ConnectionStateV21AndHigher(connectivityManager);
        }
        return new ConnectionStateV20AndLower(connectivityManager);
    }
}
