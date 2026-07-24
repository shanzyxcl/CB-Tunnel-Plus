package com.cbtunnel.plus.view;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import android.graphics.*;
import androidx.annotation.*;
import androidx.core.internal.view.SupportMenu;
import androidx.core.view.ViewCompat;
import com.cbtunnel.plus.R;
import com.cbtunnel.plus.config.ConfigUtil;
import com.cbtunnel.plus.config.SettingsConstants;
import com.cbtunnel.plus.logger.hLogStatus;
import com.cbtunnel.plus.service.HarlieService;
import com.cbtunnel.plus.utils.RetrieveData;

public class GraphFragment extends Fragment implements SettingsConstants {

	private LineChart mChart;
	private Thread mGraphThread;
	private boolean isRunning = false;

	private StatisticGraphData.DataTransferStats mGraphStats;
	private DecimalFormat df;

	private ArrayList<Entry> e1;
	private ArrayList<Entry> e2;
	protected List<Long> dList;
	protected List<Long> uList;
	private ConfigUtil mConfig;

	public static final String color_graph_text = "#000000";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.graph, container, false);
		mConfig = ConfigUtil.getInstance(getActivity());
		mChart = (LineChart)v.findViewById(R.id.chart);
		mGraphStats = StatisticGraphData.getStatisticData().getDataTransferStats();
		try {
			setGraph();
			updateByteCount();
		} catch (Exception e) {}
		return v;
	}

	@SuppressLint("RestrictedApi")
	private void setGraph() {
		this.df = new DecimalFormat("#.##");
		this.dList = new ArrayList<>();
		this.uList = new ArrayList<>();
		setSpeed();
		this.e1 = new ArrayList<>();
		this.e2 = new ArrayList<>();
		float f = 0.0f;
		for (int i = 0; i < this.dList.size(); i++) {
			toString();
			float longValue = ((float) this.dList.get(i).longValue()) / 1024.0f;
			float longValue2 = ((float) this.uList.get(i).longValue()) / 1024.0f;
			float f2 = i;
			this.e1.add(new Entry(f2, longValue));
			this.e2.add(new Entry(f2, longValue2));
			if (f < longValue) {
				f = longValue;
			}
			if (f < longValue2) {
				f = longValue2;
			}
		}
		float f3 = f < 256.0f ? 512.0f : 1024.0f;
		LineDataSet lineDataSet = new LineDataSet(this.e1, "Download");
		lineDataSet.setLineWidth(0.0f);
		lineDataSet.setCircleRadius(0.0f);
		lineDataSet.setDrawValues(false);
		lineDataSet.setColor(-16711936);
		lineDataSet.setCircleColor(0);
		lineDataSet.setCircleColorHole(0);
		LineDataSet lineDataSet2 = new LineDataSet(this.e2, "Upload");
		lineDataSet2.setLineWidth(0.0f);
		lineDataSet2.setCircleRadius(0.0f);
		lineDataSet2.setDrawValues(false);
		lineDataSet2.setColor(SupportMenu.CATEGORY_MASK);
		lineDataSet2.setCircleColor(0);
		lineDataSet2.setCircleColorHole(0);
		ArrayList arrayList = new ArrayList();
		arrayList.add(lineDataSet2);
		arrayList.add(lineDataSet);
		StringBuilder sb = new StringBuilder();
		toString();
		sb.append(String.valueOf(this.df.format(f)));
		sb.append(" KB/s");
		LimitLine limitLine = new LimitLine(f, sb.toString());
		limitLine.setLineWidth(1.0f);
		limitLine.enableDashedLine(10.0f, 10.0f, 0.0f);
		limitLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
		limitLine.setTextSize(6.0f);
		limitLine.setTextColor(ViewCompat.MEASURED_STATE_MASK);
		limitLine.setTypeface(Typeface.DEFAULT);
		limitLine.setEnabled(false);
		XAxis xAxis = this.mChart.getXAxis();
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setAxisLineColor(Color.GREEN);
		this.mChart.getAxisLeft().setEnabled(false);
		this.mChart.getAxisRight().setEnabled(false);
		this.mChart.setData(new LineData(arrayList));
		this.mChart.setDrawGridBackground(true);
		this.mChart.setGridBackgroundColor(0);
		this.mChart.setTouchEnabled(false);
		this.mChart.getViewPortHandler().setMaximumScaleX(5.0f);
		this.mChart.getViewPortHandler().setMaximumScaleY(5.0f);
		this.mChart.getDescription().setEnabled(false);
		this.mChart.getLegend().setEnabled(false);
	}

	public void addDataSet() {
		float f;
		LineData lineData = (LineData) this.mChart.getData();
		toString();

		if (lineData != null) {
			this.dList = mGraphStats.getFastReceivedSeries();
			this.uList = mGraphStats.getFastSentSeries();
			this.e1 = new ArrayList<>();
			this.e2 = new ArrayList<>();
			setSpeed();
			float f2 = 0.0f;
			for (int i = 0; i < this.dList.size(); i++) {
				float longValue = ((float) this.dList.get(i).longValue()) / 1024.0f;
				float longValue2 = ((float) this.uList.get(i).longValue()) / 1024.0f;
				float f3 = i;
				this.e1.add(new Entry(f3, longValue));
				this.e2.add(new Entry(f3, longValue2));
				if (f2 < longValue) {
					f2 = longValue;
				}
				if (f2 < longValue2) {
					f2 = longValue2;
				}
			}
			float f4 = 2048.0f;
			String str = " KB/s";
			if (f2 <= 224.0f) {
				f = f2;
				f4 = 256.0f;
			} else {
				if (f2 <= 256.0f) {
					f4 = 512.0f;
				} else if (f2 <= 896.0f) {
					f = f2;
					f4 = 1024.0f;
				} else if (f2 >= 1024.0f) {
					if (f2 >= 1792.0f) {
						f4 = f2 < 3584.0f ? 4096.0f : f2 < 7168.0f ? 8192.0f : f2 < 14336.0f ? 16384.0f : 32768.0f;
					}
					f = f2 / 1024.0f;
					str = " MB/s";
				}
				f = f2;
			}
			LineDataSet lineDataSet = new LineDataSet(this.e2, "Download");
			lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
			lineDataSet.setCubicIntensity(0.2f);
			lineDataSet.setDrawFilled(true);
			lineDataSet.setDrawValues(false);
			lineDataSet.setFillColor(Color.parseColor("#ffcdd3"));
			lineDataSet.setFillAlpha(5000);
			lineDataSet.setLineWidth(0.5f);
			lineDataSet.setCircleRadius(0.5f);
			lineDataSet.setDrawValues(false);
			lineDataSet.setColor(Color.BLUE);
			lineDataSet.setCircleColor(0);
			lineDataSet.setCircleColorHole(0);
			lineDataSet.setDrawCircleHole(false);
			LineDataSet lineDataSet2 = new LineDataSet(this.e1, "Upload");
			lineDataSet2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
			lineDataSet2.setCubicIntensity(0.2f);
			lineDataSet2.setDrawFilled(true);
			lineDataSet2.setDrawValues(false);
			lineDataSet2.setFillColor(Color.parseColor("#ffcdd3"));
			lineDataSet2.setFillAlpha(5000);
			lineDataSet2.setLineWidth(0.5f);
			lineDataSet2.setCircleRadius(0.5f);
			lineDataSet2.setColor(Color.RED);
			lineDataSet2.setCircleColor(0);
			lineDataSet2.setCircleColorHole(0);
			lineDataSet2.setHighLightColor(Color.rgb(0, 102, 0));
			lineDataSet2.setDrawValues(false);
			lineDataSet2.setDrawCircleHole(false);
			StringBuilder sb = new StringBuilder();
			toString();
			sb.append(String.valueOf(this.df.format(f)));
			sb.append(str);
			LimitLine limitLine = new LimitLine(f2, sb.toString());
			limitLine.setLineWidth(1.0f);
			limitLine.enableDashedLine(10.0f, 10.0f, 0.0f);
			limitLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
			limitLine.setTextSize(6.0f);
			limitLine.setTextColor(Color.parseColor(color_graph_text));
			limitLine.setLineColor(0);
			limitLine.setTypeface(Typeface.DEFAULT);
			XAxis xAxis = this.mChart.getXAxis();
			xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
			xAxis.setDrawGridLines(true);
			xAxis.setDrawAxisLine(true);
			xAxis.setLabelCount(0, true);
			xAxis.setTextSize(6.0f);
			xAxis.setAxisMinValue(0.0f);
			xAxis.setDrawLabels(true);
			xAxis.setTypeface(Typeface.DEFAULT);
			xAxis.setTextColor(Color.parseColor(color_graph_text));
			xAxis.setEnabled(false);
			xAxis.enableGridDashedLine(5.0f, 5.0f, 1.0f);
			YAxis axisLeft = this.mChart.getAxisLeft();
			axisLeft.setLabelCount(0, true);
			axisLeft.setAxisMaxValue(f4);
			axisLeft.setAxisMinValue(0.0f);
			axisLeft.enableGridDashedLine(5.0f, 5.0f, 1.0f);
			axisLeft.removeAllLimitLines();
			axisLeft.addLimitLine(limitLine);
			axisLeft.setDrawLimitLinesBehindData(true);
			axisLeft.setTextColor(Color.parseColor(color_graph_text));
			axisLeft.setTextSize(6.0f);
			axisLeft.setEnabled(false);
			this.mChart.getAxisRight().setEnabled(true);
			YAxis axisRight = this.mChart.getAxisRight();
			axisRight.setLabelCount(10, true);
			axisRight.setAxisMaxValue(f4 / 1024.0f);
			axisRight.setAxisMinValue(0.0f);
			axisRight.enableGridDashedLine(5.0f, 5.0f, 1.0f);
			axisRight.setDrawGridLines(false);
			axisRight.setTextSize(6.0f);
			axisRight.setTextColor(Color.parseColor(color_graph_text));
			lineData.removeDataSet(0);
			lineData.removeDataSet(1);
			lineData.clearValues();
			lineData.addDataSet(lineDataSet2);
			lineData.addDataSet(lineDataSet);
			lineData.notifyDataChanged();
			Legend legend = this.mChart.getLegend();
			legend.setTextSize(6.0f);
			legend.setTypeface(Typeface.SERIF);
			legend.setTextColor(0);
			legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
			legend.setEnabled(false);
			this.mChart.getDescription().setEnabled(false);
			this.mChart.setData(lineData);
			this.mChart.getViewPortHandler().setMaximumScaleX(5.0f);
			this.mChart.getViewPortHandler().setMaximumScaleY(5.0f);
			this.mChart.notifyDataSetChanged();
			this.mChart.invalidate();
		}
	}

	public void setSpeed() {
		/*Long valueOf = Long.valueOf(StoredData.downloadSpeed);
		Long valueOf2 = Long.valueOf(StoredData.uploadSpeed);
		if (valueOf.longValue() < PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
			String str = valueOf + " B/s";
		} else if (valueOf.longValue() < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
			this.df.format(valueOf.longValue() / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);
		} else if (valueOf.longValue() >= PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
			DecimalFormat decimalFormat = this.df;
			double longValue = valueOf.longValue();
			Double.isNaN(longValue);
			decimalFormat.format(longValue / 1048576.0d);
		}
		if (valueOf2.longValue() < PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
			String str2 = valueOf2 + " B/s";
		} else if (valueOf2.longValue() < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
			this.df.format(valueOf2.longValue() / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);
		} else if (valueOf2.longValue() < PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED) {
		} else {
			DecimalFormat decimalFormat2 = this.df;
			double longValue2 = valueOf2.longValue();
			Double.isNaN(longValue2);
			decimalFormat2.format(longValue2 / 1048576.0d);
		}*/
	}

	private void updateByteCount()
	{
		isRunning = true;
		mGraphThread = new Thread(() -> {
            while (isRunning) {
                if (getActivity() == null) {
                    return;
                }
                List<Long> findData = RetrieveData.findData();
                Long l = findData.get(0);
                Long l2 = findData.get(1);
                l.longValue();
                l2.longValue();
                mGraphStats.addBytesReceived(l);
                mGraphStats.addBytesSent(l2);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(hLogStatus.isTunnelActive()|| mConfig.getServerType().equals(SERVER_TYPE_V2RAY)){
                            try {
                                addDataSet();
                            } catch (Exception e) {}
                        }
                        if (HarlieService.isVPNRunning()) {
                            try {
                                addDataSet();
                            } catch (Exception e) {}
                        }
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
            // TODO: Implement this method
        });
		mGraphThread.start();
	}
}