package org.neuroml.export.info.model;

import java.util.List;

import org.neuroml.export.info.model.pairs.IterablePair;
import org.neuroml.export.info.model.pairs.Pair;

/**
 * @author Adrian Quintana (adrian.perez@ucl.ac.uk)
 */
public class PlotMetadataNode
{

	private String _plotTitle;
	private String _xAxisLabel;
	private String _yAxisLabel;

	public PlotMetadataNode(String plotTitle, String xAxisLabel, String yAxisLabel)
	{
		super();
		this._plotTitle = plotTitle;
		this._xAxisLabel = xAxisLabel;
		this._yAxisLabel = yAxisLabel;
	}

	/**
	 * @return the _plotTitle
	 */
	public String getPlotTitle()
	{
		return _plotTitle;
	}

	/**
	 * @return the _xAxisLabel
	 */
	public String getXAxisLabel()
	{
		return _xAxisLabel;
	}

	/**
	 * @return the _yAxisLabel
	 */
	public String getYAxisLabel()
	{
		return _yAxisLabel;
	}


	@Override
	public String toString()
	{
		return "MetadataPlotNode [Title=" + _plotTitle + ", X=" + _xAxisLabel + ", Y=" + _yAxisLabel + "]";
	}
    
	public String toShortString()
	{
		return "MetadataPlotNode [Title=" + _plotTitle + ", X=" + _xAxisLabel + ", Y=" + _yAxisLabel + "]";
	}

	private String toTruncatedArray(List<Data> _data) {
		StringBuilder sb = new StringBuilder();
		
		for(Data data : _data)
		{
			IterablePair<Double,Double> listPair = new IterablePair<Double, Double>(data.getXData(),data.getYData());
			for (Pair<Double, Double> d: listPair)
			{
				sb.append(String.format("%.4f %.4f", d.first(), d.second()));
			}
		}
		return sb.toString();

	}
	
	
}
