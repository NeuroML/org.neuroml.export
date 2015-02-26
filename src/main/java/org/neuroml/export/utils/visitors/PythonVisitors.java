package org.neuroml.export.utils.visitors;

public class PythonVisitors extends CommonLangWriter
{

	@Override
	public String getPow()
	{
		return " ** ";
	}

}
