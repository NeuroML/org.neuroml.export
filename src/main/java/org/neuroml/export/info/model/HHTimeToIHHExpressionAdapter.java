package org.neuroml.export.info.model;

import org.neuroml.model.HHTime;

public class HHTimeToIHHExpressionAdapter implements IHHExpression {
	private final HHTime _expr;

	public HHTimeToIHHExpressionAdapter(HHTime expr) {
		_expr = expr;
	}

	@Override
	public String getRate() {
		return _expr.getRate();
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

