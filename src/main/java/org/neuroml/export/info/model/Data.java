package org.neuroml.export.info.model;

import java.util.List;

/**
 * @author matteocantarelli
 *
 */
public class Data
{

	@Override
	public String toString()
	{
		return "Data [X=" + _xData + ", Y=" + _yData + ", Label=" + _label + "]";
	}

	private List<Double> _xData;
	private List<Double> _yData;
	private String _label;
	
	/**
	 * @param x
	 * @param y
	 * @param label
	 */
	public Data(List<Double> x, List<Double> y, String label)
	{
		_xData=x;
		_yData=y;
		_label=label;
	}

	/**
	 * @return the _xData
	 */
	public List<Double> getXData()
	{
		return _xData;
	}

	/**
	 * @return the _yData
	 */
	public List<Double> getYData()
	{
		return _yData;
	}

	/**
	 * @return the _label
	 */
	public String getLabel()
	{
		return _label;
	}


}
