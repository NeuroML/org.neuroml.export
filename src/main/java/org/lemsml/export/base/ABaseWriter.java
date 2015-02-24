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
    

	public ABaseWriter(Lems lems, Format format, File outputFolder) throws ModelFeatureSupportException, LEMSException, NeuroMLException 
    {
		this(lems, format, outputFolder, true);
    }
            
	public ABaseWriter(Lems lems, Format format, File outputFolder, boolean checkSupportedFeatures) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		this(lems, format, checkSupportedFeatures);
		this.outputFolder = outputFolder;
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


}
