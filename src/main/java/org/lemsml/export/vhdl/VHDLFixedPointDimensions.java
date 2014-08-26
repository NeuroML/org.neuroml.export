package org.lemsml.export.vhdl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

public class VHDLFixedPointDimensions {

	

	public static void writeBitLengths(JsonGenerator g, String dimension) throws JsonGenerationException, IOException
	{
		String[] splitDims = dimension.replace("per_time","pertime").split("_");
		if (splitDims.length == 1)
		{
			g.writeStringField("integer",getBitLengthInteger(dimension).toString());
			g.writeStringField("fraction",getBitLengthFraction(dimension).toString());
		}
		else
			if (splitDims.length == 2 && splitDims[1].matches("inv"))
			{
				Integer inte = 0-getBitLengthFraction(splitDims[0]);
				Integer fract = 0-getBitLengthInteger(splitDims[0]);
				g.writeStringField("integer",inte.toString());
				g.writeStringField("fraction",fract.toString());
			} else
				if (splitDims.length == 3 && splitDims[1].matches("div"))
				{
					Integer inte1 = 0-getBitLengthFraction(splitDims[0]);
					Integer fract1 = 0-getBitLengthInteger(splitDims[0]);
					Integer inte2 = 0-getBitLengthFraction(splitDims[2]);
					Integer fract2 = 0-getBitLengthInteger(splitDims[2]);
					Integer inte = inte1 + inte2;
					Integer fract = fract1 + fract2;
					g.writeStringField("integer",inte.toString());
					g.writeStringField("fraction",fract.toString());
				}
			
	}
	
	public static Integer getBitLengthFraction(String dimension)
	{
		int fract = 0;
		if (dimension == null || dimension.equals("none"))
		{
			 fract = -16;
		} else if (dimension.equals("voltage"))
		{
			 fract = -24;
		}  else if (dimension.equals("current")) //todo figure out what the ideal bitwidth for current is
		{
			 fract = -54;
		} else if (dimension.equals("time"))
		{
			 fract = -24;
		} 	else if (dimension.equals("capacitance"))
		{
			 fract = -47;
		} else if (dimension.equals("conductance"))
		{
			 fract =  -53;
		} else if (dimension.equals("per_time"))
		{
			 fract = -2;
		} 
		
		return fract;
	}

	public static Integer getBitLengthInteger(String dimension)
	{
		int integer = 0;
		if (dimension == null || dimension.equals("none"))
		{
			integer = 18;
		} else if (dimension.equals("voltage"))
		{
			integer = 2;
		}  else if (dimension.equals("current")) //todo figure out what the ideal bitwidth for current is
		{
			integer = -28;
		} else if (dimension.equals("time"))
		{
			integer = 6;
		} else if (dimension.equals("capacitance"))
		{
			integer = -33;
		} else if (dimension.equals("conductance"))
		{
			integer = -20;
		} else if (dimension.equals("per_time"))
		{
			integer = 20;
		} 
		return integer;
	}

}
