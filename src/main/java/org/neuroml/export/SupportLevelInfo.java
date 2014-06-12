
package org.neuroml.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.bind.JAXBException;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;

/**
 *
 * @author padraig
 */
public class SupportLevelInfo {
    
    private HashMap<String, HashMap<ModelFeature, Level>> support = new HashMap<String, HashMap<ModelFeature, Level>>();
    
    private static SupportLevelInfo myInstance = null;
    
    public static final String SUPPORTED = "Supported";
    
    public enum Level {
        OUTSIDE_CURRENT_SCOPE,
        NONE,
        LOW,
        MEDIUM,
        HIGH;
        
        public boolean isSupported() {
            return !(this==OUTSIDE_CURRENT_SCOPE || this==NONE);
        }
    }
    
    private SupportLevelInfo() {
    }
    
    public static SupportLevelInfo getSupportLevelInfo()
    {
        if (myInstance==null) myInstance = new SupportLevelInfo();
        
        return myInstance;
    }
    
    public void addSupportInfo(String format, ModelFeature mf, Level level) {
        if (!support.containsKey(format))
        {
            support.put(format, new HashMap<ModelFeature, Level>());
        }
        HashMap<ModelFeature, Level> ht = support.get(format);
        ht.put(mf, level);
    }
    
    public String isSupported(String format, ModelFeature mf) {
        if (!support.containsKey(format))
            return "Unknown format: "+format;
        else {
            HashMap<ModelFeature, Level> ht = support.get(format);
            if (!ht.containsKey(mf)){
                return "No information about whether "+format+" supports "+mf.name();
            } else {
                Level l = ht.get(mf);
                if (l.isSupported()) {
                    return SUPPORTED;
                } else {
                    return "Level of support for "+mf.name()+" in "+format+" is insufficient: "+l;
                }
            }
        }
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (String format: support.keySet()) {
            sb.append("Format: "+format+"\n");
            HashMap<ModelFeature, Level> ht = support.get(format);
            for (ModelFeature mf: ht.keySet()) {
                Level l = ht.get(mf);
                sb.append("    "+mf+": "+l+"\n");
            }
        }
        return sb.toString();
    }
    
    public void checkAllFeaturesSupported(String format, Lems lems) throws ContentError, ParseError, IOException, JAXBException, ModelFeatureSupportException {
        boolean passed = true;
        StringBuilder report = new StringBuilder();
        for (ModelFeature mf: ModelFeature.analyseModelFeatures(lems)){
            String supp = myInstance.isSupported(format, mf);
            if (!supp.equals(SUPPORTED)) {
                passed = false;
                report.append("Feature not supported in "+format+": "+mf+"\n    "+myInstance.isSupported(format, mf)+"\n");
            }
        };
        if (!passed) {
            report.append("Info on supported features:\n"+myInstance);
            throw new ModelFeatureSupportException(report.toString());
        }
        
        
    }
    
    
    public static void main(String[] args)  {
        SupportLevelInfo sli = getSupportLevelInfo();
        
        sli.addSupportInfo("NEURON", ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo("NEURON", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo("SBML", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
        
        System.out.println(sli);
        
        test("NEURON", ModelFeature.NETWORK_MODEL);
        test("NEURONa", ModelFeature.NETWORK_MODEL);
        test("NEURONa", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL);
        test("SBML", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL);
        
    }    
    
    private static void test(String format, ModelFeature mf) {
        System.out.println("\nTesting "+format+" for support of: "+mf);
        SupportLevelInfo sli = getSupportLevelInfo();
        
        System.out.println(sli.isSupported(format, mf));
    }
    
    
}
