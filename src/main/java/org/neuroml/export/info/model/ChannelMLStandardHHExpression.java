package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLException;

class ChannelMLStandardHHExpression implements IPlottableExpression
{
	public static List<String> knownExpressions = new ArrayList<String>(Arrays.asList("HHSigmoidRate", "HHExpRate", "HHExpLinearRate", "HHSigmoidVariable", "HHExpVariable", "HHExpLinearVariable"));

	private Function _function;
	private String _type;
	private Double _rate;
	private Double _midpoint;
	private Double _scale;
	private String _Id;

	ChannelMLStandardHHExpression(IHHExpression expr)
	{
		try
		{
			_rate = (double) Utils.getMagnitudeInSI(expr.getRate());
			_midpoint = (double) Utils.getMagnitudeInSI(expr.getMidpoint());
			_scale = (double) Utils.getMagnitudeInSI(expr.getScale());
		}
		catch(NeuroMLException e)
		{
			throw new RuntimeException(e);
		}

		_type = expr.getType();

		if(_type.startsWith("HHSigmoid"))
		{
			_function = new HHSigmoidalRate(_rate, _scale, _midpoint);
			setId("HHSigmoidRate");
		}
		else if(_type.startsWith("HHExpLinearRate"))
		{
			_function = new HHExponentialLinearRate(_rate, _scale, _midpoint);
			setId("HHExpLinearRate");
		}
		else if(_type.startsWith("HHExp"))
		{
			_function = new HHExponentialRate(_rate, _scale, _midpoint);
			setId("HHExpRate");
		}

	}

	@Override
	public Double eval(Double t)
	{
		return _function.eval(t);
	}

	@Override
	public String toString()
	{
		return _function.toString();
	}

	@Override
	public String getId()
	{
		// return "Standard ChannelML Expression:" + _Id;
		return _Id;
	}

	public void setId(String _Id)
	{
		this._Id = _Id;
	}

}

class HHSigmoidalRate extends Function
{
	Double rate;
	Double scale;
	Double midpoint;

	public HHSigmoidalRate(Double rate, Double scale, Double midpoint)
	{
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}

	@Override
	public Double eval(Double v)
	{
		return rate / (1 + Math.exp((v - midpoint) / scale));
	}

	@Override
	public String toString()
	{
		return String.format("%s /(1 + exp((v - (%s))/%s))", format(rate), format(midpoint), format(scale));
	}
}

class HHExponentialRate extends Function
{
	Double rate;
	Double scale;
	Double midpoint;

	public HHExponentialRate(Double rate, Double scale, Double midpoint)
	{
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}

	@Override
	public Double eval(Double v)
	{
		return rate * Math.exp((v - midpoint) / scale);
	}

	@Override
	public String toString()
	{
		return String.format("%s * exp((v - (%s))/%s)", format(rate), format(midpoint), format(scale));
	}
}

class HHExponentialLinearRate extends Function
{
	Double rate;
	Double scale;
	Double midpoint;

	public HHExponentialLinearRate(Double rate, Double scale, Double midpoint)
	{
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}

	@Override
	public Double eval(Double v)
	{
		Double fact = (v - midpoint) / scale;
		if(Math.abs(fact) < 1e-4)
		{
			// expand around removable singularity
			return rate * (1 + fact / 2);
		}
		else
		{
			return (rate * fact) / (1 - Math.exp(-fact));
		}
	}

	@Override
	public String toString()
	{
		return String.format("%1$s * (v - (%2$s))/%3$s / ( 1 - exp(-(v - (%2$s)) / %3$s))", format(rate), format(midpoint), format(scale));
	}
}