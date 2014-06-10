
package org.neuroml.export;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author padraig
 */
public class SupportLevel {
    
    public ModelFeature modelFeature;
    public Level level;
    
    public enum Level {
        OUT_OF_SCOPE,
        NONE,
        LOW,
        MEDIUM,
        HIGH;
    }
    
    public SupportLevel(ModelFeature modelFeature, Level level) {
        this.modelFeature = modelFeature;
        this.level = level;
    }
    
}
