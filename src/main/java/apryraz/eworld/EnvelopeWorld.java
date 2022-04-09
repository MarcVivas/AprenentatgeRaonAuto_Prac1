package apryraz.eworld;


import java.io.IOException;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


/**
 * The class for the main program of the Barcenas World
 **/
public class EnvelopeWorld {


    /**
     * This function should execute the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps. Each step must be executed with function
     * runNextStep() of the BarcenasFinder agent.
     *
     * @param wDim          the dimension of world
     * @param numSteps      num of steps to perform
     * @param fileSteps     file name with sequence of steps to perform
     * @param fileEnvelopes file name with sequence of steps to perform
     **/
    public static void runStepsSequence (int wDim, int numSteps, String fileSteps, String fileEnvelopes) throws
            IOException, ContradictionException, TimeoutException {

        // Make instances of TreasureFinder agent and environment object classes
        EnvelopeFinder FinderEAgent = new EnvelopeFinder(wDim);
        EnvelopeWorldEnv EnvAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);


        // save environment object into EAgent
        FinderEAgent.setEnvironment(EnvAgent);

        // load list of steps into the Finder Agent
        FinderEAgent.loadListOfSteps(numSteps, fileSteps);

        // Execute sequence of steps with the Agent
        for(int i = 0; i < numSteps; i++){
            FinderEAgent.runNextStep();
        }

    }

    /**
     * This function loads 4 arguments from the command line:
     *
     * @param args arg[0] = dimension of the word;
     *             arg[1] = num of steps to perform;
     *             arg[2] = file name with sequence of steps to perform;
     *             arg[3] = file name with list of envelopes positions
     **/
    public static void main (String[] args) throws ParseFormatException,
            IOException, ContradictionException, TimeoutException {

        checkArguments(args);

        int wDim = Integer.parseInt(args[0]);
        int numSteps = Integer.parseInt(args[1]);
        String fileSteps = args[2];
        String fileEnvelopes = args[3];

        runStepsSequence(wDim, numSteps, fileSteps, fileEnvelopes);
    }

    /**
     * This function checks if the input arguments are in the correct format.
     *
     * @param args
     * @throws ParseFormatException
     */
    private static void checkArguments (String[] args) throws ParseFormatException {
        if (args.length != 4) {
            throw new ParseFormatException(" Arguments: dim(Integer > 0) numsteps(Integer > 0) stepsfilename envelopesfilename");
        }
        if (Integer.parseInt(args[0]) <= 0 || Integer.parseInt(args[1]) <= 0) {
            throw new ParseFormatException(" Arguments: dim(Integer > 0) numsteps(Integer > 0) stepsfilename envelopesfilename");
        }
    }

}
