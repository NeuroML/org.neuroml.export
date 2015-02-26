package org.neuroml.export.info.model;

interface IPlottableExpression
{
	public abstract Double eval(Double t);

	public abstract String toString();

	public abstract String getId();
}
