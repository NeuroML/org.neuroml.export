package org.lemsml.export.vhdl;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.edlems.EDSignal;

public class VHDLFixedPointDimensions {

	

	public static void writeBitLengths(EDSignal edSignal, String dimension) throws JsonGenerationException, IOException
	{
		String[] splitDims = dimension.replace("per_time","pertime").split("_");
		if (splitDims.length == 1)
		{
			edSignal.integer = (getBitLengthInteger(dimension));
			edSignal.fraction= (getBitLengthFraction(dimension));
		}
		else
			if (splitDims.length == 2 && splitDims[1].matches("inv"))
			{
				Integer inte = 0-getBitLengthFraction(splitDims[0]);
				Integer fract = 0-getBitLengthInteger(splitDims[0]);
				edSignal.integer = inte;
				edSignal.fraction= fract;
			} else
				if (splitDims.length == 3 && splitDims[1].matches("div"))
				{
					Integer inte1 = 0-getBitLengthFraction(splitDims[0]);
					Integer fract1 = 0-getBitLengthInteger(splitDims[0]);
					Integer inte2 = 0-getBitLengthFraction(splitDims[2]);
					Integer fract2 = 0-getBitLengthInteger(splitDims[2]);
					Integer inte = inte1 + inte2;
					Integer fract = fract1 + fract2;
					edSignal.integer= inte;
					edSignal.fraction= fract;
					
					//override to keep within 32 bits for everything

					edSignal.integer= 18;
					edSignal.fraction= -13;
				}
			
	}
	
	public static Integer getBitLengthFraction(String dimension)
	{
		int fract = 0;
		if (dimension == null || dimension.equals("none"))
		{
			 fract = -13;//-16
		} else if (dimension.equals("voltage"))
		{
			 fract = -22;//-24
		}  else if (dimension.equals("current")) //todo figure out what the ideal bitwidth for current is
		{
			 fract = -53;//-54
		} else if (dimension.equals("time"))
		{
			 fract = -18;//-24
		} else if (dimension.equals("capacitance"))
		{
			 fract = -47;//-47
		} else if (dimension.equals("conductance"))
		{
			 fract =  -53;//-53
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
			integer = 18;//18
		} else if (dimension.equals("voltage"))
		{
			integer = 2;
		}  else if (dimension.equals("current")) //todo figure out what the ideal bitwidth for current is
		{
			integer = -28;//-28
		} else if (dimension.equals("time"))
		{
			integer = 6;//6
		} else if (dimension.equals("capacitance"))
		{
			integer = -33;//-33
		} else if (dimension.equals("conductance"))
		{
			integer = -22;//-20
		} else if (dimension.equals("per_time"))
		{
			integer = 18;//20
		} 
		return integer;
	}

}
