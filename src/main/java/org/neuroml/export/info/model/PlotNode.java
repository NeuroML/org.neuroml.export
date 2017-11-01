package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.neuroml.export.info.model.pairs.IterablePair;
import org.neuroml.export.info.model.pairs.Pair;

/**
 * @author borismarin
 * 
 */
public class PlotNode extends PlotMetadataNode
{

	// private String _plotTitle;
	// private String _xAxisLabel;
	// private String _yAxisLabel;
	private List<Data> _data = null;

	public PlotNode(String plotTitle, String xAxisLabel, String yAxisLabel, Double initialValue, Double finalValue, Double stepValue)
	{
		super(plotTitle, xAxisLabel, yAxisLabel, initialValue, finalValue, stepValue);
	}

	public PlotNode(String plotTitle, String xAxisLabel, String yAxisLabel)
	{
		super(plotTitle, xAxisLabel, yAxisLabel);
	}

	/**
	 * @return
	 */
	public List<Data> getData()
	{
		if(_data == null)
		{
			_data = new ArrayList<Data>();
		}
		return _data;
	}

	@Override
	public String toString()
	{
		return "PlotNode [Title=" + getPlotTitle() + ", X=" + getXAxisLabel() + ", Y=" + getYAxisLabel() + ", Data=" + toTruncatedArray(_data) + "]";
	}

    @Override
	public String toShortString()
	{
		return "PlotNode [Title=" + getPlotTitle() + ", X=" + getXAxisLabel() + ", Y=" + getYAxisLabel() + ", Num data points=" + _data.size() 
            +(_data.size()>0 ? " (first: "+_data.get(0).getLabel()+", #"+_data.get(0).getXData().size()+")" : "" ) +"]";
	}

	private String toTruncatedArray(List<Data> _data)
	{
		StringBuilder sb = new StringBuilder();

		for(Data data : _data)
		{
			IterablePair<Double, Double> listPair = new IterablePair<Double, Double>(data.getXData(), data.getYData());
			for(Pair<Double, Double> d : listPair)
			{
				sb.append(String.format(Locale.US, "%.4f %.4f", d.first(), d.second()));
			}
		}
		return sb.toString();

	}

}
