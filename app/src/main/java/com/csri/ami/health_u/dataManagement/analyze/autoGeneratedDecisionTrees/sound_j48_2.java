package com.csri.ami.health_u.dataManagement.analyze.autoGeneratedDecisionTrees;

class sound_j48_2 {

  public static double classify(Object [] i)
    throws Exception {

    double p = Double.NaN;
    p = sound_j48_2.N134ce4a0(i);
    return p;
  }
  static double N134ce4a0(Object []i) {
    double p = Double.NaN;
    if (i[4] == null) {
      p = 0;
    } else if (((Double) i[4]).doubleValue() <= 0.746428893336332) {
    p = sound_j48_2.N136a1a11(i);
    } else if (((Double) i[4]).doubleValue() > 0.746428893336332) {
    p = sound_j48_2.N6214f56(i);
    } 
    return p;
  }
  static double N136a1a11(Object []i) {
    double p = Double.NaN;
    if (i[3] == null) {
      p = 2;
    } else if (((Double) i[3]).doubleValue() <= 0.118024902629548) {
    p = sound_j48_2.N1ad6b4b2(i);
    } else if (((Double) i[3]).doubleValue() > 0.118024902629548) {
      p = 0;
    } 
    return p;
  }
  static double N1ad6b4b2(Object []i) {
    double p = Double.NaN;
    if (i[2] == null) {
      p = 2;
    } else if (((Double) i[2]).doubleValue() <= 1.01352562682857) {
    p = sound_j48_2.N5f2db03(i);
    } else if (((Double) i[2]).doubleValue() > 1.01352562682857) {
      p = 0;
    } 
    return p;
  }
  static double N5f2db03(Object []i) {
    double p = Double.NaN;
    if (i[5] == null) {
      p = 2;
    } else if (((Double) i[5]).doubleValue() <= 0.364988173686996) {
    p = sound_j48_2.Nb0a3f54(i);
    } else if (((Double) i[5]).doubleValue() > 0.364988173686996) {
      p = 0;
    } 
    return p;
  }
  static double Nb0a3f54(Object []i) {
    double p = Double.NaN;
    if (i[9] == null) {
      p = 2;
    } else if (((Double) i[9]).doubleValue() <= 0.429786429125021) {
      p = 2;
    } else if (((Double) i[9]).doubleValue() > 0.429786429125021) {
    p = sound_j48_2.Ndc41c55(i);
    } 
    return p;
  }
  static double Ndc41c55(Object []i) {
    double p = Double.NaN;
    if (i[5] == null) {
      p = 0;
    } else if (((Double) i[5]).doubleValue() <= 0.0262503837619724) {
      p = 0;
    } else if (((Double) i[5]).doubleValue() > 0.0262503837619724) {
      p = 2;
    } 
    return p;
  }
  static double N6214f56(Object []i) {
    double p = Double.NaN;
    if (i[1] == null) {
      p = 0;
    } else if (((Double) i[1]).doubleValue() <= -0.591388245416011) {
    p = sound_j48_2.N14e113b7(i);
    } else if (((Double) i[1]).doubleValue() > -0.591388245416011) {
      p = 1;
    } 
    return p;
  }
  static double N14e113b7(Object []i) {
    double p = Double.NaN;
    if (i[6] == null) {
      p = 1;
    } else if (((Double) i[6]).doubleValue() <= 0.700543695535243) {
    p = sound_j48_2.N4d76b48(i);
    } else if (((Double) i[6]).doubleValue() > 0.700543695535243) {
      p = 0;
    } 
    return p;
  }
  static double N4d76b48(Object []i) {
    double p = Double.NaN;
    if (i[1] == null) {
      p = 0;
    } else if (((Double) i[1]).doubleValue() <= -1.43107373664633) {
      p = 0;
    } else if (((Double) i[1]).doubleValue() > -1.43107373664633) {
    p = sound_j48_2.N1ac5f139(i);
    } 
    return p;
  }
  static double N1ac5f139(Object []i) {
    double p = Double.NaN;
    if (i[2] == null) {
      p = 0;
    } else if (((Double) i[2]).doubleValue() <= 0.4382512729199135) {
    p = sound_j48_2.N195dd5b10(i);
    } else if (((Double) i[2]).doubleValue() > 0.4382512729199135) {
      p = 0;
    } 
    return p;
  }
  static double N195dd5b10(Object []i) {
    double p = Double.NaN;
    if (i[7] == null) {
      p = 0;
    } else if (((Double) i[7]).doubleValue() <= -0.5917024075849335) {
      p = 0;
    } else if (((Double) i[7]).doubleValue() > -0.5917024075849335) {
    p = sound_j48_2.N1f78b6811(i);
    } 
    return p;
  }
  static double N1f78b6811(Object []i) {
    double p = Double.NaN;
    if (i[6] == null) {
      p = 0;
    } else if (((Double) i[6]).doubleValue() <= -0.285655137486672) {
      p = 0;
    } else if (((Double) i[6]).doubleValue() > -0.285655137486672) {
    p = sound_j48_2.Ne183e912(i);
    } 
    return p;
  }
  static double Ne183e912(Object []i) {
    double p = Double.NaN;
    if (i[5] == null) {
      p = 0;
    } else if (((Double) i[5]).doubleValue() <= -0.205333436943577) {
      p = 0;
    } else if (((Double) i[5]).doubleValue() > -0.205333436943577) {
      p = 0;
    } 
    return p;
  }
}
