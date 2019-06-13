package org.lemsml.export.dlems;

/**
 * @author matteocantarelli
 * 
 */
public enum DLemsKeywords
{
	DT, 
	SEED, 
	REPORT_FILE, 
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
    SPIKE_FILE, 
    SPIKE_FILE_FORMAT, 
    EVENT_SELECTIONS, 
    EVENT_SELECTION_ID, 
    SELECT, 
    EVENT_PORT, 
    FILE_NAME, 
    OUTPUT_COLUMNS, 
    VARIABLE, 
    QUANTITY, 
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
    INPUTS,
    
    
    TEMPERATURE,
    NEURON_VARIABLE_NAME,
    NEURON_MECHANISM_NAME,
    NEURON_SECTION_NAME,
    NEURON_FRACT_ALONG,
    NEURON_VARIABLE_SCALE,
    
    EXPORT_LIBRARY_VERSION;

	public String get()
	{
		return this.toString().toLowerCase();
	}
}
