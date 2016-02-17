package org.lemsml.export.dlems;

/**
 * @author matteocantarelli
 * 
 */
public enum DLemsKeywords
{
	DT, 
    DYNAMICS, 
    EVENTS, 
    CONDITION, 
    DIRECTION, 
    EFFECT, 
    NAME, 
    TITLE, 
    PARAMETERS, 
    STATE, 
    STATE_FUNCTIONS, 
    T_END, 
    T_START, 
    COMMENT, 
    DUMP_TO_FILE, 
    DISPLAY, 
    ABSCISSA_AXIS, 
    MIN, MAX, 
    ORDINATE_AXIS, 
    CURVES, 
    ABSCISSA, 
    ORDINATE, 
    POPULATION, 
    POPULATION_INDEX, 
    COLOUR, 
    POPULATIONS, 
    SIZE, 
    SYNAPSES,
    SYNAPSE,
    COMPONENT,
    INPUTS;
    
    

	public String get()
	{
		return this.toString().toLowerCase();
	}
}
