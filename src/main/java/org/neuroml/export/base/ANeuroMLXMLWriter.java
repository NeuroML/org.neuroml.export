package org.neuroml.export.base;

import java.io.File;
import java.util.HashMap;

import org.lemsml.jlems.core.eval.DoubleEvaluator;
import org.lemsml.jlems.core.expression.*;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public abstract class ANeuroMLXMLWriter extends ANeuroMLBaseWriter
{

	HashMap<Integer, Integer> indentCounts = new HashMap<Integer, Integer>();

	Integer DEFAULT_INDENT_FLAG = -1;

	public static final String INDENT = "    ";

	String commPre = "<!--";
	String commPost = "-->";

	public ANeuroMLXMLWriter(Lems lems, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format);
		indentCounts.put(DEFAULT_INDENT_FLAG, 0);
	}

	public ANeuroMLXMLWriter(Lems lems, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format, outputFolder, outputFileName);
		indentCounts.put(DEFAULT_INDENT_FLAG, 0);
	}

	public ANeuroMLXMLWriter(Lems lems, NeuroMLDocument nmlDocument, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, nmlDocument, format, outputFolder, outputFileName);
		indentCounts.put(DEFAULT_INDENT_FLAG, 0);
	}

	public ANeuroMLXMLWriter(NeuroMLDocument nmlDocument, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(nmlDocument, format);
		indentCounts.put(DEFAULT_INDENT_FLAG, 0);
	}

	protected String getIndent(Integer flag)
	{
		StringBuilder sb = new StringBuilder();
		if(!indentCounts.containsKey(flag))
		{
			indentCounts.put(flag, 0);
		}
		int indentCount = indentCounts.get(flag);
		for(int i = 0; i < indentCount; i++)
			sb.append(INDENT);
		return sb.toString();
	}

	protected void increaseIndent(Integer flag)
	{
		if(!indentCounts.containsKey(flag))
		{
			indentCounts.put(flag, 0);
		}
		int indentCount = indentCounts.get(flag);
		indentCounts.put(flag, indentCount + 1);

	}

	protected void decreaseIndent(Integer flag)
	{

		if(!indentCounts.containsKey(flag))
		{
			indentCounts.put(flag, 0);
		}
		int indentCount = indentCounts.get(flag);
		indentCounts.put(flag, indentCount - 1);
	}

	@Override
	protected void addComment(StringBuilder sb, String comment)
	{
		addComment(sb, comment, false);
	}

	protected void addComment(StringBuilder sb, String comment, boolean extraReturns)
	{
		addComment(sb, comment, extraReturns, DEFAULT_INDENT_FLAG);
	}

	protected void addComment(StringBuilder sb, String comment, boolean extraReturns, Integer flag)
	{

		if(extraReturns) sb.append("\n");
		if(comment.indexOf("\n") < 0) sb.append(getIndent(flag) + commPre + comment + commPost + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");

		if(extraReturns) sb.append("\n");

	}

	protected void startElement(StringBuilder main, String name)
	{
		startElement(main, name, DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, Integer flag)
	{
		main.append(getIndent(flag) + "<" + name + ">\n");
		increaseIndent(flag);
	}

	protected void startEndElement(StringBuilder main, String name)
	{
		startEndElement(main, name, DEFAULT_INDENT_FLAG);
	}

	protected void startEndElement(StringBuilder main, String name, Integer flag)
	{
		main.append(getIndent(flag) + "<" + name + "/>\n");
	}

	protected void startEndTextElement(StringBuilder main, String name, String contents)
	{
		main.append(getIndent(DEFAULT_INDENT_FLAG) + "<" + name + ">" + contents + "</" + name + ">\n");
	}

	protected void startElement(StringBuilder main, String name, String a1, Integer flag)
	{
		startElement(main, name, a1, false, flag);
	}

	protected void startElement(StringBuilder main, String name, String a1)
	{
		startElement(main, name, a1, false, DEFAULT_INDENT_FLAG);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, Integer flag)
	{
		startElement(main, name, a1, true, flag);
	}

	protected void startEndElement(StringBuilder main, String name, String a1)
	{
		startElement(main, name, a1, true, DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String a1, boolean endToo, Integer flag)
	{

		main.append(getIndent(flag));
		String[] aa = a1.split("=");
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + end + ">\n");
		if(!endToo) increaseIndent(flag);
	}

	protected void addTextElement(StringBuilder main, String name, String text)
	{

		main.append(getIndent(DEFAULT_INDENT_FLAG));
		main.append("<" + name + ">" + text + "</" + name + ">\n");
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, Integer flag)
	{
		startElement(main, name, a1, a2, false, flag);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2)
	{
		startElement(main, name, a1, a2, false, DEFAULT_INDENT_FLAG);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2)
	{
		startElement(main, name, a1, a2, true, DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, boolean endToo, Integer flag)
	{

		main.append(getIndent(flag));
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + end + ">\n");
		if(!endToo) increaseIndent(flag);
	}

	protected String processAttr(String attr)
	{
		if(attr.length() == 0) return "";
		String[] aa = attr.split("=");
		return aa[0].trim() + "=\"" + aa[1].trim() + "\"";

	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, Integer flag)
	{
		startElement(main, name, a1, a2, a3, false, flag);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3)
	{
		startElement(main, name, a1, a2, a3, false, DEFAULT_INDENT_FLAG);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2, String a3)
	{
		startElement(main, name, a1, a2, a3, true, DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, boolean endToo, Integer flag)
	{

		main.append(getIndent(flag));
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + end + ">\n");
		if(!endToo) increaseIndent(flag);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, String a4)
	{
		startElement(main, name, a1, a2, a3, a4, false);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2, String a3, String a4)
	{
		startElement(main, name, a1, a2, a3, a4, true);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, String a4, boolean endToo)
	{

		main.append(getIndent(DEFAULT_INDENT_FLAG));
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + " " + processAttr(a4) + end + ">\n");
		if(!endToo) increaseIndent(DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, String a4, String a5)
	{
		startElement(main, name, a1, a2, a3, a4, a5, false);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2, String a3, String a4, String a5)
	{
		startElement(main, name, a1, a2, a3, a4, a5, true);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, String a4, String a5, boolean endToo)
	{

		main.append(getIndent(DEFAULT_INDENT_FLAG));

		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + " " + processAttr(a4) + " " + processAttr(a5) + end + ">\n");
		if(!endToo) increaseIndent(DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String[] attrs)
	{
		startElement(main, name, attrs, false, DEFAULT_INDENT_FLAG);
	}

	protected void startElement(StringBuilder main, String name, String[] attrs, Integer flag)
	{
		startElement(main, name, attrs, false, flag);
	}

	protected void startEndElement(StringBuilder main, String name, String[] attrs)
	{
		startElement(main, name, attrs, true, DEFAULT_INDENT_FLAG);
	}

	protected void startEndElement(StringBuilder main, String name, String[] attrs, Integer flag)
	{
		startElement(main, name, attrs, true, flag);
	}

	protected void startElement(StringBuilder main, String name, String[] attrs, boolean endToo, Integer flag)
	{

		main.append(getIndent(flag));
		main.append("<" + name);
		for(String attr : attrs)
		{
			if(attr.length() > 0)
			{
				String[] aa = attr.split("=");
				main.append(" " + aa[0].trim() + "=\"" + aa[1].trim() + "\"");
			}
		}
		String end = endToo ? "/" : "";
		main.append(end + ">\n");
		if(!endToo) increaseIndent(flag);
	}

	protected void endElement(StringBuilder main, String name)
	{
		endElement(main, name, DEFAULT_INDENT_FLAG);
	}

	protected void endElement(StringBuilder main, String name, Integer flag)
	{
		decreaseIndent(flag);
		main.append(getIndent(flag) + "</" + name + ">\n");
	}

	public void processMathML(StringBuilder main, DoubleEvaluator expression)
	{
		addComment(main, "Generating MathML from: " + expression.toString() + " ");

		startElement(main, "math", "xmlns=http://www.w3.org/1998/Math/MathML");
		main.append("????\n");
		/*
		 * String mml = expression.getMathML(getIndent(), ""); VariableNode tVarNode = new VariableNode(SBMLWriter.GLOBAL_TIME_SBML); String timeMML = tVarNode.getMathML("", ""); if
		 * (mml.indexOf(timeMML)>=0) {
		 * 
		 * E.info("Replacing: ("+timeMML+") with ("+SBMLWriter.GLOBAL_TIME_SBML_MATHML+")" );
		 * 
		 * mml = mml.replace(timeMML, SBMLWriter.GLOBAL_TIME_SBML_MATHML); } main.append(mml+"\n");
		 */
		endElement(main, "math");
	}

	public void processMathML(StringBuilder main, ParseTree pt) throws ContentError
	{
		processMathML(main, pt, true);
	}

	public void processMathML(StringBuilder main, ParseTree pt, boolean wrapInMathMLElement) throws ContentError
	{

		if(wrapInMathMLElement) startElement(main, "math", "xmlns=http://www.w3.org/1998/Math/MathML");

		// addComment(main,"Complete export to MathML not yet implemented!");
		MathMLWriter mmlw = new MathMLWriter(INDENT, "        ");
		main.append(mmlw.serialize(pt));

		if(wrapInMathMLElement) endElement(main, "math");
	}

}
