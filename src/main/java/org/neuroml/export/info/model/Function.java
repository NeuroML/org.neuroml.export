package org.neuroml.export.info.model;

import java.text.DecimalFormat;

abstract class Function{
	abstract public Double eval(Double t);
    
    protected static DecimalFormat df = new DecimalFormat("#.##");
    
    protected static String format(double s)
    {
        return df.format(s);
    }
}