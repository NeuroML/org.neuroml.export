package org.neuroml.export.info.model;



public class ChannelMLHHExpression{
	private IPlottableExpression _expression;
	private String _type;

	public ChannelMLHHExpression(IHHExpression expr) {
		_type = expr.getType();
		if(ChannelMLStandardHHExpression.known_expressions.contains(_type)){
			_expression = new ChannelMLStandardHHExpression(expr);
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
		return _type;
	}

	public IPlottableExpression getExpression() {
		return _expression;
	}


}
