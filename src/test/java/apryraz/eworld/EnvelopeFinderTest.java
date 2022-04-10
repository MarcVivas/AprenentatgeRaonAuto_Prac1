package apryraz.eworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.lang.System.exit;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


import apryraz.eworld.*;

import static org.junit.Assert.assertEquals;

import org.junit.*;

/**
 * Class for testing the EnvelopeFinder agent
 **/
public class EnvelopeFinderTest {


    /**
     * This function should execute the next step of the agent, and the assert
     * whether the resulting state is equal to the targetState
     *
     * @param eAgent      EnvelopeFinder agent
     * @param targetState the state that should be equal to the resulting state of
     *                    the agent after performing the next step
     **/
    public void testMakeSimpleStep (EnvelopeFinder eAgent, EFState targetState) throws IOException, ContradictionException, TimeoutException {
        // Check (assert) whether the resulting state is equal to
        //  the targetState after performing action runNextStep with the agent
        assertEquals(eAgent.getState(), targetState);

    }


    /**
     * Read an state from the current position of the file through the
     * BufferedReader object
     *
     * @param br   BufferedReader object interface to the opened file of states
     * @param wDim dimension of the world
     **/
    public EFState readTargetStateFromFile (BufferedReader br, int wDim) throws
            IOException {
        EFState efstate = new EFState(wDim);
        String row;
        String[] rowvalues;

        for (int i = wDim; i >= 1; i--) {
            row = br.readLine();
            rowvalues = row.split(" ");
            for (int j = 1; j <= wDim; j++) {
                efstate.set(i, j, rowvalues[j - 1]);
            }
        }
        return efstate;
    }

    /**
     * Load a sequence of states from a file, and return the list
     *
     * @param WDim       dimension of the world
     * @param numStates  num of states to read from the file
     * @param statesFile file name with sequence of target states, that should
     *                   be the resulting states after each movement in fileSteps
     * @return returns an ArrayList of TFState with the resulting list of states
     **/
    ArrayList<EFState> loadListOfTargetStates (int wDim, int numStates, String statesFile) {

        ArrayList<EFState> listOfStates = new ArrayList<EFState>(numStates);

        try {
            BufferedReader br = new BufferedReader(new FileReader(statesFile));
            String row;

            // steps = br.readLine();
            for (int s = 0; s < numStates; s++) {
                listOfStates.add(readTargetStateFromFile(br, wDim));
                // Read a blank line between states
                row = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => States file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinderTest.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }

        return listOfStates;
    }


    /**
     * This function should run the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps.
     *
     * @param wDim          the dimension of world
     * @param numSteps      num of steps to perform
     * @param fileSteps     file name with sequence of steps to perform
     * @param fileStates    file name with sequence of target states, that should
     *                      be the resulting states after each movement in fileSteps
     * @param fileEnvelopes
     **/
    public void testMakeSeqOfSteps (int wDim, int numSteps, String fileSteps, String fileStates, String fileEnvelopes) throws IOException, ContradictionException, TimeoutException {
        EnvelopeFinder eAgent = new EnvelopeFinder(wDim);
        // Load information about the World into the EnvAgent
        EnvelopeWorldEnv envAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);
        // Load list of states
        ArrayList<EFState> seqOfStates = loadListOfTargetStates(wDim, numSteps, fileStates);


        // Load list of steps into the finder agent
        eAgent.loadListOfSteps(numSteps, fileSteps);
        // Set environment agent
        eAgent.setEnvironment(envAgent);

        // Test here the sequence of steps and check the resulting states with the
        // ones in seqOfStates
        // Execute sequence of steps with the Agent
        for (EFState currentState : seqOfStates) {
            eAgent.runNextStep();
            testMakeSimpleStep(eAgent, currentState);
        }
    }

    /**
     * test1 (states1.txt steps1.txt envelopes1.txt):  5x5 world,  5 steps,  envelopes at  2,2 4,4
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest1 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(5, 5, "tests/steps1.txt", "tests/states1.txt", "tests/envelopes1.txt");
    }

    /**
     * test2 (states2.txt steps2.txt envelopes2.txt): 5x5 world,  7 steps , envelopes at  3,2 3,4
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest2 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(5, 7, "tests/steps2.txt", "tests/states2.txt", "tests/envelopes2.txt");
    }

    /**
     * test3 (states3.txt steps3.txt envelopes3.txt): 7x7 world,  6 steps,  envelopes at  3,2 4,4 2,6
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest3 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(7, 6, "tests/steps3.txt", "tests/states3.txt", "tests/envelopes3.txt");
    }

    /**
     * test4 (states4.txt steps4.txt envelopes4.txt): 7x7 world,  12 steps , envelopes at  6,2 4,4 2,6
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest4 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(7, 12, "tests/steps4.txt", "tests/states4.txt", "tests/envelopes4.txt");
    }

    /**
     * test5 (states5.txt steps5.txt envelopes5.txt): 5x5 world,  5 steps,  envelopes at  2,2 2,3 4,4 5,3
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest5 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(5, 5, "tests/steps5.txt", "tests/states5.txt", "tests/envelopes5.txt");
    }

    /**
     * test6 (states6.txt steps6.txt envelopes6.txt): 5x5 world,  5 steps,  envelopes at  1,1 2,1 2,2 2,3
     * For each step the agent makes, check if the state is correct.
     **/
    @Test
    public void envelopeWorldTest6 () throws IOException, ContradictionException, TimeoutException {
        testMakeSeqOfSteps(5, 5, "tests/steps6.txt", "tests/states6.txt", "tests/envelopes6.txt");
    }

}
