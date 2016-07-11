package org.neuroml.export.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lemsml.export.base.IBaseWriter;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class SupportedFormats
{

	public static List<Format> getSupportedOutputs(){
		return Arrays.asList(Format.values());
	}
	
	public static List<Format> getSupportedOutputs(Lems lems) throws NeuroMLException, LEMSException{
		List<Format> supportedOutputs = new ArrayList<Format>();
		for (Format format : Format.values()){
			try
			{
				IBaseWriter writer = ExportFactory.getExportWriter(lems, format);
				if (writer != null){
					supportedOutputs.add(format);
					E.info("Format " + format + " supported");
				}
				else{
					E.info("Format " + format + " not supported");
				}
			}
			catch(ModelFeatureSupportException e)
			{
				E.info("Format " + format + " not supported");
			}
		}
		
		return supportedOutputs;
	}
}
