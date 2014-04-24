package org.neuroml.export.info.model;

import org.neuroml.model.HHVariable;

public class HHVariableToIHHExpressionAdapter implements IHHExpression{

	private final HHVariable _expr;

	public HHVariableToIHHExpressionAdapter(HHVariable expr){
		_expr = expr;
	}

	@Override
	public String getRate() {
		return _expr.getRate().toString();
	}

	@Override
	public String getMidpoint() {
		return _expr.getMidpoint();
	}

	@Override
	public String getScale() {
		return _expr.getScale();
	}

	@Override
	public String getType() {
		return _expr.getType();
	}

}
