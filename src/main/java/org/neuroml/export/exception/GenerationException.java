package org.neuroml.export.exception;

/**
 * 
 * @author padraig
 */
public class GenerationException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5568959520375097692L;

	public GenerationException(String comment, Throwable t)
	{
		super(comment, t);
	}

	public GenerationException(String comment)
	{
		super(comment);
	}

}
