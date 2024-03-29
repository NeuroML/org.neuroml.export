/**
 *
 */
package org.neuroml.export.neuron;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemsml.export.dlems.UnitConverter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.QuantityReader;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author Boris Marin, Padraig Gleeson
 *
 */
public class NRNUtils implements UnitConverter
{

    final static String NEURON_VOLTAGE = "v";
    final static String NEURON_TEMP = "celsius";
    //final static String RESERVED_STATE_SUFFIX = "_";
    final static String V_CURRENT_SUFFIX = "_I";
    final static String RATE_PREFIX = "rate_";
    final static String REGIME_PREFIX = "regime_";
    //final static String V_COPY_PREFIX = "copy_";

    final static String[] NON_NRN_STATE_VARS
        = new String[]{"weightFactor","isi","nextIsi","lastSpikeTime","nextSpikeTemp","nextSpike"};

    final static String caConc = "caConc";
    final static String vShift = "vShift";

    final static String comm = "# ";
    final static String commPre = "'''";
    final static String commPost = "'''";

    static final int commentOffset = 40;

    static final String LEN_UNIT = "cm";
    static final float LEN_CONVERSION = 1e2f;


    static final String generalUnits = "\n(nA) = (nanoamp)\n"
        + "(uA) = (microamp)\n"
        + "(mA) = (milliamp)\n"
        + "(A) = (amp)\n"
        + "(mV) = (millivolt)\n"
        + "(mS) = (millisiemens)\n"
        + "(uS) = (microsiemens)\n"
        + "(nF) = (nanofarad)\n"
        + "(molar) = (1/liter)\n"
        + "(kHz) = (kilohertz)\n"
        + "(mM) = (millimolar)\n"
        + "(um) = (micrometer)\n"
        + "(umol) = (micromole)\n"
        + "(pC) = (picocoulomb)\n"
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

    static final String ghk2FunctionDefs = "\nFUNCTION ghk2(v(mV), ci(mM), co(mM)) (mV) {\n" +
                "        LOCAL nu,f\n" +
                "\n" +
                "        f = KTF(celsius)/2\n" +
                "        nu = v/f\n" +
                "        ghk2=-f*(1. - (ci/co)*exp(nu))*efun(nu)\n" +
                "}\n" +
                "\n" +
                "FUNCTION KTF(celsius (DegC)) (mV) {\n" +
                "        KTF = ((25./293.15)*(celsius + 273.15))\n" +
                "}\n" +
                "\n" +
                "FUNCTION efun(z) {\n" +
                "	if (fabs(z) < 1e-4) {\n" +
                "		efun = 1 - z/2\n" +
                "	}else{\n" +
                "		efun = z/(exp(z) - 1)\n" +
                "	}\n" +
                "}\n";

    static final String randomFunctionDefs = "\n: Returns a float between 0 and max; implementation of random() as used in LEMS\n"
        + "FUNCTION random_float(max) {\n"
        + "    \n"
        + "    : This is not ideal, getting an exponential dist random number and then turning back to uniform\n"
        + "    : However this is the easiest what to ensure mod files with random methods fit into NEURON's\n"
        + "    : internal framework for managing internal number generation.\n"
        + "    random_float = exp(-1*erand())*max\n"
        + "    \n"
        + "}\n"
        + ""
        + "\n:****************************************************\n"
        + ": Methods copied from netstim.mod in NEURON source"
        + "\n\n"
        + " "
        + "\n" +
        "PROCEDURE seed(x) {\n" +
        "	set_seed(x)\n" +
        "}\n\n"
        + ""
        + "VERBATIM\n" +
        "double nrn_random_pick(void* r);\n" +
        "void* nrn_random_arg(int argpos);\n" +
        "ENDVERBATIM\n" +
        "\n\n" +
        "FUNCTION erand() {\n" +
        "VERBATIM\n" +
        "	if (_p_donotuse) {\n" +
        "		/*\n" +
        "		:Supports separate independent but reproducible streams for\n" +
        "		: each instance. However, the corresponding hoc Random\n" +
        "		: distribution MUST be set to Random.negexp(1)\n" +
        "		*/\n" +
        "		_lerand = nrn_random_pick(_p_donotuse);\n" +
        "	}else{\n" +
        "		/* only can be used in main thread */\n" +
        "		if (_nt != nrn_threads) {\n" +
        "           hoc_execerror(\"multithread random in NetStim\",\" only via hoc Random\");\n" +
        "		}\n" +
        "ENDVERBATIM\n" +
        "		: the old standby. Cannot use if reproducible parallel sim\n" +
        "		: independent of nhost or which host this instance is on\n" +
        "		: is desired, since each instance on this cpu draws from\n" +
        "		: the same stream\n" +
        "		erand = exprand(1)\n" +
        /*"       printf(\"- Calling erand: %g, %g \\n\",erand,exp(-1*erand))\n" +*/
        "VERBATIM\n" +
        "	}\n" +
        "ENDVERBATIM\n" +
        "}\n" +
        "\n" +
        "PROCEDURE noiseFromRandom() {\n" +
        "VERBATIM\n" +
        " {\n" +
        "	void** pv = (void**)(&_p_donotuse);\n" +
        "	if (ifarg(1)) {\n" +
        "		*pv = nrn_random_arg(1);\n" +
        "	}else{\n" +
        "		*pv = (void*)0;\n" +
        "	}\n" +
        " }\n" +
        "ENDVERBATIM\n" +
        "}\n\n"
        + ": End of methods copied from netstim.mod in NEURON source\n"
        + ":****************************************************\n"
        + ""
        + ""
        + "\n";

    static final String heavisideFunctionDefs = "\n: The Heaviside step function\nFUNCTION H(x) {\n"
        + "    \n"
        + "    if (x < 0) { H = 0 }\n"
        + "    else if (x > 0) { H = 1 }\n"
        + "    else { H = 0.5 }\n"
        + "    \n"
        + "}\n\n";

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

        if (NRNKeywords.contains(id))
        {
            suffix = "_nml2";
        }

        return id + suffix;
    }

    protected static String checkCommentLineLength(String comment)
    {
        int maxLength = 500;
        if (comment.length()<=maxLength)
            return comment;
        else
            return comment.substring(0,maxLength-3)+"...";
    }

    protected static String getStateVarName(String sv)
    {
        if (sv.equals(NRNUtils.NEURON_VOLTAGE))
        {
            return NRNUtils.NEURON_VOLTAGE + NRNUtils.V_CURRENT_SUFFIX;
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

    protected static float getThreshold(Component comp, Lems lems) throws ParseError, ContentError, LEMSException
    {
        float threshold = 0;
        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) ||
           comp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
        {
            threshold = NRNUtils.convertToNeuronUnits(comp.getStringValue("thresh"), lems);
        }
        else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_PYNN_CELL))
        {
            if ( (comp.getComponentType().isOrExtends("EIF_cond_alpha_isfa_ista") || comp.getComponentType().isOrExtends("EIF_cond_exp_isfa_ista")))
            {
                if (NRNUtils.convertToNeuronUnits(comp.getStringValue("delta_T"), lems)==0 )
                {
                    threshold = NRNUtils.convertToNeuronUnits(comp.getStringValue("v_thresh"), lems);
                }
                else
                {
                    threshold = NRNUtils.convertToNeuronUnits(comp.getStringValue("v_spike"), lems);
                }
            }
            else
            {
                threshold = NRNUtils.convertToNeuronUnits(comp.getStringValue("v_thresh"), lems);
            }
        }
        return threshold;
    }

    protected static String checkForStateVarsAndNested(String expr, Component comp, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings)
    {

        if (expr == null)
        {
            return null;
        }

        String newExpr = expr.trim();

        newExpr = newExpr.replaceAll("\\.geq\\.", ">="); // TODO, use strings from
        // GreaterThanOrEqualsNode
        // in jLEMS
        newExpr = newExpr.replaceAll("\\.gt\\.", ">");
        newExpr = newExpr.replaceAll("\\.leq\\.", "<=");
        newExpr = newExpr.replaceAll("\\.lt\\.", "<");
        newExpr = newExpr.replaceAll("\\.eq\\.", "==");
        newExpr = newExpr.replaceAll("\\.neq\\.", "!=");
        newExpr = newExpr.replaceAll("\\.and.", "&&");

        newExpr = newExpr.replaceAll(" ln\\(", " log(");
        newExpr = newExpr.replaceAll("random\\(", "random_float(");

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        for (String origName : paramMappingsComp.keySet())
        {
            String newName = paramMappingsComp.get(origName);
            newExpr = Utils.replaceInExpression(newExpr, origName, newName);
        }

        // Since modlunit is confused about statements with: (100) * v
        // It assumes the 100 is a scaling factor for units & complains
        // Change to: 100 * v
        if (newExpr.charAt(0) == '(')
        {
            int nextBracket = newExpr.indexOf(")");
            String num = newExpr.substring(1, nextBracket).trim();
            try
            {
                float f = Float.parseFloat(num);
                newExpr = f + " " + newExpr.substring(nextBracket + 1);
                // System.out.println("--------------------num: "+num+", f "+f+", newExpr "+newExpr);
            }
            catch (NumberFormatException e)
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

        if (dimensionName == null)
        {
            return ": no units???";
        }

        if (dimensionName.equals("voltage"))
        {
            return "(mV)";
        }
        else if (dimensionName.equals("per_voltage"))
        {
            return "(/mV)";
        }
        else if (dimensionName.equals("per_voltage2"))
        {
            return "(/mV2)";
        }
        else if (dimensionName.equals("conductance"))
        {
            return "(uS)";
        }
        else if (dimensionName.equals("capacitance"))
        {
            return "(nF)";
        }
        else if (dimensionName.equals("specificCapacitance"))
        {
            return "(microfarads / um2)";
        }
        else if (dimensionName.equals("time"))
        {
            return "(ms)";
        }
        else if (dimensionName.equals("per_time"))
        {
            return "(kHz)";
        }
        else if (dimensionName.equals("current"))
        {
            return "(nA)";
        }
        else if (dimensionName.equals("currentDensity"))
        {
            return "(nA / cm2)";
        }
        else if (dimensionName.equals("current_per_time"))
        {
            return "(nA / ms)";
        }
        else if (dimensionName.equals("conductanceDensity"))
        {
            return "(uS / cm2)";
        }
        else if (dimensionName.equals("conductanceDensity_hoc"))
        {
            return "(S / cm2)";
        }
        else if (dimensionName.equals("length"))
        {
            return "("+LEN_UNIT+")";
        }
        else if (dimensionName.equals("area"))
        {
            return "("+LEN_UNIT+"2)";
        }
        else if (dimensionName.equals("volume"))
        {
            return "("+LEN_UNIT+"3)";
        }
        else if (dimensionName.equals("resistivity"))
        {
            return "(ohm cm)";
        }
        else if (dimensionName.equals("resistance"))
        {
            return "(Mohm)";
        }
        else if (dimensionName.equals("concentration"))
        {
            return "(mM)";
        }
        else if (dimensionName.equals("charge_per_mole"))
        {
            return "(pC / umol)";
        }
        else if (dimensionName.equals("temperature"))
        {
            return "(K)";
        }
        else if (dimensionName.equals("idealGasConstantDims"))
        {
            return "(femtojoule / K / umol)";
        }
        else if (dimensionName.equals("rho_factor"))
        {
            return "(umol / cm / nA / ms)";
        }
        else if (dimensionName.equals("conductance_per_voltage"))
        {
            return "(uS / mV)";
        }
        else if (dimensionName.equals(Dimension.NO_DIMENSION))
        {
            return "";
        }
        else
        {
            return "Don't know units for : (" + dimensionName + ")";
        }
    }

    protected static float convertToNeuronUnits(String neuromlQuantity, Lems lems) throws ParseError, ContentError, LEMSException
    {
        DimensionalQuantity dq = QuantityReader.parseValue(neuromlQuantity, lems.getUnits());
        return convertToNeuronUnits((float)dq.getDoubleValue(), dq.getDimension().getName());
    }

    @Override
    public float convert(float siValue, String dimensionName) throws LEMSException
    {
        return convertToNeuronUnits(siValue, dimensionName);
    }

    protected static float convertToNeuronUnits(float siVal, String dimensionName) throws LEMSException
    {
        BigDecimal factor = new BigDecimal(getNeuronUnitFactor(dimensionName));
        BigDecimal newValB = new BigDecimal(siVal+"");
        newValB = newValB.multiply(factor);

        float newVal = newValB.floatValue();
        //System.out.println("f "+factor+" val "+siVal+"  new "+newVal+";  new "+newValB+"; dim "+dimensionName);
        return newVal;
    }

    public static float getNeuronUnitFactor(String dimensionName) throws LEMSException
    {

        if (dimensionName.equals("none"))
        {
            return 1;
        }
        else if (dimensionName.equals("voltage"))
        {
            return 1000f;
        }
        else if (dimensionName.equals("per_voltage"))
        {
            return 0.001f;
        }
        else if (dimensionName.equals("per_voltage2"))
        {
            return 1e-6f;
        }
        else if (dimensionName.equals("conductance"))
        {
            return 1e6f;
        }
        else if (dimensionName.equals("capacitance"))
        {
            return 1e9f;
        }
        else if (dimensionName.equals("specificCapacitance"))
        {
            return 1e-6f;
        }
        else if (dimensionName.equals("per_time"))
        {
            return 0.001f;
        }
        else if (dimensionName.equals("current"))
        {
            return 1e9f;
        }
        else if (dimensionName.equals("currentDensity"))
        {
            return 0.1f;
        }
        else if (dimensionName.equals("current_per_time"))
        {
            return 1e6f;
        }
        else if (dimensionName.equals("conductanceDensity"))
        {
            return 1e2f;
        }
        else if (dimensionName.equals("conductanceDensity_hoc"))
        {
            return 1e-4f;
        }
        else if (dimensionName.equals("time"))
        {
            return 1000f;
        }
        else if (dimensionName.equals("length"))
        {
            return LEN_CONVERSION;
        }
        else if (dimensionName.equals("area"))
        {
            return LEN_CONVERSION * LEN_CONVERSION;
        }
        else if (dimensionName.equals("volume"))
        {
            return LEN_CONVERSION * LEN_CONVERSION * LEN_CONVERSION;
        }
        else if (dimensionName.equals("resistance"))
        {
            return 1e-6f;
        }
        else if (dimensionName.equals("resistivity"))
        {
            return 100f;
        }
        else if (dimensionName.equals("concentration"))
        {
            return 1f;
        }
        else if (dimensionName.equals("charge_per_mole"))
        {
            return 1e6f;
        }
        else if (dimensionName.equals("idealGasConstantDims"))
        {
            return 1e9f;
        }
        else if (dimensionName.equals("rho_factor"))
        {
            return 1e-8f;
        }
        else if (dimensionName.equals("conductance_per_voltage"))
        {
            return 1000f;
        }
        else if (dimensionName.equals("temperature"))
        {
            return 1f;
        }
        else
        {
            throw new LEMSException("Dimension "+dimensionName+" is not known to NEURON (add to NRNUtils)");
        }
    }

    protected static String getDerivativeUnit(String dimensionName)
    {
        String unit = getNeuronUnit(dimensionName);
        if (unit.equals(""))
        {
            return "(/ms)";
        }
        if (dimensionName.equals("voltage"))  // special case... for rate to calculate neuron voltage/current from abstract cell
        {
            return "(mV/ms)";
        }
        else
        {
            return unit.replaceAll("\\)", "/ms)");
        }
    }

    public static boolean isPlottingSavingSynVariables(Component simCpt, boolean nogui)
    {
        boolean ipssv = false;
        if (!nogui)
        {
            for (Component dispComp : simCpt.getAllChildren())
            {
                if (dispComp.getTypeName().equals("Display"))
                {

                    for (Component lineComp : dispComp.getAllChildren())
                    {
                        if (lineComp.getTypeName().equals("Line"))
                        {
                            try
                            {
                                String quantity = lineComp.getStringValue("quantity");
                                if (quantity.indexOf(':') > 0)
                                {
                                    ipssv = true;
                                }
                            }
                            catch (ContentError ex)
                            {
                                //
                            }
                        }
                    }
                }
            }
        }

        for (Component ofComp : simCpt.getAllChildren())
        {
            if (ofComp.getTypeName().equals("OutputFile"))
            {

                for (Component colComp : ofComp.getAllChildren())
                {
                    if (colComp.getTypeName().equals("OutputColumn"))
                    {
                        try
                        {
                            String quantity = colComp.getStringValue("quantity");
                            if (quantity.indexOf(':') > 0)
                            {
                                ipssv = true;
                            }
                        }
                        catch (ContentError ex)
                        {
                            //
                        }
                    }
                }
            }
        }
        return ipssv;
    }


	public static void main(String args[]) throws LEMSException
	{
		NRNUtils nu = new NRNUtils();
        float f = 2.5e-5f;

        System.out.println("Converting "+f+" to "+nu.convert(f, "none"));
        System.out.println("Converting "+f+" to "+NRNUtils.convertToNeuronUnits(f, "none"));
        System.out.println("Converting "+f+" to "+nu.convert(f, "voltage"));
        System.out.println("Converting "+f+" to "+nu.convert(f, "time"));
        System.out.println("Converting "+f+" to "+NRNUtils.convertToNeuronUnits(f, "voltage"));
        System.out.println("Converting "+f+" to "+NRNUtils.convertToNeuronUnits(f, "time"));
        System.out.println("Converting "+f+" to "+NRNUtils.convertToNeuronUnits(f, "conductance"));


	}

}
