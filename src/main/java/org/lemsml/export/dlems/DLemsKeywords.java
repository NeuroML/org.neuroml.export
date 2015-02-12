package org.lemsml.export.dlems;

/**
 * @author matteocantarelli
 * 
 */
public enum DLemsKeywords
{
	DT, DYNAMICS, EVENTS, CONDITION, DIRECTION, EFFECT, NAME, PARAMETERS, STATE, STATE_FUNCTIONS, T_END, T_START, COMMENT, DUMP_TO_FILE, DISPLAY, ABSCISSA_AXIS, MIN, MAX, ORDINATE_AXIS, CURVES, ABSCISSA, ORDINATE, COLOUR;

	public String get()
	{
		return this.toString().toLowerCase();
	}
}
