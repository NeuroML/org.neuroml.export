package org.neuroml.export.info.model;

import org.neuroml.model.HHRate;

public class HHRateToIHHExpressionAdapter implements IHHExpression {
	private final HHRate _expr;

	public HHRateToIHHExpressionAdapter(HHRate expr) {
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
