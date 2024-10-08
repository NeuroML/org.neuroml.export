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
            "8.2.1", "8.2.0", "8.1.0", "8.0.2", "8.0.1", "8.0.0",
            "7.8.2", "7.8.1", "7.7.1", "7.6.7", "7.5", "7.4", "7.3", "7.2", "7.1",
            "6.2", "6.1", "6.0"
        };

        /* If NEURON_HOME is defined, it gets priority */
        if (nrnEnvVar != null && nrnEnvVar.length()>0)
        {
            options.add(nrnEnvVar);
        }
        /* If NEURON_HOME is not defined, check all the usual suspects */
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
            /* Check folders in PATH */
            for (String folder: System.getenv("PATH").split(";")) {
                options.add(folder);
            }

            /* Other possible folders */
            for (String ver : knownVersions)
            {
                options.add("/Applications/NEURON-" + ver + "/nrn/powerpc");
                options.add("/Applications/NEURON-" + ver + "/nrn/umac");
                options.add("/Applications/NEURON-" + ver + "/nrn/i386");
                options.add("/Applications/NEURON-" + ver + "/nrn/x86_64");
            }

        }
        else if (Utils.isLinuxBasedPlatform())
        {

            /* Check folders in PATH */
            for (String folder: System.getenv("PATH").split(":")) {
                options.add(folder);
            }

            /* Other possible folders */
            options.add("/usr");
            options.add("/usr/local");
            options.add("/usr/local/nrn/x86_64");
            options.add("/srv/conda/envs/notebook/"); // location of neuron when pip installed on Binder... 
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

        String env = Utils.sysEnvInfo("  ");

        throw new NeuroMLException("Could not find NEURON home directory! Options tried here: " + options
            + "\nThe NEURON executable which is sought inside this directory is: " + nrnExe + ". \n\n"
            + "Try setting the environment variable " + NeuronWriter.NEURON_HOME_ENV_VAR
            + " to the location of your NEURON installation (up to but not including bin), e.g.\n\n"
            + "  export " + NeuronWriter.NEURON_HOME_ENV_VAR + "=/home/myuser/nrn7/x86_64\n\n"+
            "Currently " + NeuronWriter.NEURON_HOME_ENV_VAR + " is set to: "+nrnEnvVar+"\n"+env, null);

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

        ArrayList<File> filesToBeCreated = new ArrayList<File> ();

        String myArch = Utils.getArchSpecificDir();
        try
        {
            String directoryToExecuteIn = modDirectory.getCanonicalPath();
            File otherCheckFileToBeCreated = null; // for now...

            E.info("Parent dir: " + directoryToExecuteIn);

            if (Utils.isWindowsBasedPlatform())
            {
                E.info("Assuming Windows environment...");

                String filename = directoryToExecuteIn + System.getProperty("file.separator") + "nrnmech.dll";

                filesToBeCreated.add(new File(filename));

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

                E.info("Name of file(s) to be created: ");
                for (File f : filesToBeCreated)
                    E.info(f.getAbsolutePath());

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


                String backupArchDir = Utils.DIR_64BIT;

                if (myArch.equals(Utils.ARCH_64BIT))
                {
                    backupArchDir = Utils.DIR_I686;
                }

                /* *.la */
                String filename1 = directoryToExecuteIn + System.getProperty("file.separator") + myArch + System.getProperty("file.separator") + "libnrnmech.la";
                filesToBeCreated.add(new File(filename1));
                E.info("Name of file to be created: " + filename1);

                // In case, e.g. a 32 bit JDK is used on a 64 bit system
                String filename2 = directoryToExecuteIn + System.getProperty("file.separator") + backupArchDir + System.getProperty("file.separator") + "libnrnmech.la";
                /* Only add if it does not already exist: prevent duplication */
                if (!filename1.equals(filename2)){
                    filesToBeCreated.add(new File(filename2));
                    E.info("Name of file to be created: " + filename2);
                }

                /* *.so */
                filename1 = directoryToExecuteIn + System.getProperty("file.separator") + myArch + System.getProperty("file.separator") + "libnrnmech.so";
                filesToBeCreated.add(new File(filename1));
                E.info("Name of file to be created: " + filename1);

                // In case, e.g. a 32 bit JDK is used on a 64 bit system
                filename2 = directoryToExecuteIn + System.getProperty("file.separator") + backupArchDir + System.getProperty("file.separator") + "libnrnmech.so";
                /* Only add if it does not already exist: prevent duplication */
                if (!filename1.equals(filename2)){
                    filesToBeCreated.add(new File(filename2));
                    E.info("Name of file to be created: " + filename2);
                }

                /* *.so in .libs */
                filename1 = directoryToExecuteIn + System.getProperty("file.separator") + myArch + System.getProperty("file.separator") + ".libs" + System.getProperty("file.separator") + "libnrnmech.so";
                filesToBeCreated.add(new File(filename1));
                E.info("Name of file to be created: " + filename1);

                // In case, e.g. a 32 bit JDK is used on a 64 bit system
                filename2 = directoryToExecuteIn + System.getProperty("file.separator") + backupArchDir + System.getProperty("file.separator") + ".libs" + System.getProperty("file.separator") + "libnrnmech.so";
                /* Only add if it does not already exist: prevent duplication */
                if (!filename1.equals(filename2)){
                    filesToBeCreated.add(new File(filename2));
                    E.info("Name of file to be created: " + filename2);
                }

                /**
                 * @todo Needs checking on Mac/powerpc/i686
                 */
                if (Utils.isMacBasedPlatform())
                {
                    filename1 = directoryToExecuteIn + System.getProperty("file.separator") + Utils.getArchSpecificDir() + System.getProperty("file.separator") + "libnrnmech.la";
                    E.info("Name of file to be created 1: " + filename1);
                    filesToBeCreated.add(new File(filename1));

                    filename1 = directoryToExecuteIn + System.getProperty("file.separator") + "umac" + System.getProperty("file.separator") + "libnrnmech.la";
                    E.info("Name of file to be created 2: " + filename1);
                    filesToBeCreated.add(new File(filename1));

                    filename1 = directoryToExecuteIn + System.getProperty("file.separator") + Utils.getArchSpecificDir() + System.getProperty("file.separator") + "libnrnmech.dylib";
                    E.info("Name of file to be created 3: " + filename1);
                    filesToBeCreated.add(new File(filename1));

                    filename1 = directoryToExecuteIn + System.getProperty("file.separator") + "arm64" + System.getProperty("file.separator") + "libnrnmech.dylib";
                    E.info("Name of file to be created 4: " + filename1);
                    filesToBeCreated.add(new File(filename1));
                }

                commandToExecute = neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "nrnivmodl";
                E.info("commandToExecute: " + commandToExecute);

            }

            /* Check if libnrnmech.* already exists */
            File createdFile = null;
            for (File f: filesToBeCreated)
            {
                if (f.exists())
                {
                    createdFile = f;
                    E.info("Found previously compiled file: " + f.getAbsolutePath());
                    break;
                }
            }
            /* If it exists check if we were asked to force recompile etc. */
            if (createdFile != null)
            {
                /* We were asked to force recompile */
                if (forceRecompile)
                {
                    E.info("Forcing recompile...");
                }
                /* We weren't asked to recompile, so we check if there are newer mod files which will require compilation */
                else
                {
                    File fileToCheck = null;
                    boolean newerModExists = false;
                    for (File fn: filesToBeCreated)
                    {
                        if (fn.exists())
                        {
                            fileToCheck = fn;
                            E.info("Going to check if mods in " + modDirectory + "" + " are newer than " + fileToCheck);

                            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
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
                        }
                    }
                    if (!newerModExists)
                    {
                        E.info("Not being asked to recompile, and no mod files exist in " + modDirectory + "" + " which are newer than " + fileToCheck);
                        return true;
                    }
                    else
                    {
                        E.info("Newer mod files exist! Will recompile.");
                    }
                }
            }

            E.info("Trying to delete any previously created files: ");
            for (File f: filesToBeCreated)
            {
                E.info(f.getAbsolutePath());

                if (f.exists())
                {
                    f.delete();

                    E.info("Deleted.");
                }
            }

            E.info("directoryToExecuteIn: " + directoryToExecuteIn);

            Process currentProcess = rt.exec(commandToExecute, null, new File(directoryToExecuteIn));
            ProcessOutputWatcher procOutputMain = new ProcessOutputWatcher(currentProcess.getInputStream(), "NMODL Compile >> ");
            procOutputMain.start();

            ProcessOutputWatcher procOutputError = new ProcessOutputWatcher(currentProcess.getErrorStream(), "NMODL Error   >> ");
            procOutputError.start();


            currentProcess.waitFor();
            int retVal = currentProcess.exitValue();

            String linMacWarn = "   NOTE: make sure you can compile NEURON mod files on your system!\n\n"
                + "Often, extra packages (e.g. dev packages of ncurses & readline) need to be installed "
                + "to successfully run nrnivmodl, which compiles mod files\n" + "Go to " + modDirectory
                + " and try running nrnivmodl";

            E.info("Executed command " + commandToExecute);
            if (retVal == 0)
            {
                /* Check if the necessary file was created to confirm compilation succeded */
                createdFile = null;
                for (File f: filesToBeCreated)
                {
                    E.info("Verifying mod file compilation: looking for file: " + f.getAbsolutePath());
                    if (f.exists())
                    {
                        createdFile = f;
                        E.info(createdFile + " found. Compilation successful.");
                        return true;
                    }
                    else
                    {
                        E.info(f.getAbsolutePath() + " not found.");
                    }
                }
                /* If a generated file is not found */
                if (createdFile == null)
                {
                    E.info("Compilation failed. Unable to find necessary file(s)." +
                            " Please note that Neuron checks every *.mod file in this file's parent directory\n" +
                            "(" + modDirectory + ").\n\n" +
                            linMacWarn);

                    /* Print list of files we look for */
                    for (File f1: filesToBeCreated)
                    {
                        E.info(f1.getAbsolutePath());
                    }

                    /* TODO: what are we doing here? */
                    /* Only delete on non-Windows machines? */
                    if (!Utils.isWindowsBasedPlatform())
                    {
                        E.info("Deleting generated dir(s): ");
                        for (File f2: filesToBeCreated)
                        {
                            E.info(f2.getParentFile().getAbsolutePath());
                            if (f2.getParentFile().getName().equals(myArch));
                            {
                                Utils.removeAllFiles(f2.getParentFile(), true, true);
                            }
                        }
                    }
                    return false;
                }
            }
            else
            {
                E.info("Unsuccessful compilation of NEURON mod files.");
                E.info(linMacWarn);

                /* TODO: Ignoring Mac errors? */
                if (Utils.isMacBasedPlatform())
                {
                    return true;
                }
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
            for (File f: filesToBeCreated)
            {
                f.delete();
            }
            throw new NeuroMLException("Error testing: " + modDirectory.getAbsolutePath() + ".\nIs NEURON correctly installed?\n" + "NEURON home dir being used: " + findNeuronHome().getAbsolutePath()
                + "\n\n", ex);
        }
        return true;
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
