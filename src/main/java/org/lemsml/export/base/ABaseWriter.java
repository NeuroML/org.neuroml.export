package org.lemsml.export.base;

import java.io.File;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public abstract class ABaseWriter implements IBaseWriter
{

	protected Lems lems;
	protected final Format format;
	private File outputFolder;
	private String outputFileName;

	protected static SupportLevelInfo sli = SupportLevelInfo.getSupportLevelInfo();

	public ABaseWriter(Lems lems, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		this(lems, format, true);
	}
    
	public ABaseWriter(Lems lems, Format format, boolean checkSupportedFeatures) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		this.lems = lems;
		this.format = format;
		setSupportedFeatures();
        if (checkSupportedFeatures)
        {
            sli.checkConversionSupported(format, lems);
        }
	}
    

	public ABaseWriter(Lems lems, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException 
    {
		this(lems, format, outputFolder, outputFileName, true);
    }
            
	public ABaseWriter(Lems lems, Format format, File outputFolder, String outputFileName, boolean checkSupportedFeatures) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		this(lems, format, checkSupportedFeatures);
		this.outputFolder = outputFolder;
		this.outputFileName = outputFileName;
	}

	protected abstract void addComment(StringBuilder sb, String comment);


	public File getOutputFolder()
	{
		return outputFolder;
	}

	public void setOutputFolder(File outputFolder)
	{
		this.outputFolder = outputFolder;
	}

	public String getOutputFileName()
	{
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName)
	{
		this.outputFileName = outputFileName;
	}
	
	


}
