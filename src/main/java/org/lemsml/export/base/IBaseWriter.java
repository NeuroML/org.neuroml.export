package org.lemsml.export.base;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.neuroml.export.exceptions.GenerationException;

public interface IBaseWriter
{

	List<File> convert() throws GenerationException, IOException;
	
	void setSupportedFeatures();
	
	void setOutputFolder(File outputFolder);
	
	void setOutputFileName(String outputFileName);
	
}
