package org.neuroml.export.info.model;

public class ChannelMLGenericHHExpression implements IPlottableExpression {
	private Function _function;	
	private String _type;

	public ChannelMLGenericHHExpression(IHHExpression expr) {
		_type = expr.getType();
		_function = new NullFunction(expr.toString());
	}

	@Override
	public Double eval(Double t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

}

class NullFunction extends Function {
	private String _repr;

	public NullFunction(String string) {
		_repr = string;
	}

	@Override
	public Double eval(Double t) {
		return null;
	}

	@Override
	public String toString() {
		return _repr;
	}

    	
}
