package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.export.Utils;
import org.neuroml.model.HHRate;

class ChannelMLStandardRateExpression implements NeuroMLExpression
{
	public static List<String> known_expressions = new ArrayList<String>(Arrays.asList("HHSigmoidRate", "HHExpRate", "HHExpLinearRate"));

	private Function _function;	
	private String _type;
	private Double _rate;
	private Double _midpoint;
	private Double _scale;

	ChannelMLStandardRateExpression(HHRate expr)
	{

		try
		{
			_rate = (double) Utils.getMagnitudeInSI(expr.getRate());
			_midpoint = (double) Utils.getMagnitudeInSI(expr.getMidpoint());
			_scale = (double) Utils.getMagnitudeInSI(expr.getScale());
		}
		catch(ParseError e)
		{
			throw new RuntimeException(e);
		}
		catch(ContentError e)
		{
			throw new RuntimeException(e);
		}

		_type = expr.getType();
		if(_type.equals("HHSigmoidRate")){
			_function = new HHSigmoidalRate(_rate, _scale, _midpoint);
		}
		else if(_type.equals("HHExpRate")){
			_function = new HHExponentialRate(_rate, _scale, _midpoint);
		}
		else if(_type.equals("HHExpLinearRate")){
			_function = new HHExponentialLinearRate(_rate, _scale, _midpoint);
		}
	}


	public Double eval(Double t) {
		return _function.eval(t);
	}

	public String toString()
	{
		return _function.toString();
	}

	}