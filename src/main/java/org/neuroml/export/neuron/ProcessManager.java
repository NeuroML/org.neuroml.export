package org.neuroml.export.neuron;

import java.io.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Date;

import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.neuroml.export.utils.ProcessOutputWatcher;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.util.NeuroMLException;

/**
 *
 * @author Padraig Gleeson
 *
 */
public class ProcessManager
{

    public static File findNeuronHome() throws NeuroMLException
    {

        String nrnExe = "bin/nrniv";
        String nrnExe64 = "bin/nrniv";

        if (Utils.isWindowsBasedPlatform())
        {
            nrnExe = "bin/neuron.exe";
            nrnExe64 = "bin64/neuron.exe";
        }
        ArrayList<String> options = new ArrayList<String>();
        String nrnEnvVar = System.getenv(NeuronWriter.NEURON_HOME_ENV_VAR);
        String[] knownVersions = new String[]
        {
            "7.4", "7.3", "7.2", "7.1", "6.2", "6.1", "6.0"
        };

        if (nrnEnvVar != null)
        {
            options.add(nrnEnvVar);
        }
        else if (Utils.isWindowsBasedPlatform())
        {
            for (String ver : knownVersions)
            {
                options.add("C:\\nrn" + ver.replaceAll("\\.", ""));
                options.add("C:\\nrn" + ver.replaceAll("\\.", "") + "w");
            }

        }
        else if (Utils.isMacBasedPlatform())
        {
            for (String ver : knownVersions)
            {
                options.add("/Applications/NEURON-" + ver + "/nrn/powerpc");
                options.add("/Applications/NEURON-" + ver + "/nrn/umac");
                options.add("/Applications/NEURON-" + ver + "/nrn/i386");
            }

        }
        else if (Utils.isLinuxBasedPlatform())
        {
            options.add("/usr/local");
            options.add("/usr/local/nrn/x86_64");
        }

        for (String option : options)
        {
            File f = new File(option, nrnExe);
            if (f.exists())
            {
                return new File(option);
            }
            f = new File(option, nrnExe64);
            if (f.exists())
            {
                return new File(option);
            }
        }

        throw new NeuroMLException("Could not find NEURON home directory! Options tried: " + options
            + ", NEURON executable sought: " + nrnExe + ". \n\n"
            + "Try setting the environment variable " + NeuronWriter.NEURON_HOME_ENV_VAR
            + " to the location of your NEURON installation (containing bin), e.g.\n\n"
            + "  export " + NeuronWriter.NEURON_HOME_ENV_VAR + "=/home/myuser/nrn7/x86_64\n", null);

    }

    /*
     * Compliles all of the mod files at the specified location using NEURON's nrnivmodl/mknrndll.sh
     */
    public static boolean compileFileWithNeuron(File modDirectory, boolean forceRecompile) throws NeuroMLException
    {
        E.info("Going to compile the mod files in: " + modDirectory.getAbsolutePath() + ", forcing recompile: " + forceRecompile);

        Runtime rt = Runtime.getRuntime();

        String commandToExecute = null;

        File neuronHome = findNeuronHome();

        try
        {
            String directoryToExecuteIn = modDirectory.getCanonicalPath();
            File fileToBeCreated = null;
            File otherCheckFileToBeCreated = null; // for now...

            E.info("Parent dir: " + directoryToExecuteIn);

            if (Utils.isWindowsBasedPlatform())
            {
                E.info("Assuming Windows environment...");

                String filename = directoryToExecuteIn + System.getProperty("file.separator") + "nrnmech.dll";

                fileToBeCreated = new File(filename);

                String binExe = neuronHome + "\\bin\\sh.exe";
                if (!(new File(binExe)).exists())
                {
                    binExe = neuronHome + "\\bin64\\sh.exe";
                }
                if (!(new File(binExe)).exists())
                {
                    binExe = neuronHome + "\\mingw\\bin\\sh.exe";
                }
                if (!(new File(binExe)).exists())
                {
                    binExe = neuronHome + "\\mingw64\\bin\\sh.exe";
                }

                E.info("Name of file to be created: " + fileToBeCreated.getAbsolutePath());

                File modCompileScript = Utils.copyFromJarToTempLocation("/neuron/mknrndll.sh");

                String shFriendlyPath = 
                    modCompileScript.getAbsolutePath().replaceAll("c:\\\\", "/cygdrive/c/").replaceAll("C:\\\\", "/cygdrive/c/").replaceAll("\\\\", "/");

                if (binExe.indexOf("mingw") > 0)
                {
                    throw new NeuroMLException("****\n  Unfortunately, jNeuroML doesn't currently support MinGW versions of NEURON. "
                        + "Try the Cygwin version of 7.3, e.g. nrn-7.3.i686-pc-cygwin-setup.exe\n****\n");
                }
                if (binExe.indexOf("74") > 0)
                {
                    throw new NeuroMLException("****\n  Unfortunately, jNeuroML doesn't yet support NEURON 7.4. "
                        + "Try the Cygwin version of 7.3, e.g. nrn-7.3.i686-pc-cygwin-setup.exe\n****\n");
                }

                commandToExecute = binExe + " \"" + shFriendlyPath + "\" " + neuronHome + " " + " -q";

                E.info("commandToExecute: " + commandToExecute);

            }
            else
            {
                E.info("Assuming *nix environment...");

                String myArch = Utils.getArchSpecificDir();

                String backupArchDir = Utils.DIR_64BIT;

                if (myArch.equals(Utils.ARCH_64BIT))
                {
                    backupArchDir = Utils.DIR_I686;
                }

                String filename = directoryToExecuteIn + System.getProperty("file.separator") + myArch + System.getProperty("file.separator") + "libnrnmech.la";

                // In case, e.g. a 32 bit JDK is used on a 64 bit system
                String backupFilename = directoryToExecuteIn + System.getProperty("file.separator") + backupArchDir + System.getProperty("file.separator") + "libnrnmech.la";

                /**
                 * @todo Needs checking on Mac/powerpc/i686
                 */
                if (Utils.isMacBasedPlatform())
                {
                    filename = directoryToExecuteIn + System.getProperty("file.separator") + Utils.getArchSpecificDir() + System.getProperty("file.separator") + "libnrnmech.la";

                    backupFilename = directoryToExecuteIn + System.getProperty("file.separator") + "umac" + System.getProperty("file.separator") + "libnrnmech.la";

                }

                E.info("Name of file to be created: " + filename);
                E.info("Backup file to check for success: " + backupFilename);

                fileToBeCreated = new File(filename);
                otherCheckFileToBeCreated = new File(backupFilename);

                commandToExecute = neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "nrnivmodl";

                E.info("commandToExecute: " + commandToExecute);

            }

            if (!forceRecompile)
            {
                File fileToCheck = null;

                if (fileToBeCreated.exists())
                {
                    fileToCheck = fileToBeCreated;
                }

                if (otherCheckFileToBeCreated != null && otherCheckFileToBeCreated.exists())
                {
                    fileToCheck = otherCheckFileToBeCreated;
                }

                E.info("Going to check if mods in " + modDirectory + "" + " are newer than " + fileToCheck);

                if (fileToCheck != null)
                {
                    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

                    boolean newerModExists = false;
                    File[] allMods = modDirectory.listFiles();
                    for (File f : allMods)
                    {
                        if (f.getName().endsWith(".mod") && f.lastModified() > fileToCheck.lastModified())
                        {
                            newerModExists = true;
                            E.info("File " + f + " (" + df.format(new Date(f.lastModified())) + ") was modified later than " + fileToCheck + " (" + df.format(new Date(fileToCheck.lastModified()))
                                + ")");
                        }
                    }
                    if (!newerModExists)
                    {
                        E.info("Not being asked to recompile, and no mod files exist in " + modDirectory + "" + " which are newer than " + fileToCheck);
                        return true;
                    }
                    else
                    {
                        E.info("Newer mod files exist!");
                    }
                }

            }
            else
            {
                E.info("Forcing recompile...");
            }

            E.info("Trying to delete any previous: " + fileToBeCreated.getAbsolutePath());

            if (fileToBeCreated.exists())
            {
                fileToBeCreated.delete();

                E.info("Deleted.");
            }

            E.info("directoryToExecuteIn: " + directoryToExecuteIn);

            Process currentProcess = rt.exec(commandToExecute, null, new File(directoryToExecuteIn));
            ProcessOutputWatcher procOutputMain = new ProcessOutputWatcher(currentProcess.getInputStream(), "NMODL Compile >> ");
            procOutputMain.start();

            ProcessOutputWatcher procOutputError = new ProcessOutputWatcher(currentProcess.getErrorStream(), "NMODL Error   >> ");
            procOutputError.start();

            E.info("Have successfully executed command: " + commandToExecute);

            currentProcess.waitFor();

            if (fileToBeCreated.exists() || otherCheckFileToBeCreated.exists())
            {
                // In case, e.g. a 32 bit JDK is used on a 64 bit system
                File createdFile = fileToBeCreated;
                if (!createdFile.exists())
                {
                    createdFile = otherCheckFileToBeCreated;
                }

                E.info("Successful compilation");

                return true;
            }
            else if (Utils.isMacBasedPlatform())
            {
                return true;
            }
            else
            {
                E.info("Unsuccessful compilation. File doesn't exist: " + fileToBeCreated.getAbsolutePath() + " (and neither does " + otherCheckFileToBeCreated.getAbsolutePath() + ")");

                String linMacWarn = "   NOTE: make sure you can compile NEURON mod files on your system!\n\n"
                    + "Often, extra packages (e.g. dev packages of ncurses & readline) need to be installed "
                    + "to successfully run nrnivmodl, which compiles mod files\n" + "Go to " + modDirectory
                    + " and try running nrnivmodl";
                if (Utils.isWindowsBasedPlatform())
                {
                    linMacWarn = "";
                }

                E.error("Problem with mod file compilation. File doesn't exist: " + fileToBeCreated.getAbsolutePath() 
                    + "\n" + "(and neither does " + otherCheckFileToBeCreated.getAbsolutePath()
                    + ")\n" + "Please note that Neuron checks every *.mod file in this file's home directory\n" + "(" + modDirectory + ").\n"
                    + "For more information when this error occurs, enable logging at Settings -> General Properties & Project Defaults -> Logging\n\n" + linMacWarn);
                return false;
            }

        }
        catch (Exception ex)
        {
            E.error("Error running the command: " + commandToExecute + "\n" + ex.getMessage());
            String dirContents = "bin/nrniv";
            if (Utils.isWindowsBasedPlatform())
            {
                dirContents = "bin\\neuron.exe";
            }
            throw new NeuroMLException("Error testing: " + modDirectory.getAbsolutePath() + ".\nIs NEURON correctly installed?\n" + "NEURON home dir being used: " + findNeuronHome().getAbsolutePath()
                + "\n\n", ex);
        }

    }

    public static void main(String args[]) throws IOException
    {
        File f = Utils.isWindowsBasedPlatform() ? new File("c:\\pyNeuroML\\examples") : new File("/home/padraig/pyNeuroML/examples");;

        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);
        try
        {
            System.out.println("Trying to compile mods in: " + f.getCanonicalPath());
            ProcessManager.compileFileWithNeuron(f, false);
            System.out.println("Done!");

            System.exit(0);
        }
        catch (NeuroMLException ex)
        {
            ex.printStackTrace();
        }

    }
}
