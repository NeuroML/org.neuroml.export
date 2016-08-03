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
    TYPE, 
    TITLE, 
    PARAMETERS, 
    STATE, 
    STATE_FUNCTIONS, 
    T_END, 
    T_START, 
    COMMENT, 
    DUMP_TO_FILE, 
    OUTPUT_FILE, 
    FILE_NAME, 
    OUTPUT_COLUMNS, 
    VARIABLE, 
    DISPLAY, 
    ABSCISSA_AXIS, 
    MIN, MAX, 
    ORDINATE_AXIS, 
    CURVES, 
    ABSCISSA, 
    ORDINATE, 
    POPULATION, 
    POPULATION_INDEX, 
    SEGMENT_ID, 
    SEGMENT_NAME, 
    FRACTION_ALONG, 
    COLOUR, 
    POPULATIONS, 
    SIZE, 
    SYNAPSES,
    SYNAPSE,
    COMPONENT,
    PROJECTIONS, 
    PRE_POPULATION, 
    POST_POPULATION, 
    CONNECTIONS,
    PRE_CELL_ID, 
    POST_CELL_ID, 
    WEIGHT, 
    DELAY, 
    INPUTS;
    
    

	public String get()
	{
		return this.toString().toLowerCase();
	}
}
