package org.neuroml.export.info.model;

/**
 * @author Adrian Quintana (adrian.perez@ucl.ac.uk)
 * 
 */
public class ExpressionNode
{

	private String expression;
	

	public ExpressionNode(String expression)
	{
		super();
		this.expression = expression;
	}

	

	public String getExpression() {
		return expression;
	}



	public void setExpression(String expression) {
		this.expression = expression;
	}



	@Override
	public String toString()
	{
		return "FunctionNode [Expression=" + expression + "]";
	}
    
	
}
