package org.neuroml.export.info.model;

import org.neuroml.model.HHRate;

public class ChannelMLRateExpression<T extends HHRate> {
	private NMLMapping _expression;
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
	
	public Float eval(Float x){
		return _expression.eval(x);
		
	}
	public String getId()
	{
		return _id;
	}
	
	
}