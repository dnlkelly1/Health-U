package com.csri.ami.health_u.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.analyze.graphing.GraphValueDay;
import com.csri.ami.health_u.dataManagement.analyze.graphing.GraphValueWeek;
import com.csri.ami.health_u.dataManagement.analyze.graphing.ListViewBarItem;
import com.csri.ami.health_u.dataManagement.analyze.graphing.ListViewDataItem;
import com.csri.ami.health_u.dataManagement.analyze.graphing.StoredDataManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.List;


public class HistoryFragment extends Fragment
{
    IconTextTabsActivity parent;
    StoredDataManager storedData;
    boolean[] visibleData_days;
    boolean[] visibleData_weeks;

    LineData[] linedata_days;
    BarData[] bardata_weeks;

    ListView lv;
    ListView lv2;

    WeekChartDataAdapter wda;
    ChartDataAdapter cda;



    Animation slide_in_left;
    Animation slide_out_right;
    ViewSwitcher viewSwitcher;




    public boolean dayIsCurrentView = true;

    public HistoryFragment()
    {
        // Required empty public constructor
    }
    private static String FRAGMENT_TAG = "fragmenttag";
    Fragment fragmentTask;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);


        Utils.init(getContext());






    }

    @Override
    public void onViewStateRestored(Bundle b)
    {
        super.onViewStateRestored(b);

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_histroy, container, false);


        viewSwitcher = (ViewSwitcher) v.findViewById(R.id.viewswitcher);

        Button b_day = (Button) v.findViewById(R.id.buttonHistroyDay);
        Button b_week = (Button) v.findViewById(R.id.buttonHistroyWeek);

        b_day.setTextColor(ContextCompat.getColor(getContext(),R.color.colorPrimary));
        b_week.setTextColor(ContextCompat.getColor(getContext(),R.color.white));



        dayIsCurrentView = true;



        Button b = (Button)v.findViewById(R.id.buttonHistroyDay);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //DayButtonClicked();
                Button b_day = (Button) getView().findViewById(R.id.buttonHistroyDay);
                Button b_week = (Button) getView().findViewById(R.id.buttonHistroyWeek);
                if (!dayIsCurrentView)//if day is click...and currently isnt set to day...then make current view day
                {
                    if (viewSwitcher != null) {
                        viewSwitcher.showNext();

                        b_day.setEnabled(false);
                        b_week.setEnabled(true);

                        b_day.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                        b_week.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                        //b_day.setChecked(true);

                        //b_week.setChecked(false);



                        dayIsCurrentView = true;
                    }
                }
                else
                {


                }
            }
        });

        Button b1 = (Button)v.findViewById(R.id.buttonHistroyWeek);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //DayButtonClicked();



                Button b_day = (Button) getView().findViewById(R.id.buttonHistroyDay);
                Button b_week = (Button) getView().findViewById(R.id.buttonHistroyWeek);

                if (dayIsCurrentView)//if week is click...and currently is set to day...then make current view week
                {
                    if (viewSwitcher != null)
                    {
                        viewSwitcher.showNext();


                        b_day.setEnabled(true);
                        b_week.setEnabled(false);
                        b_day.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                        b_week.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));


                        dayIsCurrentView = false;
                    }
                }

            }
        });

        progress = (ProgressBar)v.findViewById(R.id.spinner1);


        return v;
    }

    private void LoadListViews_Asycn()
    {
        new AsyncTask<Void, Void, Void>() {

            protected void onPreExecute()
            {
                try {
                    lv = (ListView) getActivity().findViewById(R.id.list);
                    lv2 = (ListView) getActivity().findViewById(R.id.list_week);

                    int x =5;
                    int y =x+1;
                }
                catch (Exception ex)
                {
                    int x=1;
                    int y = x+1;
                }
            }

            @Override
            protected Void doInBackground(Void... x) {

                LoadListViews_Init();



                return null;
            }

            protected void onPostExecute(Void res)
            {
                LoadListViews_PostInit();
            }

        }.execute();
    }

    private void LoadListViews_PostInit()
    {
        if(cda == null && wda == null)
        {
            if(getView() != null)
            {
                TextView tv = (TextView) getView().findViewById(R.id.emptyText1);
                tv.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
            }
        }
        else
        {
            if (lv != null) {
                lv.setAdapter(cda);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                        ViewHolder current = (ViewHolder) view.getTag();


                        if (!visibleData_days[position]) {

                            visibleData_days[position] = true;
                            current.chart.setVisibility(View.VISIBLE);

                            current.isShowingChart = true;

                            Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_remove_circle_outline_white_18dp);

                            img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
                            current.text.setCompoundDrawables(img, null, null, null);
                            LoadViewGraph(current, position);
                            //current.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_remove_circle_outline_white_18dp,0,0,0);
                        } else {
                            visibleData_days[position] = false;
                            current.chart.setVisibility(View.GONE);
                            current.isShowingChart = false;

                            Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_add_circle_outline_white_18dp);
                            img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
                            current.text.setCompoundDrawables(img, null, null, null);
                            //current.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add_circle_outline_white_18dp,0,0,0);
                        }


                    }
                });

                lv.invalidate();
            }
            if (lv2 != null) {
                lv2.setAdapter(wda);
                lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                        ViewHolder current = (ViewHolder) view.getTag();


                        if (!visibleData_weeks[position]) {
                            visibleData_weeks[position] = true;
                            current.barchart.setVisibility(View.VISIBLE);
                            current.isShowingChart = true;

                            Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_remove_circle_outline_white_18dp);

                            img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
                            current.text.setCompoundDrawables(img, null, null, null);
                            LoadViewBarGraph(current, position);
                            //current.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_remove_circle_outline_white_18dp,0,0,0);
                        } else {
                            visibleData_weeks[position] = false;
                            current.barchart.setVisibility(View.GONE);
                            current.isShowingChart = false;

                            Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_add_circle_outline_white_18dp);
                            img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
                            current.text.setCompoundDrawables(img, null, null, null);
                            //current.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add_circle_outline_white_18dp,0,0,0);
                        }


                    }
                });
                lv2.invalidate();
            }

            progress.setVisibility(View.GONE);
            viewSwitcher.setVisibility(View.VISIBLE);
        }

    }

    private void LoadListViews_Init()
    {
        //lv = (ListView) getActivity().findViewById(R.id.list);
        //lv2 = (ListView) getActivity().findViewById(R.id.list_week);

        ArrayList<ListViewDataItem> list = new ArrayList<ListViewDataItem>();
        ArrayList<ListViewBarItem> barlist = new ArrayList<ListViewBarItem>();

        if(storedData != null && storedData.days != null)
        {
            ///////////////////////////////////weekly stuff/////////////////////////////////


            for(int i=0;i<storedData.weeks.length;i++)
            {
                BarData bd = generateBarData(storedData.weeks[i]);
                String text = storedData.weeks[i].startdate.toString("dd/MM/yyyy") + " - " + storedData.weeks[i].enddate.toString("dd/MM/yyyy");
                barlist.add(new ListViewBarItem(bd,text,storedData.weeks[i].motion_weekScore,storedData.weeks[i].sound_weekScore,storedData.weeks[i].location_weekScore));
            }

            try {
                wda = new WeekChartDataAdapter(this.getContext(), barlist);
            }
            catch (Exception ex){}


            //////////////////////////////end weekly stuff////////////////////////////////////

            ///////////////////////////daily stuff//////////////////////////////////////
            for (int i = 0; i < storedData.days.length; i++)
            {
                LineData ld = generateDataLine(storedData.days[i]);
                String text = storedData.days[i].date.dayOfWeek().getAsText() + " - " + storedData.days[i].date.toString("dd/MM/yyyy") + ":";
                list.add(new ListViewDataItem(ld,text,storedData.days[i].motion_dayScore,storedData.days[i].sound_dayScore,storedData.days[i].location_dayScore));
            }

            try {
                cda = new ChartDataAdapter(this.getContext(), list);
            }
            catch (Exception ex){}


            //////////////////////////daily stuff end//////////////////////////////////////

        }

    }

    ProgressBar progress;

    public void Init(boolean forceDataLoad)
    {
        Activity p = getActivity();
        if(p instanceof IconTextTabsActivity)
        {
            parent =(IconTextTabsActivity)p;
        }

        progress.setVisibility(View.VISIBLE);
        viewSwitcher.setVisibility(View.GONE);

        boolean doReload = false;
        if(parent.storedDataManager == null)
        {
            doReload = true;
        }
        else if(Days.daysBetween(parent.storedDataManager.days[0].date.withTimeAtStartOfDay(),DateTime.now().withTimeAtStartOfDay()).getDays() > 1)
        {
            doReload = true;
        }



        if(doReload || forceDataLoad) {
            new AsyncTask<Void, Void, Void>()
            {
                protected void onPreExecute(Void res)
                {

                }

                @Override
                protected Void doInBackground(Void... x)
                {
                    boolean newDay = false;
                    if(storedData != null)
                    {
                        if(storedData.days != null) {
                            if (storedData.days.length > 0)
                                newDay = Days.daysBetween(storedData.days[0].date.withTimeAtStartOfDay(), DateTime.now().withTimeAtStartOfDay()).getDays() != 0;
                        }
                    }
                    if (storedData == null || newDay) {

                        storedData = new StoredDataManager();
                        parent.storedDataManager = storedData;
                        if(storedData.days != null) {
                            visibleData_days = new boolean[storedData.days.length];
                            for (int i = 0; i < visibleData_days.length; i++) {
                                visibleData_days[i] = false;
                            }
                            linedata_days = new LineData[visibleData_days.length];
                        }
                        if(storedData.weeks != null) {
                            visibleData_weeks = new boolean[storedData.weeks.length];
                            for (int i = 0; i < visibleData_weeks.length; i++) {
                                visibleData_weeks[i] = false;
                            }
                            bardata_weeks = new BarData[visibleData_weeks.length];

                        }
                    }

                    return null;
                }

                protected void onPostExecute(Void res) {
                    LoadListViews_Asycn();
                }

            }.execute();
        }
        else
        {
            LoadListViews_Asycn();
        }


    }
//
    @Override
    public void onResume()
    {
        super.onResume();

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {

        super.onActivityCreated(savedInstanceState);

        viewSwitcher = (ViewSwitcher) getView().findViewById(R.id.viewswitcher);

        Init(false);

    }

    private class ChartDataAdapter extends ArrayAdapter<ListViewDataItem> {

        private Typeface mTf;


        public ChartDataAdapter(Context context, List<ListViewDataItem> objects) {
            super(context, 0, objects);



            mTf = Typeface.DEFAULT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            ListViewDataItem item = getItem(position);
            LineData data = item.lineData;

            linedata_days[position] = data;

            ViewHolder holder = null;

            if (convertView == null)
            {

                holder = new ViewHolder();

                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.list_item_linechart, null);
                holder.chart = (LineChart) convertView.findViewById(R.id.chart);


                holder.text = (TextView) convertView.findViewById(R.id.date);

                holder.movement_summary_text = (TextView) convertView.findViewById(R.id.movement_summary_m);
                holder.sound_summary_text = (TextView) convertView.findViewById(R.id.sound_summary_m);
                holder.location_summary_text = (TextView) convertView.findViewById(R.id.location_summary_m);
                holder.img_minus = ContextCompat.getDrawable(getContext(), R.drawable.ic_remove_circle_outline_white_18dp);
                holder.img_minus.setBounds(0,0,holder.img_minus.getIntrinsicWidth(),holder.img_minus.getIntrinsicHeight());
                holder.img_plus = ContextCompat.getDrawable(getContext(), R.drawable.ic_add_circle_outline_white_18dp);
                holder.img_plus.setBounds(0,0,holder.img_plus.getIntrinsicWidth(),holder.img_plus.getIntrinsicHeight());


                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }



            holder.text.setText(item.text);

            String timeMove = getTimeStringFromMins(item.movement_summary);
            SpannableString movementString = generateDigitalText_Time(timeMove);// generateCenterSpannableText(getResources().getString(R.string.movement), timeMove, COLOR_motion,1.0f);
            holder.movement_summary_text.setText(movementString);

            String timeSound = getTimeStringFromMins(item.sound_summary);
            SpannableString soundString = generateDigitalText_Time(timeSound);// generateCenterSpannableText(getResources().getString(R.string.socialSound), timeSound, COLOR_sound,0.8f);
            holder.sound_summary_text.setText(soundString);

            String locationM = Integer.toString((int)item.location_summary) + "%";
            SpannableString locationString = generateDigitalText(locationM);// generateCenterSpannableText(getResources().getString(R.string.travelReg), locationM, COLOT_location, 0.8f);
            holder.location_summary_text.setText(locationString);




            if(visibleData_days[position])
            {
                // apply styling
                //////////////////////////////////////
                holder.chart.setVisibility(View.VISIBLE);
                Drawable img = holder.img_minus;

                holder.text.setCompoundDrawables(img, null, null, null);
                //current.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_remove_circle_outline_white_18dp,0,0,0);/
                //////////////////////////////////////////
                LoadViewGraph(holder,position);

            }
            else
            {
                holder.chart.setVisibility(View.GONE);

                Drawable img = holder.img_plus;
                holder.text.setCompoundDrawables(img, null, null, null);

            }

            return convertView;
        }


    }

    private void LoadViewGraph(ViewHolder holder,int position)
    {
        float minUpperRangeOfXAxis = 100;
        float maxVal = (float)Math.max(minUpperRangeOfXAxis,storedData.MaxHourValue);

        Typeface mTf = Typeface.DEFAULT;
        linedata_days[position].setValueTypeface(mTf);
        linedata_days[position].setValueTextColor(Color.BLACK);
        holder.chart.setDescription("");
        holder.chart.setDrawGridBackground(false);




        XAxis xAxis = holder.chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(mTf);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = holder.chart.getAxisLeft();
        leftAxis.setTypeface(mTf);
        leftAxis.setLabelCount(5, false);
        leftAxis.setSpaceTop(5f);
        leftAxis.setAxisMaxValue(maxVal);
        leftAxis.setDrawGridLines(false);
        leftAxis.setEnabled(false);

        YAxis rightAxis = holder.chart.getAxisRight();
        rightAxis.setTypeface(mTf);
        rightAxis.setLabelCount(5, false);
        rightAxis.setSpaceTop(5f);
        rightAxis.setAxisMaxValue(maxVal);
        rightAxis.setDrawGridLines(false);
        rightAxis.setEnabled(false);

        // set data
        holder.chart.setData(linedata_days[position]);

        Legend lh = holder.chart.getLegend();
        lh.setEnabled(false);

        holder.chart.refreshDrawableState();
        holder.chart.invalidate();
        //holder.chart.animateX(1200, Easing.EasingOption.EaseInCubic);
    }

    private class WeekChartDataAdapter extends ArrayAdapter<ListViewBarItem> {

        private Typeface mTf;


        public WeekChartDataAdapter(Context context, List<ListViewBarItem> objects)
        {


           super(context, 0, objects);




            mTf = Typeface.DEFAULT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            ListViewBarItem item = getItem(position);
            BarData data = item.lineData;
            bardata_weeks[position] = data;

            ViewHolder holder = null;

            if (convertView == null) {

                holder = new ViewHolder();

                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.list_item_barchart, null);
                holder.barchart = (BarChart) convertView.findViewById(R.id.chart);


                holder.text = (TextView) convertView.findViewById(R.id.date);

                holder.movement_summary_text = (TextView) convertView.findViewById(R.id.movement_summary_m);
                holder.sound_summary_text = (TextView) convertView.findViewById(R.id.sound_summary_m);
                holder.location_summary_text = (TextView) convertView.findViewById(R.id.location_summary_m);
                holder.img_minus = ContextCompat.getDrawable(getContext(), R.drawable.ic_remove_circle_outline_white_18dp);
                holder.img_minus.setBounds(0,0,holder.img_minus.getIntrinsicWidth(),holder.img_minus.getIntrinsicHeight());
                holder.img_plus = ContextCompat.getDrawable(getContext(), R.drawable.ic_add_circle_outline_white_18dp);
                holder.img_plus.setBounds(0,0,holder.img_plus.getIntrinsicWidth(),holder.img_plus.getIntrinsicHeight());


                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // apply styling

            holder.text.setText(item.text);

            String timeMove = getTimeStringFromMins(item.movement_summary);
            SpannableString movementString = generateDigitalText_Time(timeMove);// generateCenterSpannableText(getResources().getString(R.string.movement), timeMove, COLOR_motion,1.0f);
            holder.movement_summary_text.setText(movementString);

            String timeSound = getTimeStringFromMins(item.sound_summary);
            SpannableString soundString = generateDigitalText_Time(timeSound);// generateCenterSpannableText(getResources().getString(R.string.socialSound), timeSound, COLOR_sound,0.8f);
            holder.sound_summary_text.setText(soundString);

            String locationM = Integer.toString((int)item.location_summary) + "%";
            SpannableString locationString = generateDigitalText(locationM);// generateCenterSpannableText(getResources().getString(R.string.travelReg), locationM, COLOT_location, 0.8f);
            holder.location_summary_text.setText(locationString);



            if(visibleData_weeks[position]) {

                holder.barchart.setVisibility(View.VISIBLE);

                Drawable img = holder.img_minus;

                holder.text.setCompoundDrawables(img, null, null, null);
                LoadViewBarGraph(holder,position);

            } else {
                holder.barchart.setVisibility(View.GONE);


                Drawable img = holder.img_plus;
                holder.text.setCompoundDrawables(img, null, null, null);

            }


            return convertView;
        }


    }

    private void LoadViewBarGraph(ViewHolder holder,int position)
    {
        double maxValue = Math.max(100,storedData.MaxWeekDayValue);

        Typeface mTf = Typeface.DEFAULT;
        bardata_weeks[position].setValueTypeface(mTf);
        bardata_weeks[position].setValueTextColor(Color.BLACK);
        holder.barchart.setDescription("");
        holder.barchart.setDrawGridBackground(false);
        holder.barchart.setDrawValueAboveBar(false);



        XAxis xAxis = holder.barchart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(mTf);
        xAxis.setDrawGridLines(false);


        YAxis leftAxis = holder.barchart.getAxisLeft();
        leftAxis.setTypeface(mTf);
        leftAxis.setLabelCount(5, false);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMaxValue((int)maxValue);
        leftAxis.setDrawGridLines(false);
        leftAxis.setEnabled(false);

        YAxis rightAxis = holder.barchart.getAxisRight();
        rightAxis.setTypeface(mTf);
        rightAxis.setLabelCount(5, false);
        rightAxis.setSpaceTop(15f);
        rightAxis.setAxisMaxValue((int)maxValue);
        rightAxis.setDrawGridLines(false);
        rightAxis.setEnabled(false);

        // set data
        holder.barchart.setData(bardata_weeks[position]);

        Legend lh = holder.barchart.getLegend();
        lh.setEnabled(false);

        holder.barchart.refreshDrawableState();
        holder.barchart.invalidate();
        //holder.barchart.animateX(1200, Easing.EasingOption.EaseInCubic);

    }

    private class ViewHolder {

        LineChart chart;
        BarChart barchart;
        TextView text;
        TextView movement_summary_text;
        TextView sound_summary_text;
        TextView location_summary_text;
        Drawable img_plus;
        Drawable img_minus;


        boolean isShowingChart = false;
    }

    private BarData generateBarData(GraphValueWeek values)
    {
        ArrayList<BarEntry> set = new ArrayList<BarEntry>();

        float minsPerDay_active = (60 * 16);

        for(int i=0;i<values.dayCount;i++)
        {
            float motionValue = ((float)values.motion[i].graph_value);// / minsPerDay_active) * 100;
            float soundValue = ((float)values.sound[i].graph_value);// / minsPerDay_active) * 100;
            float locationValue = (float)values.location[i].graph_value;

            set.add(new BarEntry(new float[]{locationValue,soundValue,motionValue},values.motion[i].timeSlotIndex-1));
        }

        BarDataSet setWeek = new BarDataSet(set, "Weekly Data");

        int COLOR_motion_fill = ContextCompat.getColor(this.getContext(), R.color.motiongraphfill);//Color.parseColor("#4DB6AC");// Color.rgb(128, 147, 173);

        int COLOR_sound_fill = ContextCompat.getColor(this.getContext(), R.color.soundgraphfill);//Color.parseColor("#81C784");// Color.rgb(121, 147, 126);

        int COLOR_location_fill= ContextCompat.getColor(this.getContext(), R.color.locationgraphfill);//Color.parseColor("#E57373");// Color.rgb(204, 37, 41);
        setWeek.setColors(new int[]{COLOR_location_fill,COLOR_sound_fill,COLOR_motion_fill});

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(setWeek);

        BarData data = new BarData(getDays(), dataSets);
        data.setDrawValues(false);

        return data;
    }

    private Entry[] FillInData(Entry start,Entry end,int numPoints)
    {
        float valueDiff = end.getVal() - start.getVal();

        float valueInc = valueDiff / (float)(numPoints + 1);

        Entry[] points = new Entry[numPoints];

        for(int i=1;i<=points.length;i++)
        {
            points[i-1] = new Entry(start.getVal() + (valueInc * i),start.getXIndex() + i);
        }

        return points;
    }

    private ArrayList<Entry> Interpolate(ArrayList<Entry> data)
    {

        if(data.size() > 0)
        {

            ArrayList<Entry> newData = new ArrayList<Entry>();
            int firstIndex = data.get(0).getXIndex();
            int endIndex = data.get(data.size()-1).getXIndex();

            int numEnd = 23 - endIndex;

            for(int i=0;i<firstIndex;i++)
            {
                Entry c = new Entry(0,i);
                newData.add(c);
            }

            newData.add(data.get(0));

            Entry previous = data.get(0);

            for (int i = 1; i < data.size(); i++) {
                Entry current = data.get(i);

                int diff = current.getXIndex() - previous.getXIndex();

                if (diff > 1) {
                    Entry[] newPoints = FillInData(previous, current, diff - 1);

                    for (int j = 0; j < newPoints.length; j++) {
                        newData.add(newPoints[j]);
                    }
                    newData.add(current);

                    previous = current;
                }
                else if(diff == 1)
                {
                    newData.add(current);

                    previous = current;
                }
            }

            for(int i=0;i<numEnd;i++)
            {
                Entry c = new Entry(0, endIndex +1 + i);
                newData.add(c);
            }

            return newData;
        }
        else
        {
            return data;
        }
    }

    private LineData generateDataLine(GraphValueDay values)
    {
        ArrayList<LineDataSet> sets = new ArrayList<LineDataSet>();
        int COLOR_motion = ContextCompat.getColor(this.getContext(), R.color.motiongraph);// Color.parseColor("#009688");// Color.rgb(57, 106, 177);
        int COLOR_motion_fill = ContextCompat.getColor(this.getContext(), R.color.motiongraphfill);//Color.parseColor("#4DB6AC");// Color.rgb(128, 147, 173);
        int COLOR_sound = ContextCompat.getColor(this.getContext(), R.color.soundgraph);//Color.parseColor("#4CAF50");// Color.rgb(62, 150, 81);
        int COLOR_sound_fill = ContextCompat.getColor(this.getContext(), R.color.soundgraphfill);//Color.parseColor("#81C784");// Color.rgb(121, 147, 126);
        int COLOT_location= ContextCompat.getColor(this.getContext(), R.color.locationgraph);//Color.parseColor("#F44336") ;//Color.rgb(204, 37, 41);
        int COLOR_location_fill= ContextCompat.getColor(this.getContext(), R.color.locationgraphfill);//Color.parseColor("#E57373");// Color.rgb(204, 37, 41);

        float linewidth = 1.5f;
        float circleSize = 2.6f;

        LineDataSet locationEntries = null;
        if(values.location != null)
        {
            ArrayList<Entry> e3 = new ArrayList<Entry>();

            for (int i = 0; i < values.location.length; i++)
            {
               e3.add(new Entry((int) (values.location[i].graph_value), values.location[i].timeSlotIndex));
            }

            ArrayList<Entry> e3_filled = Interpolate(e3);

            locationEntries = new LineDataSet(e3_filled, "Location Unpredictability");
            locationEntries.setFillColor(COLOR_location_fill);
            locationEntries.setDrawFilled(true);
            locationEntries.setFillAlpha(255);
            locationEntries.setLineWidth(linewidth);

            locationEntries.setDrawCircles(false);
            locationEntries.setHighLightColor(COLOT_location);
            locationEntries.setColor(COLOT_location);
            locationEntries.setCircleColor(COLOT_location);
            locationEntries.setCircleColorHole(COLOT_location);
            locationEntries.setDrawValues(false);

            locationEntries.setDrawCubic(true);
            locationEntries.setCubicIntensity(0.1f);


        }


        LineDataSet soundEntries=null;
        if(values.sound != null)
        {
            ArrayList<Entry> e2 = new ArrayList<Entry>();

            for (int i = 0; i < values.sound.length; i++)
            {
                if(i > 0)
                {
                    if(values.sound[i].timeSlotIndex - values.sound[i-1].timeSlotIndex > 6)
                    {


                        e2.add(new Entry(0, values.sound[i].timeSlotIndex-1));
                    }
                }

                double lValue=0;
                if(locationEntries != null) {
                    lValue = locationEntries.getYValForXIndex(values.sound[i].timeSlotIndex);
                    if (Double.isNaN(lValue)) {
                        lValue = 0;
                    }
                }


                e2.add(new Entry((int) (lValue + (values.sound[i].graph_value)), values.sound[i].timeSlotIndex));
            }

            ArrayList<Entry> e2_filled = Interpolate(e2);

            soundEntries = new LineDataSet(e2_filled, "Percentage Voice");

            soundEntries.setFillColor(COLOR_sound_fill);
            soundEntries.setDrawFilled(true);
            soundEntries.setFillAlpha(255);
            soundEntries.setLineWidth(linewidth);
            //d2.setCircleSize(circleSize);
            soundEntries.setDrawCircles(false);
            soundEntries.setHighLightColor(COLOR_sound);
            soundEntries.setColor(COLOR_sound);
            soundEntries.setCircleColor(COLOR_sound);
            soundEntries.setCircleColorHole(COLOR_sound);
            soundEntries.setDrawValues(false);

            soundEntries.setDrawCubic(true);
            soundEntries.setCubicIntensity(0.1f);

            //sets.add(d2);
        }


        LineDataSet motionEntries=null;
        if(values.motion != null)
        {
            ArrayList<Entry> e1 = new ArrayList<Entry>();

            for (int i = 0; i < values.motion.length; i++)
            {
                if(i > 0)
                {
                    if(values.motion[i].timeSlotIndex - values.motion[i-1].timeSlotIndex > 6)
                    {
                        e1.add(new Entry(0, values.motion[i].timeSlotIndex-1));
                    }
                }

                double slValue=0;
                if(locationEntries != null) {
                    slValue = soundEntries.getYValForXIndex(values.motion[i].timeSlotIndex);
                    if (Double.isNaN(slValue)) {
                        slValue = 0;
                    }
                }


                e1.add(new Entry((int) (slValue + values.motion[i].graph_value ), values.motion[i].timeSlotIndex));
            }

            ArrayList<Entry> e1_filled = Interpolate(e1);



            motionEntries = new LineDataSet(e1_filled , "Percentage Movement");


            motionEntries.setFillColor(COLOR_motion_fill);
            motionEntries.setDrawFilled(true);
            motionEntries.setFillAlpha(255);
            motionEntries.setLineWidth(linewidth);

            motionEntries.setDrawCircles(false);
            motionEntries.setHighLightColor(COLOR_motion);
            motionEntries.setColor(COLOR_motion);
            motionEntries.setCircleColor(COLOR_motion);
            motionEntries.setCircleColorHole(COLOR_motion);
            motionEntries.setDrawValues(false);
            motionEntries.setDrawCubic(true);
            motionEntries.setCubicIntensity(0.1f);

        }

        if(motionEntries != null){sets.add(motionEntries);}
        if(soundEntries != null){sets.add(soundEntries);}
        if(locationEntries != null){sets.add(locationEntries);}

        LineData cd = new LineData(getHours(), sets);
        return cd;
    }

    private String getTimeStringFromMins(double minutes)
    {
        String line = "";

        if(minutes < 60)
        {
            if(Math.round(minutes) < 10) {
                line = "00H:0" + Long.toString(Math.round(minutes)) + "M";
            }
            else
            {
                line = "00H:" + Long.toString(Math.round(minutes)) + "M";
            }
        }
        else if(minutes >= 60)
        {
            int numHours = (int)(minutes / 60);
            int numMins = (int)((int)minutes % (int)60);
            if(numHours < 10)
            {
                line = "0" + Integer.toString(numHours) + "H";
            }
            else
            {
                line = Integer.toString(numHours) + "H";
            }

            if(numMins < 10)
            {
                line += ":0" + numMins + "M";
            }
            else
            {
                line += ":" + numMins + "M";
            }
        }

        return line;
    }

    private SpannableString generateDigitalText(String l1) {

        int change = l1.length();
        String l = l1;
        SpannableString s = new SpannableString(l);

        Typeface digitalFont = Typeface.createFromAsset(getContext().getAssets(),"digital-7.ttf");

        int measureColor = ContextCompat.getColor(this.getContext(), R.color.clocktext);

        s.setSpan(new CustomTypefaceSpan("",digitalFont), 0, l1.length(), 0);
        s.setSpan(new ForegroundColorSpan(measureColor), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(1.0f), 0, l1.length(), 0);

        return s;
    }

    private SpannableString generateDigitalText_Time(String l1) {

        int change = l1.length();
        String l = l1;
        SpannableString s = new SpannableString(l);

        Typeface digitalFont = Typeface.createFromAsset(getContext().getAssets(),"digital-7.ttf");

        int measureColor = ContextCompat.getColor(this.getContext(), R.color.clocktext);


        if(l1.length() < 7)
        {
            int x = 1;
            int y=x+1;
        }

        s.setSpan(new CustomTypefaceSpan("",digitalFont), 0, 2, 0);
        s.setSpan(new ForegroundColorSpan(measureColor), 0, 2, 0);
        s.setSpan(new RelativeSizeSpan(1.0f), 0, 2, 0);

        s.setSpan(new CustomTypefaceSpan("",digitalFont), 2, 3, 0);
        s.setSpan(new ForegroundColorSpan(measureColor), 2, 3, 0);
        s.setSpan(new RelativeSizeSpan(0.4f), 2, 3, 0);

        s.setSpan(new CustomTypefaceSpan("",digitalFont), 3, 6, 0);
        s.setSpan(new ForegroundColorSpan(measureColor), 3, 6, 0);
        s.setSpan(new RelativeSizeSpan(1.0f), 3, 6, 0);

        s.setSpan(new CustomTypefaceSpan("",digitalFont), 6, 7, 0);
        s.setSpan(new ForegroundColorSpan(measureColor), 6, 7, 0);
        s.setSpan(new RelativeSizeSpan(0.4f), 6, 7, 0);


        return s;
    }


    private ArrayList<String> getHours() {

        ArrayList<String> m = new ArrayList<String>();
        m.add("1AM");
        m.add("2AM");
        m.add("3AM");
        m.add("4AM");
        m.add("5AM");
        m.add("6AM");
        m.add("7AM");
        m.add("8AM");
        m.add("9AM");
        m.add("10AM");
        m.add("11AM");
        m.add("12noon");
        m.add("1PM");
        m.add("2PM");
        m.add("3PM");
        m.add("4PM");
        m.add("5PM");
        m.add("6PM");
        m.add("7PM");
        m.add("8PM");
        m.add("9PM");
        m.add("10PM");
        m.add("11PM");
        m.add("12PM");

        return m;
    }

    private ArrayList<String> getDays() {

        ArrayList<String> m = new ArrayList<String>();
        m.add("Mon");
        m.add("Tue");
        m.add("Wed");
        m.add("Thu");
        m.add("Fri");
        m.add("Sat");
        m.add("Sun");


        return m;
    }

}
