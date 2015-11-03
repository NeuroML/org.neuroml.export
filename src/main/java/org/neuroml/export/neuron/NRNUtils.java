/**
 *
 */
package org.neuroml.export.neuron;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.QuantityReader;
import org.neuroml.export.utils.Utils;

/**
 * @author Boris Marin & Padraig Gleeson
 * 
 */
public class NRNUtils
{

	final static String NEURON_VOLTAGE = "v";
	final static String NEURON_TEMP = "celsius";
	final static String RESERVED_STATE_SUFFIX = "I";
	final static String RATE_PREFIX = "rate_";
	final static String REGIME_PREFIX = "regime_";
	final static String V_COPY_PREFIX = "copy_";

	final static String caConc = "caConc";

	final static String comm = "# ";
	final static String commPre = "'''";
	final static String commPost = "'''";

	static final int commentOffset = 40;

	static final String generalUnits = "\n(nA) = (nanoamp)\n" 
                                        + "(uA) = (microamp)\n" 
                                        + "(mA) = (milliamp)\n" 
                                        + "(A) = (amp)\n" 
                                        + "(mV) = (millivolt)\n" 
                                        + "(mS) = (millisiemens)\n"
                                        + "(uS) = (microsiemens)\n" 
                                        + "(molar) = (1/liter)\n" 
                                        + "(kHz) = (kilohertz)\n" 
                                        + "(mM) = (millimolar)\n" 
                                        + "(um) = (micrometer)\n" 
                                        + "(umol) = (micromole)\n" 
                                        + "(S) = (siemens)\n";

	static final String ghkUnits = ": bypass nrn default faraday const\n" + "FARADAY = 96485.3 (coulomb)\n" + "R = (k-mole) (joule/degC)\n";

	static final String ghkFunctionDefs = "\nFUNCTION ghk(v(mV), ci(mM), co(mM)) (.001 coul/cm3) {\n" 
                                            + "        LOCAL z, eci, eco\n" 
                                            + "        z = (1e-3)*2*FARADAY*v/(R*(celsius+273.15))\n"
                                            + "        eco = co*efun(z)\n" + "        eci = ci*efun(-z)\n" 
                                            + "        :high cao charge moves inward\n" 
                                            + "        :negative potential charge moves inward\n"
                                            + "        ghk = (.001)*2*FARADAY*(eci - eco)\n" 
                                            + "}\n" 
                                            + "\n" 
                                            + "FUNCTION efun(z) {\n" 
                                            + "        if (fabs(z) < 1e-4) {\n" 
                                            + "                efun = 1 - z/2\n" 
                                            + "        }else{\n"
                                            + "                efun = z/(exp(z) - 1)\n" 
                                            + "        }\n" + "}\n";
    
	static final String randomFunctionDefs = "\n: Returns a float between 0 and max\nFUNCTION random_float(max) {\n" +
                                            "    \n" +
                                            "    random_float = scop_random()*max\n" +
                                            "    \n" +
                                            "}\n\n";

	// TODO Add more keywords / builtin mechanisms
	static final Set<String> NRNKeywords = new HashSet<String>()
	{
		{
			add("IClamp");
			add("pas");
			add("hh");
			add("extracellular");
			add("fastpas");
		}
	};

	public static String getSafeName(String id)
	{
		String suffix = "";

		if(NRNKeywords.contains(id))
		{
			suffix = "_nml2";
		}

		return id + suffix;
	}

	protected static String getStateVarName(String sv)
	{
		if(sv.equals(NRNUtils.NEURON_VOLTAGE))
		{
			return NRNUtils.NEURON_VOLTAGE + NRNUtils.RESERVED_STATE_SUFFIX;
		}
		else
		{
			return sv;
		}
	}

	protected static String checkForBinaryOperators(String expr)
	{
		return expr.replace("\\.gt\\.", ">").replace("\\.geq\\.", ">=").replace("\\.lt\\.", "<").replace("\\.leq\\.", "<=").replace("\\.and\\.", "&&").replace("\\.neq\\.", "!=");
	}

	protected static String checkForStateVarsAndNested(String expr, Component comp, HashMap<String, HashMap<String, String>> paramMappings)
	{

		if(expr == null)
		{
			return null;
		}

		String newExpr = expr.trim();

		newExpr = newExpr.replaceAll("\\.geq\\.", ">="); // TODO, use strings from
		// GreaterThanOrEqualsNode
		// in jLEMS
		newExpr = newExpr.replaceAll("\\.gt\\.", ">");
		newExpr = newExpr.replaceAll("\\.leq\\.", "<=");
		newExpr = newExpr.replaceAll("\\.lt\\.", "<=");
		newExpr = newExpr.replaceAll("\\.eq\\.", "==");
		newExpr = newExpr.replaceAll("\\.neq\\.", "!=");
		newExpr = newExpr.replaceAll("\\.and.", "&&");

		newExpr = newExpr.replaceAll(" ln\\(", " log(");
		newExpr = newExpr.replaceAll(" random\\(", " random_float(");

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

		for(String origName : paramMappingsComp.keySet())
		{
			String newName = paramMappingsComp.get(origName);
			newExpr = Utils.replaceInExpression(newExpr, origName, newName);
		}

		// Since modlunit is confused about statements with: (100) * v
		// It assumes the 100 is a scaling factor for units & complains
		// Change to: 100 * v
		if(newExpr.charAt(0) == '(')
		{
			int nextBracket = newExpr.indexOf(")");
			String num = newExpr.substring(1, nextBracket).trim();
			try
			{
				float f = Float.parseFloat(num);
				newExpr = f + " " + newExpr.substring(nextBracket + 1);
				// System.out.println("--------------------num: "+num+", f "+f+", newExpr "+newExpr);
			}
			catch(NumberFormatException e)
			{
			}

		}

		return newExpr;
	}

	public static String getMechanismName(Component comp, String popName)
	{
		return String.format("m_%s_%s", comp.getID(), popName);
	}

	protected static String getNeuronUnit(String dimensionName)
	{

		if(dimensionName == null)
		{
			return ": no units???";
		}

		if(dimensionName.equals("voltage"))
		{
			return "(mV)";
		}
		else if(dimensionName.equals("per_voltage"))
		{
			return "(/mV)";
		}
		else if(dimensionName.equals("conductance"))
		{
			return "(uS)";
		}
		else if(dimensionName.equals("capacitance"))
		{
			return "(microfarads)";
		}
		else if(dimensionName.equals("time"))
		{
			return "(ms)";
		}
		else if(dimensionName.equals("per_time"))
		{
			return "(kHz)";
		}
		else if(dimensionName.equals("current"))
		{
			return "(nA)";
		}
		else if(dimensionName.equals("currentDensity"))
		{
			return "(nA / um2)";
		}
		else if(dimensionName.equals("length"))
		{
			return "(um)";
		}
		else if(dimensionName.equals("area"))
		{
			return "(um2)";
		}
		else if(dimensionName.equals("volume"))
		{
			return "(um3)";
		}
		else if(dimensionName.equals("resistance"))
		{
			return "(Mohm)";
		}
		else if(dimensionName.equals("concentration"))
		{
			return "(mM)";
		}
		else if(dimensionName.equals("charge_per_mole"))
		{
			return "(C / umol)";
		}
		else if(dimensionName.equals("temperature"))
		{
			return "(K)";
		}
		else if(dimensionName.equals("idealGasConstantDims"))
		{
			return "(millijoule / K)";
		}
		else if(dimensionName.equals("rho_factor"))
		{
			return "(mM m2 /A /s)";
		}
		else if(dimensionName.equals("conductance_per_voltage"))
		{
			return "(uS / mV)";
		}
		else if(dimensionName.equals(Dimension.NO_DIMENSION))
		{
			return "";
		}
		else
		{
			return "Don't know units for : (" + dimensionName + ")";
		}
	}

	protected static float convertToNeuronUnits(String neuromlQuantity, Lems lems) throws ParseError, ContentError
	{
		DimensionalQuantity dq = QuantityReader.parseValue(neuromlQuantity, lems.getUnits());
		return convertToNeuronUnits((float) dq.getDoubleValue(), dq.getDimension().getName());
	}

	protected static float convertToNeuronUnits(float val, String dimensionName)
	{
		float newVal = val * getNeuronUnitFactor(dimensionName);
		return newVal;
	}

	protected static float getNeuronUnitFactor(String dimensionName)
	{

		if(dimensionName.equals("none"))
		{
			return 1f;
		}
        else if(dimensionName.equals("voltage"))
		{
			return 1000f;
		}
		else if(dimensionName.equals("per_voltage"))
		{
			return 0.001f;
		}
		else if(dimensionName.equals("conductance"))
		{
			return 1000000f;
		}
		else if(dimensionName.equals("capacitance"))
		{
			return 1e6f;
		}
		else if(dimensionName.equals("per_time"))
		{
			return 0.001f;
		}
		else if(dimensionName.equals("current"))
		{
			return 1e9f;
		}
		else if(dimensionName.equals("currentDensity"))
		{
			return 1e-3f;
		}
		else if(dimensionName.equals("conductanceDensity"))
		{
			return 1e-4f;
		}
		else if(dimensionName.equals("time"))
		{
			return 1000f;
		}
		else if(dimensionName.equals("length"))
		{
			return 1000000f;
		}
		else if(dimensionName.equals("area"))
		{
			return 1e12f;
		}
		else if(dimensionName.equals("volume"))
		{
			return 1e18f;
		}
		else if(dimensionName.equals("resistance"))
		{
			return 1e-6f;
		}
		else if(dimensionName.equals("concentration"))
		{
			return 1f;
		}
		else if(dimensionName.equals("charge_per_mole"))
		{
			return 1e-6f;
		}
		else if(dimensionName.equals("idealGasConstantDims"))
		{
			return 1000f;
		}
		else if(dimensionName.equals("rho_factor"))
		{
			return 1f;
		}
		else if(dimensionName.equals("conductance_per_voltage"))
		{
			return 1000f;
		}
		else if(dimensionName.equals("temperature"))
		{
			return 1f;
		}
        else {
            return Float.NaN;
        }
	}

	protected static String getDerivativeUnit(String dimensionName)
	{
		String unit = getNeuronUnit(dimensionName);
		if(unit.equals(""))
		{
			return "(/ms)";
		}
		else
		{
			return unit.replaceAll("\\)", "/ms)");
		}
	}

}
