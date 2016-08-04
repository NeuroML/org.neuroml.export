package org.neuroml.export.utils;

import java.util.ArrayList;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPath
{

    private final String quantity;
    protected String population;
    protected String scale = "1";
    protected int populationIndex = 0;
    protected int segmentId = 0;

    protected enum Type
    {

        UNKNOWN, VAR_IN_SINGLE_COMP, VAR_IN_CELL_IN_POP, VAR_IN_CELL_IN_POP_LIST, VAR_ON_SEG_IN_CELL_IN_POP_LIST
    }

    protected Type myType = Type.UNKNOWN;

    protected String[] variableParts;

    public final static String DUMMY_POPULATION_PREFIX = "population_";

    public LEMSQuantityPath(String q)
    {
        quantity = q;
        parseQuantity();
    }

    public LEMSQuantityPath(String q, String s)
    {
        quantity = q;
        scale = s;
        parseQuantity();
    }

    public void setPopulation(String p)
    {
        population = p;
    }

    public String getPopulationArray()
    {
        return "a_" + population;
    }

    public String getQuantity()
    {
        return quantity;
    }

    public static String getVariablePartsAsString(String[] variableParts)
    {
        return getVariablePartsAsString("_", variableParts);
    }
    
    
    public String[] getVariableParts()
    {
        return variableParts;
    }

    public static String getVariablePartsAsString(String separator, String[] variableParts)
    {
        StringBuilder var = new StringBuilder();
        for (String varPart : variableParts)
        {
            var.append(separator).append(varPart);
        }
        return var.toString().substring(1);
    }

    public static String getVariable(String[] variableParts, Type myType)
    {

        switch (myType)
        {
            case VAR_IN_SINGLE_COMP:
                return variableParts[0];
            case VAR_IN_CELL_IN_POP:
                return getVariablePartsAsString(variableParts);
            case VAR_IN_CELL_IN_POP_LIST:
                return getVariablePartsAsString(variableParts);

            default:
                return getVariablePartsAsString(variableParts);
        }
    }

    public String getVariable()
    {
        return getVariable(variableParts, myType);
    }

    public String getVariablePathInPopComp()
    {
        return getVariablePathInPopComp(variableParts, myType);
    }

    public static String getVariablePathInPopComp(String[] variableParts, Type myType)
    {
        switch (myType)
        {
            case VAR_IN_SINGLE_COMP:
                return variableParts[0];
            case VAR_IN_CELL_IN_POP:
                return getVariablePartsAsString("/",variableParts);
            case VAR_IN_CELL_IN_POP_LIST:
                return getVariablePartsAsString("/",variableParts);

            default:
                return getVariablePartsAsString("/",variableParts);
        }
    }

    public String getPopulation()
    {
        return population;
    }

    public String getScale()
    {
        return scale;
    }

    public int getPopulationIndex()
    {
        return populationIndex;
    }

    public int getSegmentId()
    {
        return segmentId;
    }

    public boolean isVariableInPopulation()
    {
        return quantity.contains("/");
    }

    public boolean isVariableOnSynapse()
    {
        return quantity.contains(":");
    }

    private void parseQuantity()
    {
        //System.out.println("Parsing: "+quantity);
        String[] parts = quantity.split("/");

        if (parts.length == 1)
        {
            variableParts = new String[]
            {
                parts[0]
            };
            population = DUMMY_POPULATION_PREFIX;

            myType = Type.VAR_IN_SINGLE_COMP;

        }
        else if (quantity.indexOf('[') > 0)
        {
            population = quantity.split("\\[")[0];
            populationIndex = Integer.parseInt(quantity.split("\\[")[1].split("\\]")[0]);

            variableParts = new String[parts.length - 1];
            for (int i = 1; i < parts.length; i++)
            {

                variableParts[i - 1] = parts[i];
            }

            myType = Type.VAR_IN_CELL_IN_POP;

        }
        else if (parts.length == 4)
        {
            population = parts[0];
            populationIndex = Integer.parseInt(parts[1]);
            myType = Type.VAR_IN_CELL_IN_POP_LIST;
            variableParts = new String[parts.length - 3];
            for (int i = 3; i < parts.length; i++)
            {
                variableParts[i - 3] = parts[i];
            }
        }
        else if (parts.length == 5 && !isVariableOnSynapse())
        {
            population = parts[0];
            populationIndex = Integer.parseInt(parts[1]);
            try
            {
                segmentId = Integer.parseInt(parts[3]);
                myType = Type.VAR_ON_SEG_IN_CELL_IN_POP_LIST;
                variableParts = new String[parts.length - 4];
                for (int i = 4; i < parts.length; i++)
                {
                    variableParts[i - 4] = parts[i];
                }
            }
            catch (NumberFormatException nfe)
            {
                myType = Type.VAR_IN_CELL_IN_POP_LIST;
                variableParts = new String[parts.length - 3];
                for (int i = 3; i < parts.length; i++)
                {
                    variableParts[i - 3] = parts[i];
                }
            }
            
        }
        else if (parts.length >= 5 && isVariableOnSynapse())
        {
            population = parts[0];
            populationIndex = Integer.parseInt(parts[1]);
            
            if (isInteger(parts[3]))
            {

                myType = Type.VAR_ON_SEG_IN_CELL_IN_POP_LIST;
                segmentId = Integer.parseInt(parts[3]);
                variableParts = new String[parts.length - 4];
                for (int i = 4; i < parts.length; i++)
                {
                    variableParts[i - 4] = parts[i];
                }

            }
            else
            {

                myType = Type.VAR_IN_CELL_IN_POP_LIST;
                variableParts = new String[parts.length - 3];
                for (int i = 3; i < parts.length; i++)
                {
                    variableParts[i - 3] = parts[i];
                }
            }
        }
        else if (parts.length > 5)
        {
            population = parts[0];
            populationIndex = Integer.parseInt(parts[1]);

            if (isInteger(parts[3]))
            {

                myType = Type.VAR_ON_SEG_IN_CELL_IN_POP_LIST;
                segmentId = Integer.parseInt(parts[3]);
                variableParts = new String[parts.length - 4];
                for (int i = 4; i < parts.length; i++)
                {
                    variableParts[i - 4] = parts[i];
                }

            }
            else
            {

                myType = Type.VAR_IN_CELL_IN_POP_LIST;
                variableParts = new String[parts.length - 3];
                for (int i = 3; i < parts.length; i++)
                {
                    variableParts[i - 3] = parts[i];
                }
            }
        }
        else
        {
            myType = Type.UNKNOWN;
        }

    }

    public static boolean isInteger(String s)
    {
        try
        {
            Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "Original:       " + quantity
            + "\n    " + "Type:           " + myType
            + "\n    " + "Variable parts: " + getVariablePartsAsString(variableParts)
            + "\n    " + "Variable:       " + getVariable()
            + "\n    " + "Is var in pop:  " + this.isVariableInPopulation() + (this.isVariableInPopulation() ? " (" + this.getPopulation() + ")" : "")
            + "\n    " + "Is var in syn:  " + this.isVariableOnSynapse()
            + "\n    " + "Num:            " + populationIndex
            + "\n    " + "segmentId:      " + segmentId;
    }

    public static void main(String[] args) throws Exception
    {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");
        paths.add("Gran/0/Granule_98/v");
        paths.add("TestBasket/0/pvbasketcell/v");
        paths.add("TestBasket/0/pvbasketcell/3/v");
        paths.add("One_ChannelML/0/OneComp_ChannelML/biophys/membraneProperties/Na_ChannelML_all/Na_ChannelML/m/q");
        paths.add("One_ChannelML/0/OneComp_ChannelML/4/biophys/membraneProperties/Na_ChannelML_all/Na_ChannelML/m/q");
        paths.add("pasPop1[0]/synapses:nmdaSyn1:0/g");
        paths.add("pasPop1/0/pasCell/synapses:nmdaSyn1:0/g");
        
        paths.add("pasPop1/0/pasCell/0/synapses:nmdaSyn1:0/g");
        paths.add("pop0/1/MultiCompCell/2/synapses:AMPA:0/g");
        paths.add("pop0/1/MultiCompCell/synapses:AMPA:0/g");/**/

        for (String path : paths)
        {
            LEMSQuantityPath l1 = new LEMSQuantityPath(path);
            System.out.println("\n--------\n" + l1);
        }
    }

}
