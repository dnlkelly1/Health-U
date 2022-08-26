package com.csri.ami.health_u.ui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.analyze.graphing.TimeValueFormatter;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;


public class SensorFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{

    Intent backgroundServiceIntent = null;
    boolean isSensing = false;
    ///ui elements
    SwitchCompat sensorToggleSwitch;
    SharedPreferences preferences;

    PieChart pChart_day;
    PieChart pChart_hour;
    PieChart pChart_min;
    private Typeface mTf;
    ArrayList<Integer> colors=null;
    ArrayList<String> labels=null;
    ////////////////

    public SensorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        backgroundServiceIntent = new Intent(getActivity(), BackgroundService.class);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView,boolean isChecked)
    {

        SharedPreferences.Editor pref = getActivity().getSharedPreferences(BackgroundService.PREFS_NAME, Context.MODE_PRIVATE).edit();
        pref.putBoolean(BackgroundService.SENSOR_ON_PREF,isChecked);
        pref.apply();


        UpdateSensorByPreference();
    }


    public void InitSensor()
    {


        boolean isSensorRunning = isMyServiceRunning(getContext());

        if(!isSensorRunning)//if not running...check should it be running and turn it on////if is is running, then leave it running, it was probably turned on at boot, user should choose to turn it off again if they want
        {
            SharedPreferences prefs2 = getActivity().getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
            boolean accepted = prefs2.getBoolean(InformedConsent.PREFS_INFORMED_CONSENT_ACCEPTED, false);

            SharedPreferences prefs = getActivity().getSharedPreferences(BackgroundService.PREFS_NAME, Context.MODE_PRIVATE);
            boolean turnOnSensors = prefs.getBoolean(BackgroundService.SENSOR_ON_PREF, true);

            double battLevel = BatteryLevelReceiver.batteryLevel(getContext());

            if(turnOnSensors && accepted)// && battLevel > 0.2)
            {
                getActivity().startService(backgroundServiceIntent);
                getActivity().registerReceiver(broadcastReceiver, new IntentFilter(BackgroundService.BROADCAST_ACTION));

                EnableSensorDisplay();
                //SetSwitchCheck(true);
            }
            else
            {
                DisableSensorDisplay();
                //SetSwitchCheck(false);
            }
        }
        else
        {
            EnableSensorDisplay();
            try
            {
                getActivity().unregisterReceiver(broadcastReceiver);
            }
            catch(IllegalArgumentException ex){}

            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(BackgroundService.BROADCAST_ACTION));
            if(sensorToggleSwitch != null) {
                sensorToggleSwitch.setChecked(true);
            }

        }
    }

    private void UpdateSensorByPreference()
    {
        SharedPreferences prefs2 = getActivity().getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
        boolean accepted = prefs2.getBoolean(InformedConsent.PREFS_INFORMED_CONSENT_ACCEPTED, false);

        SharedPreferences prefs = getActivity().getSharedPreferences(BackgroundService.PREFS_NAME,Context.MODE_PRIVATE);
        boolean turnOnSensors = prefs.getBoolean(BackgroundService.SENSOR_ON_PREF, true);
        if(!turnOnSensors || !accepted)
        {
            if(isMyServiceRunning(getContext()))
            {
                try
                {
                    getActivity().unregisterReceiver(broadcastReceiver);
                }
                catch(IllegalArgumentException e)
                {

                }
                getActivity().stopService(backgroundServiceIntent);
                isSensing = false;
                DisableSensorDisplay();
            }

        }
        else//turn on
        {
            if(!isMyServiceRunning(getContext()))
            {

                getActivity().startService(backgroundServiceIntent);
                getActivity().registerReceiver(broadcastReceiver, new IntentFilter(BackgroundService.BROADCAST_ACTION));
                isSensing = true;
                EnableSensorDisplay();
            }

        }

    }

    @Override
    public void onPause()
    {
        super.onPause();



        if(isMyServiceRunning(getContext()))
        {
            try
            {
                getActivity().unregisterReceiver(broadcastReceiver);
            }
            catch(IllegalArgumentException e)
            {

            }

        }
    }

    public void SetSwitchCheck(boolean checked)
    {
        SwitchCompat s = (SwitchCompat)getActivity().findViewById(R.id.switch1);
        if(s != null)
        {
            s.setChecked(checked);
        }
    }


    public void EnableSensorDisplay()
    {
        LinearLayout l = (LinearLayout)getView().findViewById(R.id.sensorDataLayout);

        l.setVisibility(View.VISIBLE);

        LinearLayout warn = (LinearLayout)getView().findViewById(R.id.sensoroffwarning);
        warn.setVisibility(View.GONE);
    }

    public void DisableSensorDisplay()
    {
        LinearLayout l = (LinearLayout)getView().findViewById(R.id.sensorDataLayout);

        l.setVisibility(View.GONE);

        LinearLayout warn = (LinearLayout)getView().findViewById(R.id.sensoroffwarning);
        warn.setVisibility(View.VISIBLE);
    }

    @Override
    public  void onResume()
    {
        super.onResume();

        InitSensor();

        if(isMyServiceRunning(getContext()))
        {
            try
            {
               getActivity().unregisterReceiver(broadcastReceiver);
            }
            catch(IllegalArgumentException e)
            {

            }
            getActivity().registerReceiver(broadcastReceiver, new IntentFilter(BackgroundService.BROADCAST_ACTION));
        }



    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_sensor, container, false);

        RelativeLayout r = (RelativeLayout)v.findViewById(R.id.layout2);


        ////////////////////switch/////////////////////////

        sensorToggleSwitch = (SwitchCompat)getActivity().findViewById(R.id.switch1);

        SharedPreferences prefs = getActivity().getSharedPreferences(BackgroundService.PREFS_NAME,Context.MODE_PRIVATE);
        boolean turnOnSensors = prefs.getBoolean(BackgroundService.SENSOR_ON_PREF, true);

        sensorToggleSwitch.setChecked(turnOnSensors);

        sensorToggleSwitch.setOnCheckedChangeListener(this);
        ////////////////////////////////////////////////////////

        ///pie chart set up


        if(pChart_day == null) {
            pChart_day = InitMainPie(40f,52f);
        }
        if(pChart_hour == null) {
            pChart_hour = InitMainPie(55f,65f);
        }
        if(pChart_min == null)
        {
            pChart_min = InitMainPie(75f,80f);
        }



        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new  DisplayMetrics();
        display.getMetrics(metrics);

        int orientation = getActivity().getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            Legend l = pChart_day.getLegend();
            l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_CENTER);

            int graphHeight = (int)(metrics.widthPixels * 0.6);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(metrics.widthPixels, graphHeight );
            //lp.leftMargin = (metrics.widthPixels - graphWidth) /2;
            lp.topMargin = 0;
            //pChart_day.setExtraOffsets(graphWidth/20, 0,graphWidth/20 , 0);

            pChart_day.setLayoutParams(lp);

            int remainingHeight = metrics.heightPixels - graphHeight;

            double smallPieWeight = 0.4;
            int desiredSmallGraphHeight = (int)(metrics.widthPixels*smallPieWeight);
            if(desiredSmallGraphHeight > remainingHeight)
            {
                desiredSmallGraphHeight = (int)(remainingHeight * 0.9);
            }

            int heightLeftAfterSmallGraphs = remainingHeight - desiredSmallGraphHeight - 50;//-50 for sensor enable switch at bottom


            double remainingWidth = metrics.widthPixels - (desiredSmallGraphHeight * 2);

            int spacePerGraph = (int)(remainingWidth/2);

            RelativeLayout.LayoutParams lp_hour = new RelativeLayout.LayoutParams(desiredSmallGraphHeight, desiredSmallGraphHeight);
            lp_hour.topMargin = graphHeight ;
            lp_hour.leftMargin = spacePerGraph/2;
            pChart_hour.setLayoutParams(lp_hour);

            RelativeLayout.LayoutParams lp_min = new RelativeLayout.LayoutParams(desiredSmallGraphHeight, desiredSmallGraphHeight);
            lp_min.topMargin = graphHeight ;
            lp_min.leftMargin = (int)(metrics.widthPixels * (1-smallPieWeight)) - (spacePerGraph/2);
            pChart_min.setLayoutParams(lp_min);

        }
        else
        {
            Legend l = pChart_day.getLegend();
            l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_CENTER);
            l.setXOffset(20f);

            pChart_day.setLayoutParams(new RelativeLayout.LayoutParams(metrics.widthPixels / 2, metrics.heightPixels / 2));

            RelativeLayout.LayoutParams lp_hour = new RelativeLayout.LayoutParams((int)(metrics.widthPixels*0.25), (int)(metrics.widthPixels*0.25));
            lp_hour.leftMargin = (int)(metrics.widthPixels * 0.5);
            pChart_hour.setLayoutParams(lp_hour);

            RelativeLayout.LayoutParams lp_min = new RelativeLayout.LayoutParams((int)(metrics.widthPixels*0.25), (int)(metrics.widthPixels*0.25));
            lp_min.leftMargin = (int)(metrics.widthPixels * 0.75);
            pChart_min.setLayoutParams(lp_min);
        }

        Legend lh = pChart_hour.getLegend();
        lh.setEnabled(false);
        Legend lm = pChart_min.getLegend();
        lm.setEnabled(false);

        r.addView(pChart_day);
        r.addView(pChart_hour);
        r.addView(pChart_min);
        ////////////////////////

        return v;
    }

    private PieChart InitMainPie(float holeRadius,float transparentRadius)
    {
        labels = new ArrayList<String>();
        labels.add(getResources().getString(R.string.stationary));
        labels.add(getResources().getString(R.string.light));
        labels.add(getResources().getString(R.string.moderate));
        labels.add(getResources().getString(R.string.intense));

        colors = new ArrayList<Integer>();
        colors.add(Color.parseColor("#E0E0E0"));
        colors.add(Color.parseColor("#FFE082"));
        colors.add(Color.parseColor("#FFB300"));
        colors.add(Color.parseColor("#FF6F00"));


        PieChart pChart = new PieChart(getContext());
        pChart.setDescription("");

        pChart.setDragDecelerationFrictionCoef(0.95f);
        mTf = Typeface.DEFAULT;

        pChart.setCenterTextTypeface(mTf);
        pChart.setCenterText(generateCenterSpannableText(getResources().getString(R.string.day), getResources().getString(R.string.actSince), getResources().getString(R.string.today)));

        pChart.setDrawHoleEnabled(true);
        pChart.setHoleColorTransparent(true);

        pChart.setTransparentCircleColor(Color.WHITE);
        pChart.setTransparentCircleAlpha(110);

        pChart.setHoleRadius(holeRadius);

        pChart.setTransparentCircleRadius(transparentRadius);//3 bigger

        pChart.setDrawCenterText(true);

        pChart.setRotationAngle(-90);
        // enable rotation of the chart by touch
        pChart.setRotationEnabled(false);

        pChart.setDrawSliceText(false);

        pChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);


        return pChart;
    }

    private void setData(double[] values,PieChart chart) {



        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        for (int i = 0; i < values.length ; i++)
        {
            yVals1.add(new Entry((float)values[i], i));
        }

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < values.length; i++)
            xVals.add( labels.get(i));

        PieDataSet dataSet = new PieDataSet(yVals1, getResources().getString(R.string.actIntensity));
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);

        data.setValueFormatter(new TimeValueFormatter());

        data.setValueTextSize(11f);

        data.setValueTextColor(Color.parseColor("#8C000000"));
        data.setValueTypeface(mTf);


        chart.setData(data);

        // undo all highlights
        chart.highlightValues(null);

        chart.invalidate();
    }

    private SpannableString generateCenterSpannableText(String l1,String l2,String l3) {

        int change = l1.length();
        String l = l1 + "\n" + l2 + "\n" + l3;
        SpannableString s = new SpannableString(l);

        s.setSpan(new RelativeSizeSpan(1.7f), 0, l1.length(), 0);

        s.setSpan(new StyleSpan(Typeface.NORMAL), l1.length(), l1.length()+l2.length()+1, 0);
        s.setSpan(new ForegroundColorSpan(Color.GRAY), l1.length(), l1.length()+l2.length()+1, 0);
        s.setSpan(new RelativeSizeSpan(.8f), l1.length(), l1.length()+l2.length()+1, 0);

        s.setSpan(new StyleSpan(Typeface.ITALIC), l1.length()+l2.length()+1, l.length(), 0);
        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), l1.length()+l2.length()+1, l.length(), 0);
        return s;
    }

    private SpannableString generateCenterSpannableText() {

        int change = 8;
        SpannableString s = new SpannableString("Activity \nToday");
        s.setSpan(new RelativeSizeSpan(1.0f), 0, change, 0);

        s.setSpan(new StyleSpan(Typeface.NORMAL), change, s.length() , 0);
        s.setSpan(new ForegroundColorSpan(Color.GRAY), change, s.length() , 0);
        s.setSpan(new RelativeSizeSpan(1.4f), change, s.length(), 0);

        s.setSpan(new StyleSpan(Typeface.ITALIC), change, s.length(), 0);
        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), change, s.length(), 0);
        return s;
    }

    public static boolean isMyServiceRunning(Context context)
    {

        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.csri.ami.health_u.ui.BackgroundService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateUI(intent);
        }
    };


    public void UpdateUI(Intent intent)
    {
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            String[] data = extras.getStringArray(BackgroundService.DIARY_DATA);
            double[] values_day = extras.getDoubleArray(BackgroundService.VAR_BANDS_DAY_DATA);
            double[] values_hour = extras.getDoubleArray(BackgroundService.VAR_BANDS_HOUR_DATA);
            double[] values_min = extras.getDoubleArray(BackgroundService.VAR_BANDS_WINDOW_DATA);

            String hourStart = extras.getString(BackgroundService.VAR_BANDS_HOUR_START);
            String windowStart = extras.getString(BackgroundService.VAR_BANDS_WINDOW_START);

            //////////////////////////////////////////gps info////////////////////////////////////////
            double gps = (int)extras.getDouble(BackgroundService.GPS_DISTANCE);
            int kms = (int)Math.round(gps / 1000);
            String gps_String = Integer.toString(kms);

            //SpannableString gpsHeading = generateSpannableTextHeading(getResources().getString(R.string.locationAct));
            SpannableString s1_gps = generateSpannableText(gps_String, getResources().getString(R.string.metersToday));

            //Spanned message1Gps = (Spanned) TextUtils.concat( "\n", s1_gps);
            SpannableString messageGps = new SpannableString(s1_gps);

            TextView gpsText = (TextView)getView().findViewById(R.id.gpsDistance);
            gpsText.setText(messageGps);
            /////////////////////////////////////////////////////////////////////////////////////////

            //////////////////////////////////////////sound info///////////////////////////////////////
            int sound_recent= (int)extras.getDouble(BackgroundService.SOUND_MOSTRECENT);
            String sound_String = Integer.toString(sound_recent) + "%";
            int sound_total = (int)extras.getDouble(BackgroundService.SOUND_MOSTOTOAL);
            String soundTotal_String = Integer.toString(sound_total) + "%";

           // SpannableString heading = generateSpannableTextHeading(getResources().getString(R.string.soundAct));
            SpannableString s1 = generateSpannableText(sound_String, getResources().getString(R.string.mostRecent));
            SpannableString s2 = generateSpannableText(soundTotal_String,getResources().getString(R.string.todayAvg));

            Spanned message1 = (Spanned) TextUtils.concat(s1, "\n",s2);

            SpannableString message = new SpannableString(message1);

            TextView soundText = (TextView)getView().findViewById(R.id.soundAvg);
            soundText.setText(message);
            ////////////////////////////////////////////////////////////////////////




            if(values_day != null)
            {
                setData(values_day,pChart_day);

            }
            if(values_hour != null)
            {
                setData(values_hour,pChart_hour);
                SpannableString s = generateCenterSpannableText(getResources().getString(R.string.hour),getResources().getString(R.string.actSince), hourStart);
                pChart_hour.setCenterText(s);
            }
            if(values_min != null)
            {
                setData(values_min,pChart_min);
                SpannableString s = generateCenterSpannableText(getResources().getString(R.string.minute),getResources().getString(R.string.actSince), windowStart);
                pChart_min.setCenterText(s);
            }

        }
    }

    private SpannableString generateSpannableText(String l1,String l2) {

        int change = l1.length();
        String l = l1 + "-" + l2;
        SpannableString s = new SpannableString(l);

       // Typeface digitalFont = Typeface.createFromAsset(getContext().getAssets(),"digital-7.ttf");
        int textColor = Color.parseColor("#B3FFFFFF");

        int measureColor = ContextCompat.getColor(this.getContext(), R.color.colorAccent);

        s.setSpan(new ForegroundColorSpan(measureColor), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(0.8f), 0, l1.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, l1.length(), 0);


        s.setSpan(new ForegroundColorSpan(textColor), l1.length(), l.length(), 0);
        s.setSpan(new RelativeSizeSpan(0.6f), l1.length(), l.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD),l1.length(), l.length(),0);

        return s;
    }

    private SpannableString generateSpannableTextHeading(String l1) {

        int change = l1.length();
        String l = l1 ;
        SpannableString s = new SpannableString(l);

        // Typeface digitalFont = Typeface.createFromAsset(getContext().getAssets(),"digital-7.ttf");


        int textColor = ContextCompat.getColor(this.getContext(), R.color.textColorPrimary);

        s.setSpan(new ForegroundColorSpan(textColor), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(0.8f), 0, l1.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, l1.length(), 0);

        return s;
    }

}
