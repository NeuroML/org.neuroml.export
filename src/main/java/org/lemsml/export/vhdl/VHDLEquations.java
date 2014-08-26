package org.lemsml.export.vhdl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.VHDLUtilComparator;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Requirement;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.StateVariable;

public class VHDLEquations {

	public static String cond2sign(String cond) 
	{
	    String ret = "???";
	    if (cond.indexOf(".gt.")>0 )
	    	return " ,2,-18))(20)  = '0'";
	    if (cond.indexOf(".geq.")>0)
	    	return " ,2,-18))(20)  = '0'";
	    if (cond.indexOf(".lt.")>0)
	    	return " ,2,-18))(20)  = '1'";
	    if (cond.indexOf(".leq.")>0)
	    	return ",2,-18))(20)  = '1'";
	    if (cond.indexOf(".eq.")>0)
	    	return ",2,-18)) = (20 downto 0 => '0')";
	    if (cond.indexOf(".neq.")>0)
	    	return ",2,-18)) /= (20 downto 0 => '0')";
	    return ret;
	}

	public static String inequalityToCondition(String ineq)
	{

		    String[] s = ineq.split("(\\.)[gt|lt|neq|eq]+(\\.)");
		    //E.info("Split: "+ineq+": len "+s.length+"; "+s[0]+", "+s[1]);
		    String expr =  "To_slv(resize( " + s[0].trim() + " - (" + s[1].trim() + ")";
		    //sign = comp2sign(s.group(2))
	    
	    return expr;
	}
	

	public static String writeInternalExpLnLogEvaluators(String toEncode, JsonGenerator g, 
			String variableName, StringBuilder sensitivityList ) throws JsonGenerationException, IOException
	{
		String returnValue = toEncode.replace("(", " ( ").replace(")", " ) ");
		Pattern MY_PATTERN = Pattern.compile("exp +\\(([a-zA-Z0-9_ \\,\\*\\\\\\/\\+\\-\\(\\)]+)\\) ");
		Matcher m = MY_PATTERN.matcher(returnValue);
		int i = 1;
		g.writeArrayFieldStart("Exponentials");
		while (m.find()) {
			g.writeStartObject();
		    String s = m.group(1);
		    m.start();
		    int openingCount = m.group().split("\\(").length -1;
		    int closingCount = m.group().split("\\)").length -1;
		    String groupToReplace =  m.group();
		    while (closingCount > openingCount )
		    {
		    	int lastIndex = groupToReplace.lastIndexOf(")");
		    	int secondlastIndex = 0;
		    	for (int j= lastIndex; j>1;j--)
		    	{
		    		secondlastIndex = groupToReplace.indexOf(")",j);
			    	if (secondlastIndex != lastIndex)
			    		break;
		    	}
		    	groupToReplace = groupToReplace.substring(0,secondlastIndex+1);
		    	closingCount--;
		    }
		    returnValue = returnValue.replace(groupToReplace,"exp_" + variableName + "_exponential_result" + i);
		    sensitivityList.append("exp_" + variableName + "_exponential_result" + i + ",");
		    g.writeStringField("name","exponential_result" + i); 
			g.writeStringField("value", groupToReplace.substring(3) ); 
		    i++;
			g.writeEndObject();
		    // s now contains "BAR"
		}
		g.writeEndArray();


		
		
		
		
		
		MY_PATTERN = Pattern.compile("([a-zA-Z0-9_ ]+) +\\*\\* +([a-zA-Z0-9_ ]+) ");
		m = MY_PATTERN.matcher(returnValue);
		i = 1;
		g.writeArrayFieldStart("Powers");
		while (m.find()) {
			g.writeStartObject();
		    String s = m.group(1);
		    m.start();
		    int openingCount = m.group().split("\\(").length -1;
		    int closingCount = m.group().split("\\)").length -1;
		    String groupToReplace =  m.group();
		    while (closingCount > openingCount )
		    {
		    	int lastIndex = groupToReplace.lastIndexOf(")");
		    	int secondlastIndex = 0;
		    	for (int j= lastIndex; j>1;j--)
		    	{
		    		secondlastIndex = groupToReplace.indexOf(")",j);
			    	if (secondlastIndex != lastIndex)
			    		break;
		    	}
		    	groupToReplace = groupToReplace.substring(0,secondlastIndex+1);
		    	closingCount--;
		    }
		    returnValue = returnValue.replace(groupToReplace,"pow_" + variableName + "_power_result" + i);
			sensitivityList.append("pow_" + variableName + "_power_result" + i + ",");
		    g.writeStringField("name","power_result" + i); 
			g.writeStringField("valueA", m.group(1) ); 
			g.writeStringField("valueX", m.group(2) ); 
		    i++;
			g.writeEndObject();
		    // s now contains "BAR"
		}
		g.writeEndArray();
		return returnValue;
	}
	
	public static String encodeVariablesStyle(String toEncode, LemsCollection<FinalParam> paramsOrig, 
			LemsCollection<StateVariable> stateVariables,LemsCollection<DerivedVariable> derivedVariables,
			LemsCollection<Requirement> requirements, StringBuilder sensitivityList 
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError
	{
		char[] arrOperators = { '(',' ', ')', '*', '/', '\\', '+', '-', '^' };
	    String regex = "(" + new String(arrOperators).replaceAll("(.)", "\\\\$1|").replaceAll("\\|$", ")"); // escape every char with \ and turn into "OR"
		String returnString = toEncode;
		String[] items = toEncode.split(regex );//"[ \\(\\)\\*\\/\\+\\-\\^]");
		List<String> list = new ArrayList<String>(); 
		for (int i = 0; i < items.length; i ++)
		{
			list.add(items[i]);
		}
		
		VHDLUtilComparator comparator = new VHDLUtilComparator("abc");
		HashSet<String> hs = new HashSet<String>();
		hs.addAll(list);
		list.clear();
		list.addAll(hs);
		java.util.Collections.sort(list, comparator );//  
		returnString = " " + returnString.replaceAll("\\*"," \\* ").replaceAll("\\^"," \\** ").replaceAll("/"," / ").replaceAll("\\("," \\( ").replaceAll("\\)"," \\) ").replaceAll("-"," - ").replaceAll("\\+"," \\+ ").replaceAll("  "," ") + " ";

		returnString = returnString.replaceAll("([a-zA-Z0-9_]+) \\*\\* 2", "$1 \\* $1");
		
		for (int i = list.size() -1; i >= 0 ; i --)
		{
			String toReplace = list.get(i);
			try {
				if (paramsOrig.hasName(toReplace))
				{
					sensitivityList.append(" param_" + paramsOrig.getByName(toReplace).r_dimension.getName() + "_" + toReplace + ",");
					returnString = returnString.replaceAll(" " + toReplace + " "," param_" + paramsOrig.getByName(toReplace).r_dimension.getName() + "_" + toReplace + " ");
				}
				else
				if (requirements.hasName(toReplace))
				{
					sensitivityList.append(" requirement_" + requirements.getByName(toReplace).dimension + "_" + toReplace + " ,");
					returnString = returnString.replaceAll(" " + toReplace + " "," requirement_" + requirements.getByName(toReplace).dimension + "_" + toReplace + " ");
				}
				else
				if (stateVariables.hasName(toReplace))
				{
					sensitivityList.append(" statevariable_" + stateVariables.getByName(toReplace).dimension + "_" + toReplace + "_in ,");
					returnString = returnString.replaceAll(" " + toReplace + " "," statevariable_" + stateVariables.getByName(toReplace).dimension + "_" + toReplace + "_in ");
				}
				else
				if (derivedVariables.hasName(toReplace))
				{
					sensitivityList.append(" derivedvariable_" + derivedVariables.getByName(toReplace).dimension + "_" + toReplace + " ,");
					returnString = returnString.replaceAll(" " + toReplace + " "," derivedvariable_" + derivedVariables.getByName(toReplace).dimension + "_" + toReplace + " ");
				}
				else
					if (toReplace.equals("t"))
					{
						sensitivityList.append(" sysparam_time_simtime,");
						returnString = returnString.replaceAll(" t ","sysparam_time_simtime");
					}
					else if (tryParseFloat(toReplace))
					{
						float number = Float.parseFloat(toReplace);
						int top = 30;
						for (int j = 0; j < 30; j++)
						{
							if (Math.pow(2,j) > number)
							{
								top = j;
								break;
							}
						}
						int bottom = 1;
						for (int j = 1; j < 30; j++)
						{
							if (Math.pow(2,j) * number % 1 == 0)
							{
								bottom = j;
								break;
							}
						}
						returnString = returnString.replaceAll(" " + toReplace + " "," to_sfixed ( " + toReplace + " ," + top + " , -" + bottom + ")");
						
					}
			} catch (ContentError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		returnString.replace("  ", " ");
		
		
		//replace all param_ / param_ with one precalculated parameter
		for (FinalParam param1 : paramsOrig)
		{
			for (FinalParam param2 : paramsOrig)
			{
				String param1_s = "param_" + param1.r_dimension.getName() + "_" + param1.name;
				String param2_s = "param_" + param2.r_dimension.getName() + "_" + param2.name;
				if (returnString.replace("  ", " ").contains(param1_s + " / " + param2_s))
				{
					returnString = returnString.replace(param1_s + " / " + param2_s," param_" + param1.r_dimension.getName()  + "_div_"  +param2.r_dimension.getName()  + "_" + param1.name + "_div_" + param2.name);
					FinalParam fp = new FinalParam(param1.name + "_div_" + param2.name, new Dimension(param1.r_dimension.getName() + "_div_" + param2.r_dimension.getName()));
					if (!params.hasName(fp.name))
					{
						params.add(fp);
						combinedParameterValues.add(new ParamValue(fp, combinedParameterValues.getByName(param1.name).getDoubleValue() / combinedParameterValues.getByName(param2.name).getDoubleValue()));
					}
				}
			}
		}

		//replace all / param_ with one precalculated inverse parameter
		for (FinalParam param1 : paramsOrig)
		{
				String param1_s = "param_" + param1.r_dimension.getName() + "_" + param1.name;
				if (returnString.replace("  ", " ").contains(" / "+param1_s))
				{
					returnString = returnString.replace(" / " + param1_s," * param_" + param1.r_dimension.getName() + "_inv_" + param1.name + "_inv");
					FinalParam fp = new FinalParam(param1.name + "_inv", new Dimension(param1.r_dimension.getName() + "_inv"));
					if (!params.hasName(fp.name))
					{
						params.add(fp);
						combinedParameterValues.add(new ParamValue(fp, 1 / combinedParameterValues.getByName(param1.name).getDoubleValue()));
					}
				}
		}

		//replace all param_ * param_ with one precalculated parameter
		
		
		
		return returnString;
	}
	

	private static boolean tryParseFloat(String value)  
	{  
	     try  
	     {  
	    	 Float.parseFloat(value);  
	         return true;  
	      } catch(NumberFormatException nfe)  
	      {  
	          return false;  
	      }  
	}
	

}
