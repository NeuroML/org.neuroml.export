package org.neuroml.export.info.model;

import java.text.DecimalFormat;

abstract class Function
{
	abstract public Double eval(Double t);

	protected static DecimalFormat df = new DecimalFormat("#.####E0");

	// protected static Formatter fmt = new Formatter();

	protected static String format(double s)
	{
		return df.format(s);
		// return fmt.format("%f", s).toString();
		// return Double.toString(s);
	}
}