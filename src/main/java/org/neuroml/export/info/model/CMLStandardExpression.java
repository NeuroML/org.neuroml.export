package org.neuroml.export.info.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.model.HHRate;
import org.neuroml.export.Utils;


public class CMLStandardExpression<T extends HHRate> {
	private String expression;
	private Float rate;
	private Float midpoint;
	private Float scale;

	private static final Map<String, String> stdExprTempl;
	static
	{
		stdExprTempl = new HashMap<String, String>();
		stdExprTempl.put("HHSigmoidRate", "%g /(1 + exp((v - %g)/%g))");
		stdExprTempl.put("HHExpRate", "%g * exp((v - %g)/%g)");
		stdExprTempl.put("HHExpLinearRate", "%1$g * (v - %2$g) / ( 1 - exp(-(v - %2$g) / %3$g))");
		
	}

	CMLStandardExpression(T expr)  {
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

		expression = String.format(stdExprTempl.get(expr.getType()), rate, midpoint, scale);
	}

	

	/**
	 * @return the expression
	 */
	public String toString() {
		return expression;
	}
	
	private Double Sigmoid(double v, double rate, double midpoint, double scale)
	{
		return rate / (1 + Math.exp(v - midpoint) / scale);
	}
}
