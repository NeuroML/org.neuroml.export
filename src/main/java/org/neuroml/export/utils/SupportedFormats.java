package org.neuroml.export.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.model.util.NeuroMLException;

public class SupportedFormats
{

	public static List<Format> getSupportedOutputs(){
		return Arrays.asList(Format.values());
	}
	
	public static List<Format> getSupportedOutputs(Lems lems){
		List<Format> supportedOutputs = new ArrayList<Format>();
		for (Format format : Format.values()){
			
			try
			{
				ExportFactory.getExportWriter(lems, format);
				supportedOutputs.add(format);
			}
			catch(ModelFeatureSupportException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(NeuroMLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(LEMSException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return supportedOutputs;
	}
}
