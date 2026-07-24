package com.cbtunnel.plus.utils;

import android.net.TrafficStats;
import java.util.ArrayList;
import java.util.List;

public class RetrieveData {

    static long totalUpload = 0;
    static long totalDownload = 0;
    static long totalUpload_n = 0;
    static long totalDownload_n = 0;

    public static List<Long> findData() {
        List<Long> allData = new ArrayList<>();

        long newTotalDownload, incDownload, newTotalUpload, incUpload;

        if (totalDownload == 0)
            totalDownload = TrafficStats.getTotalRxBytes();

        if (totalUpload == 0)
            totalUpload = TrafficStats.getTotalTxBytes();

        newTotalDownload = TrafficStats.getTotalRxBytes();
        incDownload = newTotalDownload - totalDownload;

        newTotalUpload = TrafficStats.getTotalTxBytes();
        incUpload = newTotalUpload - totalUpload;

        totalDownload = newTotalDownload;
        totalUpload = newTotalUpload;

        allData.add(incDownload);
        allData.add(incUpload);

        return allData;
    }

    public static long getNotificationData() {

        long newTotalDownload, incDownload, newTotalUpload, incUpload;

        if (totalDownload_n == 0)
            totalDownload_n = TrafficStats.getTotalRxBytes();

        if (totalUpload_n == 0)
            totalUpload_n = TrafficStats.getTotalTxBytes();

        newTotalDownload = TrafficStats.getTotalRxBytes();
        incDownload = newTotalDownload - totalDownload_n;

        newTotalUpload = TrafficStats.getTotalTxBytes();
        incUpload = newTotalUpload - totalUpload_n;

        totalDownload_n = newTotalDownload;
        totalUpload_n = newTotalUpload;

        return incDownload + incUpload;
    }
}

