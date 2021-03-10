package org.neuroml.export.info.model;

public class ChannelMLHHExpression
{
	private IPlottableExpression _expression;
	private String _type;

	public ChannelMLHHExpression(IHHExpression expr)
	{
		_type = expr.getType();
		if(ChannelMLStandardHHExpression.knownExpressions.contains(_type))
		{
			setExpression(new ChannelMLStandardHHExpression(expr));
		}
		else
		{
			setExpression(new ChannelMLGenericHHExpression(expr));
		}
	}

	public String toString()
	{
		return getExpression().toString();
	}

	public Double eval(Double x)
	{
		return getExpression().eval(x);

	}

	public String getId()
	{
		return _type;
	}

	public IPlottableExpression getExpression()
	{
		return _expression;
	}

	/**
	 * @param expression
	 *            the _expression to set
	 */
	public void setExpression(IPlottableExpression expression)
	{
		_expression = expression;
	}

}
