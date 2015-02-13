package org.lemsml.export.base;

import java.io.File;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public abstract class ABaseWriter implements IBaseWriter
{

	protected Lems lems;
	protected final String format;
	private File outputFolder;
	
	protected static SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();

	public ABaseWriter(Lems lems, String format)
	{
		this.lems = lems;
		this.format = format;
		setSupportedFeatures();
		if(!isConversionSupported())
		{
			// FIXME
			System.out.println("Not supported");
		}
	}

	//To be removed but not sure yet
//	public ABaseWriter(String lemsFile, String format) throws LEMSException
//	{
//		this(Utils.readLemsNeuroMLFile(lemsFile).getLems(), format);
//	}
	
	public ABaseWriter(Lems lems, String format, File outputFolder)
	{
		this(lems, format);
		this.outputFolder = outputFolder;
	}

	protected abstract void addComment(StringBuilder sb, String comment);

	protected void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ALL, SupportLevelInfo.Level.HIGH);

	}

	@Override
	public Boolean isConversionSupported()
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

	public File getOutputFolder()
	{
		return outputFolder;
	}

	public void setOutputFolder(File outputFolder)
	{
		this.outputFolder = outputFolder;
	}
	
	


	// public class CompInfo
	// {
	// public StringBuilder stateVars = new StringBuilder();
	// public StringBuilder params = new StringBuilder();
	// public StringBuilder eqns = new StringBuilder();
	// public StringBuilder initInfo = new StringBuilder();
	// }

}
