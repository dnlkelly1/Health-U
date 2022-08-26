package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

/**
 * Created by daniel on 06/10/2015.
 */
public abstract class ClusterElement
{
    public abstract DataElement GetElement();
    public abstract DataElement[] GetRawElements();

    public abstract String ToString();
    public abstract ClusterElement FromString(String[] filedata);

}
