package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.model.HHRate;
import org.neuroml.export.Utils;

abstract class NMLMapping{
	protected String string_repr;

	public abstract Float eval(Float t);
	
	public String toString(){
		return string_repr;
	};
}


class ChannelMLStandardRateExpression extends NMLMapping{
	public static List<String> known_expressions = new ArrayList<String>(
			Arrays.asList("HHSigmoidRate", "HHExpRate", "HHExpLinearRate")
	);
	
	R1toR1Mapping function;
	String type;
	Float rate;
	Float midpoint;
	Float scale;

	ChannelMLStandardRateExpression(HHRate expr) {

		try {
			rate =  Utils.getMagnitudeInSI(expr.getRate());
			midpoint = Utils.getMagnitudeInSI(expr.getMidpoint());
			scale =  Utils.getMagnitudeInSI(expr.getScale());
		} catch (ParseError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ContentError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		type = expr.getType();
		if(type.equals("HHSigmoidRate")){
			function = new HHSigmoidalRate(rate, scale, midpoint);
		}
		else if(type.equals("HHExpRate")){
			function = new HHExponentialRate(rate, scale, midpoint);
		}
		else if(type.equals("HHExpLinearRate")){
			function = new HHExponentialLinearRate(rate, scale, midpoint);
		}
	}


	public Float eval(Float t) {
		return function.eval(t);
	}

	public String toString() {
		return function.toString();
	}

	}

    interface R1toR1Mapping{
    	public Float eval(Float t);
    	public String toString();
    }

	class HHSigmoidalRate implements R1toR1Mapping {
		Float rate;
		Float scale;
		Float midpoint;

		public HHSigmoidalRate(Float rate, Float scale, Float midpoint) {
			this.rate = rate;
			this.scale = scale;
			this.midpoint = midpoint;
		}


		public Float eval(Float v){
			return (float) (rate / (1 + Math.exp(v - midpoint) / scale));
		}
		
		public String toString(){
			return String.format("%g /(1 + exp((v - %g)/%g))", rate, midpoint, scale);
		}
	}


	class HHExponentialRate implements R1toR1Mapping {
		Float rate;
		Float scale;
		Float midpoint;

		public HHExponentialRate(Float rate, Float scale, Float midpoint) {
			this.rate = rate;
			this.scale = scale;
			this.midpoint = midpoint;
		}

		public Float eval(Float v){
			return (float) (rate * Math.exp((v - midpoint) / scale));
		}
		
		public String toString(){
			return String.format("%g * exp((v - %g)/%g)", rate, midpoint, scale);
		}
	}


	class HHExponentialLinearRate implements R1toR1Mapping {
		Float rate;
		Float scale;
		Float midpoint;

		public HHExponentialLinearRate(Float rate, Float scale, Float midpoint) {
			this.rate = rate;
			this.scale = scale;
			this.midpoint = midpoint;
		}

		public Float eval(Float v){
			Float fact = (v - midpoint) / scale;
			return (float) ((rate * fact)/(1 - Math.exp(-fact)));
		}
		
		public String toString(){
			return String.format("%1$g * (v - %2$g)/%3$g / ( 1 - exp(-(v - %2$g) / %3$g))", rate, midpoint, scale);
		}
	}