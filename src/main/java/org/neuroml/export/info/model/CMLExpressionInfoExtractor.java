package org.neuroml.export.info.model;

import org.neuroml.model.HHRate;

public class CMLExpressionInfoExtractor<T extends HHRate> {
	private NMLMapping _expression;

	public CMLExpressionInfoExtractor(T expr) {
		String ty = expr.getType();
		if(CMLStandardExpression.known_expressions.contains(ty)){
			_expression = new CMLStandardExpression(expr);
		}
	}
	public String toString() {
		return _expression.toString();
	}
	
	public Float eval(Float x){
		return _expression.eval(x);
		
	}
	
	
}