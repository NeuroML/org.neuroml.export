package org.lemsml.export.base;

/**
 *
 * @author padraig
 */
public class GenerationException extends Exception {
    
    private GenerationException() {
        
    }
    
    public GenerationException(String comment, Throwable t)
    {
        super(comment, t);
    }
    public GenerationException(String comment)
    {
        super(comment);
    }
    
}
