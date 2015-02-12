package org.neuroml.export.utils;

import java.io.*;
import org.lemsml.jlems.core.logging.E;

/**
 * 
 * @author Padraig Gleeson
 * 
 */

public class ProcessOutputWatcher extends Thread
{

	private InputStreamReader inputStrReader = null;

	private String referenceName = null;

	private StringBuffer log = new StringBuffer();

	public ProcessOutputWatcher(InputStream inputStr, String referenceName)
	{
		this.inputStrReader = new InputStreamReader(inputStr);
		this.referenceName = referenceName;
	}

	public String getLog()
	{
		return log.toString();
	}

	@Override
	public void run()
	{
		try
		{
			// int numberOfBytesRead;

			BufferedReader br = new BufferedReader(inputStrReader);
			String line = null;
			while((line = br.readLine()) != null)
			{
				E.info(referenceName + "> " + line);
				log.append(line + "\n");
			}
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
