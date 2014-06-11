
package org.neuroml.export;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author padraig
 */
public class SupportLevelInfo {
    
    private Hashtable<String, Hashtable<ModelFeature, Level>> support = new Hashtable<String, Hashtable<ModelFeature, Level>>();
    
    private static SupportLevelInfo myInstance = null;
    
    public static final String SUPPORTED = "Supported";
    
    public enum Level {
        OUT_OF_SCOPE,
        NONE,
        LOW,
        MEDIUM,
        HIGH;
        
        public boolean isSupported() {
            return !(this==OUT_OF_SCOPE || this==NONE);
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
            support.put(format, new Hashtable<ModelFeature, Level>());
        }
        Hashtable<ModelFeature, Level> ht = support.get(format);
        ht.put(mf, level);
    }
    
    public String isSupported(String format, ModelFeature mf) {
        if (!support.containsKey(format))
            return "Unknown format: "+format;
        else {
            Hashtable<ModelFeature, Level> ht = support.get(format);
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
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (String format: support.keySet()) {
            sb.append("Format: "+format+"\n");
            Hashtable<ModelFeature, Level> ht = support.get(format);
            for (ModelFeature mf: ht.keySet()) {
                Level l = ht.get(mf);
                sb.append("    "+mf+": "+l+" (supported: "+l.isSupported()+")\n");
            }
        }
        return sb.toString();
    }
    
    
    public static void main(String[] args)  {
        SupportLevelInfo sli = getSupportLevelInfo();
        
        sli.addSupportInfo("NEURON", ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.HIGH);
        sli.addSupportInfo("NEURON", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo("SBML", ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.OUT_OF_SCOPE);
        
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
