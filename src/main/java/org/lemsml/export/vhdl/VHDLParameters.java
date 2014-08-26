package org.lemsml.export.vhdl;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Attachments;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.DerivedParameter;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.util.StringUtil;

public class VHDLParameters {

	public static void writeParameters(JsonGenerator g, Component comp, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();

		for(FinalParam p: params)
		{
			ParamValue pv = combinedParameterValues.getByName(p.getName());
			g.writeObjectFieldStart(p.getName());
			g.writeStringField("name",p.getName());
			
			if (pv != null)
			{
				g.writeStringField("type",pv.getDimensionName()+"");
				g.writeStringField("value",(float)pv.getDoubleValue()+"");
				VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
			}
			else
			{
				pv = combinedParameterValues.getByName(p.getName());
				g.writeStringField("type",pv.getDimensionName()+"");
				g.writeStringField("value",(float)pv.getDoubleValue()+"");
				VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
			}
			g.writeEndObject();
		}
		

	}
	

	public static void writeDerivedParameters(JsonGenerator g, ComponentType ct, 
			LemsCollection<DerivedParameter> derivedParameters, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues)  throws ContentError, JsonGenerationException, IOException
	{
		for (DerivedParameter dp: derivedParameters)
		{
			StringBuilder sensitivityList = new StringBuilder();
			g.writeObjectFieldStart(dp.getName());
			g.writeStringField("name",dp.getName());
			g.writeStringField("select",dp.getSelect() == null ? "" : dp.getSelect());
			g.writeStringField("type",dp.getDimension().getName()+"");
			String value = VHDLEquations.encodeVariablesStyle(dp.getValue(),
					ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
					ct.getRequirements(),sensitivityList,params,combinedParameterValues);
			g.writeStringField("value",	value);
			

			value = VHDLEquations.writeInternalExpLnLogEvaluators(value,g,dp.getName(),sensitivityList);
			VHDLFixedPointDimensions.writeBitLengths(g,dp.getDimension().getName());
					
			g.writeEndObject();
		}
	}
	
	
}
