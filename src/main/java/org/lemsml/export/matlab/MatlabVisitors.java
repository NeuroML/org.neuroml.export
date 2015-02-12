package org.lemsml.export.matlab;

import org.neuroml.export.utils.visitors.CommonLangWriter;

public class MatlabVisitors extends CommonLangWriter
{

	@Override
	public String getPow()
	{
		return " ^ ";
	}

	@Override
	public String getMult()
	{
		return " .* ";
	}

	@Override
	public String getDiv()
	{
		return " ./ ";
	}

}
