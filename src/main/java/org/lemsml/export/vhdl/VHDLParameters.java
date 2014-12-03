package org.lemsml.export.vhdl;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDDerivedParameter;
import org.lemsml.export.vhdl.edlems.EDParameter;
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

	public static ArrayList<EDParameter> writeParameters( Component comp, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		ArrayList<EDParameter> edParameters = new ArrayList<EDParameter>();
		for(FinalParam p: params)
		{
			EDParameter edParameter = new EDParameter();
			ParamValue pv = combinedParameterValues.getByName(p.getName());

			edParameter.name=(p.getName());
			
			if (pv != null)
			{
				edParameter.type=(pv.getDimensionName()+"");
				edParameter.value=((float)pv.getDoubleValue()+"");
				VHDLFixedPointDimensions.writeBitLengths(edParameter,pv.getDimensionName());
			}
			else
			{
				pv = combinedParameterValues.getByName(p.getName());
				edParameter.type=(pv.getDimensionName()+"");
				edParameter.value=((float)pv.getDoubleValue()+"");
				VHDLFixedPointDimensions.writeBitLengths(edParameter,pv.getDimensionName());
			}
			edParameters.add(edParameter);
		}
		return edParameters;

	}
	

	public static ArrayList<EDDerivedParameter> writeDerivedParameters( ComponentType ct, 
			LemsCollection<DerivedParameter> derivedParameters, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues)  throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDDerivedParameter> derivedparameters = new ArrayList<EDDerivedParameter>();
		for (DerivedParameter dp: derivedParameters)
		{
			EDDerivedParameter edDerivedParameter = new EDDerivedParameter();
			StringBuilder sensitivityList = new StringBuilder();
			edDerivedParameter.name = (dp.getName());
			edDerivedParameter.select = (dp.getSelect() == null ? "" : dp.getSelect());
			edDerivedParameter.type = (dp.getDimension().getName()+"");
			String value = VHDLEquations.encodeVariablesStyle(dp.getValue(),
					ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
					ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
			

			value = VHDLEquations.writeInternalExpLnLogEvaluators(value,edDerivedParameter,dp.getName(),sensitivityList,"");
			value = value.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
			edDerivedParameter.value=(value);
			VHDLFixedPointDimensions.writeBitLengths(edDerivedParameter,dp.getDimension().getName());
					
			derivedparameters.add(edDerivedParameter);
		}
		return derivedparameters;
	}
	
	
}
