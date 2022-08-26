package com.csri.ami.health_u.dataManagement.analyze.graphing;

import com.github.mikephil.charting.data.BarData;

/**
 * Created by daniel on 29/10/2015.
 */
public class ListViewBarItem
{
    public BarData lineData;
    public String text;

    public double movement_summary;
    public double sound_summary;
    public double location_summary;

    public ListViewBarItem(BarData l,String t,double movement,double sound,double location)
    {
        lineData = l;
        text = t;
        movement_summary = movement;
        sound_summary = sound;
        location_summary = location;
    }
}
