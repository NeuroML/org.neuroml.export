package org.neuroml.export.info.model;


interface NeuroMLExpression
{
	public abstract Double eval(Double t);
	public String toString();
}
