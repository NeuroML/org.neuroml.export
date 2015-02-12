package org.lemsml.export.base;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exception.ModelFeatureSupportException;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public abstract class ABaseWriter implements IBaseWriter
{

	protected Lems lems;
	protected final String format;

	protected static SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();

	public ABaseWriter(Lems lems, String format)
	{
		this.lems = lems;
		this.format = format;
		setSupportedFeatures();
	}

	protected abstract void addComment(StringBuilder sb, String comment);

	protected void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ALL, SupportLevelInfo.Level.HIGH);

	}

	@Override
	public Boolean isConversionSupported(Lems lems)
	{

		try
		{
			sli.checkConversionSupported(format, lems);
		}
		catch(ModelFeatureSupportException e)
		{
			return false;
		}
		catch(LEMSException | NeuroMLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	// public abstract String getMainScript() throws LEMSException, IOException;

	// public class CompInfo
	// {
	// public StringBuilder stateVars = new StringBuilder();
	// public StringBuilder params = new StringBuilder();
	// public StringBuilder eqns = new StringBuilder();
	// public StringBuilder initInfo = new StringBuilder();
	// }

}
