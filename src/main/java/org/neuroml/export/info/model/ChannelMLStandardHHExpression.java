package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.export.Utils;

class ChannelMLStandardHHExpression implements IPlottableExpression
{
	public static List<String> known_expressions = new ArrayList<String>(Arrays.asList("HHSigmoidRate", "HHExpRate", "HHExpLinearRate"));

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
			setId("HHSigmoidRate");
		}
		else if(_type.equals("HHExpRate")){
			_function = new HHExponentialRate(_rate, _scale, _midpoint);
			setId("HHExpRate");
		}
		else if(_type.equals("HHExpLinearRate")){
			_function = new HHExponentialLinearRate(_rate, _scale, _midpoint);
			setId("HHExpLinearRate");
		}
	}


	public Double eval(Double t) {
		return _function.eval(t);
	}

	public String toString()
	{
		return _function.toString();
	}


	@Override
	public String getId() {
		return "Standard ChannelML Expression:" + _Id;
	}


	public void setId(String _Id) {
		this._Id = _Id;
	}

}


interface Function{
	public Double eval(Double t);
}

class HHSigmoidalRate implements Function {
	Double rate;
	Double scale;
	Double midpoint;

	public HHSigmoidalRate(Double rate, Double scale, Double midpoint) {
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}


	public Double eval(Double v){
		return  rate / (  1 + Math.exp( (v - midpoint) / scale )  ) ;
	}

	public String toString()
	{
		return String.format("%g /(1 + exp((v - %g)/%g))", rate, midpoint, scale);
	}
}


class HHExponentialRate implements Function {
	Double rate;
	Double scale;
	Double midpoint;

	public HHExponentialRate(Double rate, Double scale, Double midpoint) {
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}

	public Double eval(Double v){
		return rate * Math.exp((v - midpoint) / scale);
	}

	public String toString()
	{
		return String.format("%g * exp((v - %g)/%g)", rate, midpoint, scale);
	}
}


class HHExponentialLinearRate implements Function {
	Double rate;
	Double scale;
	Double midpoint;

	public HHExponentialLinearRate(Double rate, Double scale, Double midpoint) {
		this.rate = rate;
		this.scale = scale;
		this.midpoint = midpoint;
	}

	public Double eval(Double v){
		Double fact = (v - midpoint) / scale;
		if (Math.abs(fact) < 1e-4) {
			//expand around removable singularity
			return rate * (1 + fact/2);
		}
		else {
			return (rate * fact)/(1 - Math.exp(-fact));
		}
	}

	public String toString()
	{
		return String.format("%1$g * (v - %2$g)/%3$g / ( 1 - exp(-(v - %2$g) / %3$g))", rate, midpoint, scale);
	}
}