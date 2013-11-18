package org.neuroml.export.info.model;

import org.neuroml.model.HHRate;

public class ChannelMLRateExpression<T extends HHRate> {
	private NeuroMLExpression _expression;
	private String _id;

	public ChannelMLRateExpression(T expr) {
		_id=expr.getType();
		if(ChannelMLStandardRateExpression.known_expressions.contains(_id)){
			_expression = new ChannelMLStandardRateExpression(expr);
		}
	}
	public String toString() {
		return _expression.toString();
	}
	
	public Double eval(Double x){
		return _expression.eval(x);
		
	}
	public String getId()
	{
		return _id;
	}
	
	
}

interface Function{
	public Double eval(Double t);
	public String toString();
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