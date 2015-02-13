package org.lemsml.export.base;

import org.lemsml.jlems.core.eval.DoubleEvaluator;
import org.lemsml.jlems.core.expression.*;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.export.exceptions.ModelFeatureSupportException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public abstract class AXMLWriter extends ABaseWriter
{

	int indentCount = 0;
	public static final String INDENT = "    ";

	String commPre = "<!--";
	String commPost = "-->";

	public AXMLWriter(Lems lems, String format)
	{
		super(lems, format);
	}

	protected void addComment(StringBuilder sb, String comment)
	{
		addComment(sb, comment, false);
	}

	protected void addComment(StringBuilder sb, String comment, boolean extraReturns)
	{

		if(extraReturns) sb.append("\n");
		if(comment.indexOf("\n") < 0) sb.append(getIndent() + commPre + comment + commPost + "\n");
		else sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");

		if(extraReturns) sb.append("\n");

	}

	protected void startElement(StringBuilder main, String name)
	{
		main.append(getIndent() + "<" + name + ">\n");
		indentCount++;
	}

	protected void startEndElement(StringBuilder main, String name)
	{
		main.append(getIndent() + "<" + name + "/>\n");
	}

	protected void startEndTextElement(StringBuilder main, String name, String contents)
	{
		main.append(getIndent() + "<" + name + ">" + contents + "</" + name + ">\n");
	}

	protected String getIndent()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < indentCount; i++)
			sb.append(INDENT);
		return sb.toString();
	}

	protected void startElement(StringBuilder main, String name, String a1)
	{
		startElement(main, name, a1, false);
	}

	protected void startEndElement(StringBuilder main, String name, String a1)
	{
		startElement(main, name, a1, true);
	}

	protected void startElement(StringBuilder main, String name, String a1, boolean endToo)
	{

		main.append(getIndent());
		String[] aa = a1.split("=");
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + end + ">\n");
		if(!endToo) indentCount++;
	}

	protected void addTextElement(StringBuilder main, String name, String text)
	{

		main.append(getIndent());
		main.append("<" + name + ">" + text + "</" + name + ">\n");
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2)
	{
		startElement(main, name, a1, a2, false);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2)
	{
		startElement(main, name, a1, a2, true);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, boolean endToo)
	{

		main.append(getIndent());
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + end + ">\n");
		if(!endToo) indentCount++;
	}

	protected String processAttr(String attr)
	{
		if(attr.length() == 0) return "";
		String[] aa = attr.split("=");
		return aa[0].trim() + "=\"" + aa[1].trim() + "\"";

	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3)
	{
		startElement(main, name, a1, a2, a3, false);
	}

	protected void startEndElement(StringBuilder main, String name, String a1, String a2, String a3)
	{
		startElement(main, name, a1, a2, a3, true);
	}

	protected void startElement(StringBuilder main, String name, String a1, String a2, String a3, boolean endToo)
	{

		main.append(getIndent());
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + end + ">\n");
		if(!endToo) indentCount++;
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

		main.append(getIndent());
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + " " + processAttr(a4) + end + ">\n");
		if(!endToo) indentCount++;
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

		main.append(getIndent());
		/*
		 * String[] aa = a1.split("="); String[] aaa = a2.split("="); String[] aaaa = a3.split("="); String[] aaaaa = a4.split("="); String[] aaaaaa = a5.split("=");
		 */
		String end = endToo ? "/" : "";
		main.append("<" + name + " " + processAttr(a1) + " " + processAttr(a2) + " " + processAttr(a3) + " " + processAttr(a4) + " " + processAttr(a5) + end + ">\n");
		if(!endToo) indentCount++;
	}

	protected void startElement(StringBuilder main, String name, String[] attrs)
	{
		startElement(main, name, attrs, false);
	}

	protected void startEndElement(StringBuilder main, String name, String[] attrs)
	{
		startElement(main, name, attrs, true);
	}

	protected void startElement(StringBuilder main, String name, String[] attrs, boolean endToo)
	{

		main.append(getIndent());
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
		if(!endToo) indentCount++;
	}

	protected void endElement(StringBuilder main, String name)
	{
		indentCount--;
		main.append(getIndent() + "</" + name + ">\n");
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
