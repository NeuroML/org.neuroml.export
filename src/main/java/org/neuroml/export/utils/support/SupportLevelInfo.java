package org.neuroml.export.utils.support;

import java.util.HashMap;
import java.util.Map;

import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.model.util.NeuroMLException;

/**
 * 
 * @author padraig
 */
public class SupportLevelInfo
{

    private final HashMap<Format, HashMap<ModelFeature, Level>> support = new HashMap<Format, HashMap<ModelFeature, Level>>();

    private static SupportLevelInfo myInstance = null;

    public static final String SUPPORTED = "Supported";

    public enum Level
    {
        OUTSIDE_CURRENT_SCOPE, /* Not supported and unlikely to be any time soon, due to known limitations of target simulator/format */
        NONE,                  /* Not supported yet, but possibly could be in future*/
        LOW,                   /* Supported format, but with known shortcomings */
        MEDIUM,                /* Supported format, tested with a number of examples */
        HIGH;                  /* Supported format, well tested */

        public boolean isSupported()
        {
            return !(this == OUTSIDE_CURRENT_SCOPE || this == NONE);
        }
    }

    private SupportLevelInfo()
    {
        /* Add info on formats supported in jLEMS */
        addSupportInfo(Format.LEMS, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.NETWORK_WITH_ANALOG_CONNS_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        addSupportInfo(Format.LEMS, ModelFeature.CHANNEL_POPULATIONS_CELL_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.CHANNEL_DENSITY_ON_SEGMENT, SupportLevelInfo.Level.NONE);
        addSupportInfo(Format.LEMS, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.HIGH);
        addSupportInfo(Format.LEMS, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.MEDIUM);
    }

    public static SupportLevelInfo getSupportLevelInfo()
    {
        if(myInstance == null) myInstance = new SupportLevelInfo();

        return myInstance;
    }

    public final void addSupportInfo(Format format, ModelFeature mf, Level level)
    {
        if(!support.containsKey(format))
        {
            support.put(format, new HashMap<ModelFeature, Level>());
        }
        HashMap<ModelFeature, Level> ht = support.get(format);
        ht.put(mf, level);
    }

    public String isSupported(Format format, ModelFeature mf)
    {
        if(!support.containsKey(format)) return "Unknown format: " + format;
        else
        {
            HashMap<ModelFeature, Level> ht = support.get(format);
            if(!ht.containsKey(mf))
            {
                return "No information about whether " + format + " supports " + mf.name();
            }
            else
            {
                Level l = ht.get(mf);
                if(l.isSupported())
                {
                    return SUPPORTED;
                }
                else
                {
                    return "Level of support for " + mf.name() + " in " + format + " is insufficient: " + l;
                }
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Format, HashMap<ModelFeature, Level>> format : support.entrySet())
        {
            sb.append("Format: " + format.getKey() + "\n");
            HashMap<ModelFeature, Level> mfVsLevels = format.getValue();

            for(ModelFeature mf: ModelFeature.values())
            {
                if (mfVsLevels.containsKey(mf)) 
                {
                    Level l = mfVsLevels.get(mf);
                    sb.append("    " + (l.isSupported() ? '\u2713' : "x") + " " + mf + ": " + l + "\n");
                } 
                else if (!mf.equals(ModelFeature.ALL))
                {
                    sb.append("    " + "x" + " " + mf + ": " + Level.NONE + "\n");
                }
            }
        }
        return sb.toString();
    }

    public void checkConversionSupported(Format format, Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        boolean passed = true;
        if(!myInstance.isSupported(format, ModelFeature.ALL).equals(SUPPORTED))
        {
            StringBuilder report = new StringBuilder();
            for(ModelFeature mf : ModelFeature.analyseModelFeatures(lems))
            {
                String supp = myInstance.isSupported(format, mf);
                if(!supp.equals(SUPPORTED))
                {
                    passed = false;
                    report.append("Feature not supported in " + format + ": " + mf + "\n    " + myInstance.isSupported(format, mf) + "\n");
                }
            }
            if(!passed)
            {
                report.append("\nInfo on supported features:\n" + myInstance);
                throw new ModelFeatureSupportException(report.toString());
            }
        }

    }

    public static void main(String[] args)
    {
        SupportLevelInfo sli = getSupportLevelInfo();

        sli.addSupportInfo(Format.NEURON, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo(Format.NEURON, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(Format.SBML, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);

        System.out.println(sli);

        test(Format.NEURON, ModelFeature.NETWORK_MODEL);
        test(Format.NEURON_A, ModelFeature.NETWORK_MODEL);
        test(Format.NEURON_A, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL);
        test(Format.SBML, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL);

        System.out.println("\nSummary of all:\n");
        System.out.println(sli);

    }

    private static void test(Format format, ModelFeature mf)
    {
        System.out.println("\nTesting " + format + " for support of: " + mf);
        SupportLevelInfo sli = getSupportLevelInfo();

        System.out.println(sli.isSupported(format, mf));
    }

}
