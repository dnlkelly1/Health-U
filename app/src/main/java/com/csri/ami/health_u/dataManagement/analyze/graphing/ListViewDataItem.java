package com.csri.ami.health_u.dataManagement.analyze.graphing;

import com.github.mikephil.charting.data.LineData;

/**
 * Created by daniel on 20/10/2015.
 */
public class ListViewDataItem
{
    public LineData lineData;
    public String text;

    public double movement_summary;
    public double sound_summary;
    public double location_summary;

    public ListViewDataItem(LineData l,String t,double movement,double sound,double location)
    {
        lineData = l;
        text = t;
        movement_summary = movement;
        sound_summary = sound;
        location_summary = location;
    }
}


