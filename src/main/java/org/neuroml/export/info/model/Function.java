package org.neuroml.export.info.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

abstract class Function
{
	abstract public Double eval(Double t);

	protected static final DecimalFormat df1 = new DecimalFormat("#.#####E0");
	protected static final DecimalFormat df2 = new DecimalFormat("#.#####");

    
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.getDefault());
        sym.setDecimalSeparator('.');
        sym.setExponentSeparator("e");
        df1.setDecimalFormatSymbols(sym);
        df2.setDecimalFormatSymbols(sym);
    }

	protected static String format(double d)
	{
        // finer control on appearance... There may be a way to do this with DecimalFormat...
        if ( (d<1000 && d > 0.001) || (d>-1000 && d < -0.001))
            return df2.format(d);
		return df1.format(d);
	}
}