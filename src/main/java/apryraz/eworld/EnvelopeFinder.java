

package apryraz.eworld;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;


/**
*  This agent performs a sequence of movements, and after each
*  movement it "senses" from the environment the resulting position
*  and then the outcome from the smell sensor, to try to locate
*  the position of Envelope
*
**/
public class EnvelopeFinder  {


/**
  * The list of steps to perform
**/
    ArrayList<Position> listOfSteps;
/**
* index to the next movement to perform, and total number of movements
**/
    int idNextStep, numMovements;
/**
*  Array of clauses that represent conclusions obtained in the last
* call to the inference function, but rewritten using the "past" variables
*  (t-1)
**/
    ArrayList<VecInt> futureToPast = new ArrayList<>();
/**
* the current state of knowledge of the agent (what he knows about
* every position of the world)
**/
    EFState efstate;
/**
*   The object that represents the interface to the Envelope World
**/
   EnvelopeWorldEnv EnvAgent;
/**
*   SAT solver object that stores the logical boolean formula with the rules
*   and current knowledge about not possible locations for Envelope
**/
    ISolver solver;
/**
*   Agent position in the world 
**/
    int agentX, agentY;
/**
*  Dimension of the world and total size of the world (Dim^2)
**/
    int WorldDim, WorldLinealDim;

/**
*    This set of variables CAN be used to mark the beginning of different sets
*    of variables in your propositional formula (but you may have more sets of
*    variables in your solution).
**/
    int EnvelopePastOffset;
    int EnvelopeFutureOffset;
    int Sensor1Offset;
    int actualLiteral;
    int Sensor2Offset;
    int Sensor3Offset;

    /**
     * Set used to identify the previous conclusions that were already added in previous steps
     */
    HashSet<VecInt> previousConsequences = new HashSet<>();
    /**
     The class constructor must create the initial Boolean formula with the
     rules of the Envelope World, initialize the variables for indicating
     that we do not have yet any movements to perform, make the initial state.

     @param WDim the dimension of the Envelope World

   **/
    public EnvelopeFinder(int WDim)
    {

        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        try {
            solver = buildGamma();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ContradictionException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        numMovements = 0;
        idNextStep = 0;
        System.out.println("STARTING Envelope FINDER AGENT...");


        efstate = new EFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        efstate.printState();
    }

    /**
      Store a reference to the Environment Object that will be used by the
      agent to interact with the Envelope World, by sending messages and getting
      answers to them. This function must be called before trying to perform any
      steps with the agent.

      @param environment the Environment object

    **/
    public void setEnvironment( EnvelopeWorldEnv environment ) {

         EnvAgent =  environment;
    }


    /**
      Load a sequence of steps to be performed by the agent. This sequence will
      be stored in the listOfSteps ArrayList of the agent.  Steps are represented
      as objects of the class Position.

      @param numSteps number of steps to read from the file
      @param stepsFile the name of the text file with the line that contains
                       the sequence of steps: x1,y1 x2,y2 ...  xn,yn

    **/
    public void loadListOfSteps( int numSteps, String stepsFile )
    {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(numSteps);
        for (int i = 0 ; i < numSteps ; i++ ) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     *    Returns the current state of the agent.
     *
     *    @return the current state of the agent, as an object of class EFState
    **/
    public EFState getState()
    {
        return efstate;
    }

    /**
    *    Execute the next step in the sequence of steps of the agent, and then
    *    use the agent sensor to get information from the environment
     *   so that the agent can discard not possible locations for the envelopes.
    *
    **/
    public void runNextStep() throws IOException,  ContradictionException, TimeoutException
    {
          
                                                            /*
          Add the conclusions obtained in the previous step
          but as clauses that use the "past" variables
                                                            */
          addLastFutureClausesToPastClauses();

          // Ask to move, and check whether it was successful
          processMoveAnswer( moveToNext( ) );


          // Next, use Detector sensor to discover new information
          processDetectorSensorAnswer( DetectsAt() );
           

          // Perform logical consequence questions for all the positions
          // of the Envelope World
          performInferenceQuestions();

          // Print the resulting knowledge matrix
          efstate.printState();
    }


    /**
    *   Ask the agent to move to the next position, by sending an appropriate
    *   message to the environment object. The answer returned by the environment
    *   will be returned to the caller of the function.
    *
    *   @return the answer message from the environment, that will tell whether the
    *           movement was successful or not.
    **/
    public AMessage moveToNext()
    {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE","","", ""));
        }
    }

    /**
    * Use agent "actuators" to move to (x,y)
    * We simulate this by telling to the World Agent (environment)
    * that we want to move, but we need the answer from it
    * to be sure that the movement was made with success
    *
    *  @param x  horizontal coordinate (row) of the movement to perform
    *  @param y  vertical coordinate (column) of the movement to perform
    *
    *  @return returns the answer obtained from the environment object to the
    *           moveto message sent
    **/
    public AMessage moveTo( int x, int y )
    {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (new Integer(x)).toString(), (new Integer(y)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

   /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
   **/
    public void processMoveAnswer ( AMessage moveans )
    {
        if ( moveans.getComp(0).equals("movedto") ) {
          agentX = Integer.parseInt( moveans.getComp(1) );
          agentY = Integer.parseInt( moveans.getComp(2) );
        
          System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")" );
        }
    }

    /**
     *   Send to the environment object the question:
     *   "Does the detector sense something around(agentX,agentY) ?"
     *
     *   @return return the answer given by the environment
    **/
    public AMessage DetectsAt( )
    {
        AMessage msg, ans;

        msg = new AMessage( "detectsat", (new Integer(agentX)).toString(), (new Integer(agentY)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ") Sensors output: " + ans.getComp(0));
        return ans;
    }


    /**
    *   Process the answer obtained for the query "Detects at (x,y)?"
    *   by adding the appropriate evidence clause to the formula
    *
    *   @param ans message obtained to the query "Detects at (x,y)?".
    *          It will a message with three fields: DetectorValue x y
    *
    *    DetectorValue must be a number that encodes all the valid readings 
    *    of the sensor given the envelopes in the 3x3 square around (x,y)
    **/
    public void processDetectorSensorAnswer( AMessage ans ) throws IOException, ContradictionException,  TimeoutException
    {

        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String sensorsOutput = ans.getComp(0);

         // Add the evidence clauses to Gamma to then be able to infer new NOT possible positions

        switch (sensorsOutput){
            case "1":
                // Sensor 1 has detected an envelope
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor2Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor3Offset))));
                break;
            case "2":
                // Sensor 2 has detected an envelope
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor2Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor3Offset))));
                break;
            case "3":
                // Sensor 3 has detected an envelope
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor2Offset))));
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor3Offset))));
                break;
            case "12":
                // Sensor 1 and 2 have detected envelopes
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor2Offset))));
                break;
            case "13":
                // Sensor 1 and 3 have detected envelopes
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor3Offset))));
                break;
            case "23":
                // Sensor 2 and 3 have detected envelopes
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor2Offset))));
                insertClause(new ArrayList<>(Arrays.asList(coordToLineal(x, y, Sensor3Offset))));
                break;
            case "":
                // Sensor 1,2 and 3 haven't detected envelopes
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor1Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor2Offset))));
                insertClause(new ArrayList<>(Arrays.asList(-coordToLineal(x, y, Sensor3Offset))));
                break;
        }
    }

    /**
    *  This function adds all the clauses stored in the list
    *  futureToPast to the formula stored in solver.
    *  It uses the addClause( VecInt ) function to add each clause to the solver
    *
    **/
    public void addLastFutureClausesToPastClauses() throws  IOException, ContradictionException, TimeoutException
    {
        while(futureToPast.size() > 0){
            VecInt clause = futureToPast.remove(0);
            solver.addClause(clause);
        }
    }

    /**
    * This function checks, using the future variables related
    * to possible positions of Envelope, whether it is a logical consequence
    * that an envelope is NOT at certain positions. This is checked for all the
    * positions of the Envelope World.
    *
    *  The logical consequences obtained, are stored in the futureToPast list
    * but using the variables corresponding to the "past" variables of the same positions
    *
    * An efficient version of this function should try to not add to the futureToPast
    * conclusions that were already added in previous steps, although this will not produce
    * any bad functioning in the reasoning process with the formula.
    **/
    public void  performInferenceQuestions() throws  IOException, ContradictionException, TimeoutException
    {
        // Generate all possible positions
        for(int x = 1; x <= WorldDim; x++){
            for(int y = 1; y <= WorldDim; y++){

                // Get variable number for position x,y in future variables
                int linealIndex = coordToLineal(x, y, EnvelopeFutureOffset);

                // Get the same variable, but in the past subset
                int linealIndexPast = coordToLineal(x, y, EnvelopePastOffset);

                // Gamma + Evidence + variablePositive is UNSAT?
                VecInt variablePositive = new VecInt();
                variablePositive.insertFirst(linealIndex);

                // Check if the conclusion hasn't appeared before
                if(!previousConsequences.contains(variablePositive)){
                    if (!(solver.isSatisfiable(variablePositive))) {
                        // Add conclusion to list, but rewritten with respect to "past" variables
                        previousConsequences.add(variablePositive);
                        VecInt concPast = new VecInt();
                        concPast.insertFirst(-(linealIndexPast));
                        futureToPast.add(concPast);
                        efstate.set( x , y , "X" );
                    }
                }
                // The conclusion has appeared earlier
                else
                {
                    efstate.set( x , y , "X" );
                }
            }
        }
    }

    /**
    * This function builds the initial logical formula of the agent and stores it
    * into the solver object.
    *
    *  @return returns the solver object where the formula has been stored
    **/
    public ISolver buildGamma() throws UnsupportedEncodingException, FileNotFoundException, IOException, ContradictionException
    {
        // Total number of boolean variables in gamma formula
        int totalNumVariables = WorldLinealDim * 5;


        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequential order,
        // the variable identifiers of all the variables
        actualLiteral = 1;

        // Add all the clauses
        generateClauses();

        return solver;
    }

    /**
     * This function is responsible to generate all the clauses of the gamma formula
     * @throws ContradictionException
     */
    public void generateClauses() throws ContradictionException {

        // Get the offsets
        EnvelopePastOffset = actualLiteral;
        EnvelopeFutureOffset = EnvelopePastOffset + WorldLinealDim;
        Sensor1Offset = EnvelopeFutureOffset + WorldLinealDim;
        Sensor2Offset = Sensor1Offset + WorldLinealDim;
        Sensor3Offset = Sensor2Offset + WorldLinealDim;

        // Clause that says that the envelopes must be in some position
        // with respect to the variables that talk about past positions
        VecInt pastClause = new VecInt();

        // Clause that says that the envelopes must be in some position
        // with respect to the variables that talk about future positions
        VecInt futureClause = new VecInt();

        // For each possible position in the world generate the respective clauses
        for (int i = 0; i < WorldLinealDim; i++) {
            // For the same position, get the respective literal for each variable
            int currentEnvPastLiteral = actualLiteral;
            int currentEnvFutureLiteral = actualLiteral + WorldLinealDim;
            int currentSensor1Literal = actualLiteral + WorldLinealDim * 2;
            int currentSensor2Literal = actualLiteral + WorldLinealDim * 3;
            int currentSensor3Literal = actualLiteral + WorldLinealDim * 4;

            // Get current 2-D position
            int[] sensorCoords =  linealToCoord(actualLiteral, 1);
            int sensor_x = sensorCoords[0];
            int sensor_y = sensorCoords[1];

            // Add the variables to the future and past clauses that say that the envelopes must be in some position
            pastClause.insertFirst(currentEnvPastLiteral);
            futureClause.insertFirst(currentEnvFutureLiteral);


            /*
             * Add the clause that says that if in the past we reached the conclusion
             * that an envelope cannot be in a position (x,y), then this should be also true
             * in the future
             */
            insertClause(new ArrayList<>(Arrays.asList(currentEnvPastLiteral, -currentEnvFutureLiteral)));


            /*
             * Add the clause related to implications between the sensor 1 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "1"),
                    new ArrayList<>(Arrays.asList(-currentSensor1Literal, currentSensor2Literal, currentSensor3Literal))
            );

            /*
             * Add the clause related to implications between the sensor 2 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "2"),
                    new ArrayList<>(Arrays.asList(-currentSensor2Literal, currentSensor1Literal, currentSensor3Literal))
            );

            /*
             * Add the clause related to implications between the sensor 3 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "3"),
                    new ArrayList<>(Arrays.asList(-currentSensor3Literal, currentSensor1Literal, currentSensor2Literal))
            );

            /*
             * Add the clause related to implications between the sensor 1 and 2 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "12"),
                    new ArrayList<>(Arrays.asList(-currentSensor1Literal, -currentSensor2Literal))
            );

            /*
             * Add the clause related to implications between the sensor 1 and 3 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "13"),
                    new ArrayList<>(Arrays.asList(-currentSensor1Literal, -currentSensor3Literal))
            );

            /*
             * Add the clause related to implications between the sensor 2 and 3 evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), "23"),
                    new ArrayList<>(Arrays.asList(-currentSensor2Literal, -currentSensor3Literal))
            );

            /*
             * Add the clause related to no detection implications evidence and
             * the envelopes forbidden positions
             */
            insertSensorClause(
                    getForbiddenEnvelopesPositions(new Position(sensor_x, sensor_y), ""),
                    new ArrayList<>(Arrays.asList(currentSensor1Literal, currentSensor2Literal, currentSensor3Literal))
            );

            actualLiteral++;
        }
        solver.addClause(pastClause);
        solver.addClause(futureClause);
    }

    /**
     * For a specific position, add all the clauses related to the implications between the sensors
     * that are activated and the forbidden positions.
     * @param noEnvelopesPositions Positions you know for sure where there won't be an envelope
     * @param clause List that contains the sensor variables of the clause, the sensors that are or not activated
     * @throws ContradictionException
     */
    public void insertSensorClause(ArrayList<Position> noEnvelopesPositions, ArrayList<Integer> clause) throws ContradictionException {
        // Positions you know for sure where there won't be an envelope
        for(Position pos: noEnvelopesPositions){
            if(withinLimits(pos)){
                // Add to the clause the position you know there won't be an envelope
                clause.add(-coordToLineal(pos.getX(), pos.getY(), EnvelopeFutureOffset));
                // Clause completed
                insertClause(clause);
                // Get back the clause without the position you know there won't be an envelope
                clause.remove(clause.size()-1);
            }
        }
    }

    /**
     * Get the positions where you are sure the envelopes won't be
     * @param pos Current position of the agent
     * @param sensorsOutput Output of the sensors
     * @return A list of positions where you know for sure the envelopes won't be
     */
    public ArrayList<Position> getForbiddenEnvelopesPositions (Position pos, String sensorsOutput){
        int x = pos.getX();
        int y = pos.getY();
        ArrayList<Position> output = new ArrayList<>();

        switch (sensorsOutput){
            case "1":
                output = getSensor2Scope(pos);
                output.addAll(getSensor3Scope(pos));
                break;
            case "2":
                output = getSensor1Scope(pos);
                output.addAll(getSensor3Scope(pos));
                break;
            case "3":
                output = getSensor1Scope(pos);
                output.addAll(getSensor2Scope(pos));
                break;
            case "12":
                output = getSensor3Scope(pos);
                break;
            case "13":
                output = getSensor2Scope(pos);
                break;
            case "23":
                output = getSensor1Scope(pos);
                break;
            case "":
                output = getSensor1Scope(pos);
                output.addAll(getSensor2Scope(pos));
                output.addAll(getSensor3Scope(pos));
                break;
        }
       return output;
    }

    /**
     * This function puts into a list the scope of the sensor 1
     * @param pos The current position of the agent
     * @return A list of positions the sensor 1 is able to detect
     */
    public ArrayList<Position> getSensor1Scope (Position pos){
        int x = pos.getX();
        int y = pos.getY();
        return new ArrayList<>(Arrays.asList(
                new Position(x+1, y),
                new Position(x-1,y),
                new Position(x, y-1),
                new Position(x, y+1)
        ));
    }

    /**
     * This function puts into a list the scope of the sensor 2
     * @param pos The current position of the agent
     * @return A list of positions the sensor 2 is able to detect
     */
    public ArrayList<Position> getSensor2Scope (Position pos){
        int x = pos.getX();
        int y = pos.getY();
        return new ArrayList<>(Arrays.asList(
                new Position(x-1, y-1),
                new Position(x+1,y-1),
                new Position(x-1, y+1),
                new Position(x+1, y+1)
        ));
    }

    /**
     * This function puts into a list the scope of the sensor 3
     * @param pos The current position of the agent
     * @return A list of positions the sensor 3 is able to detect
     */
    public ArrayList<Position> getSensor3Scope (Position pos){
        int x = pos.getX();
        int y = pos.getY();
        return new ArrayList<>(Arrays.asList(
                new Position(x, y)
        ));
    }

    /**
     * This function builds and adds a clause to the solver
     * @param vars Array of integers that contains all the variables of the clause with
     *             their respective symbol.
     * @throws ContradictionException
     */
    public void insertClause(ArrayList<Integer> vars) throws ContradictionException {
        VecInt clause = new VecInt();
        for(int variable: vars){
            clause.insertFirst(variable);
        }
        solver.addClause(clause);
    }

     /**
     * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
     * of variable that stores that information in the formula, using
     * offset as the initial index for that subset of position variables
     * (past and future position variables have different variables, so different
     * offset values)
     *
     *  @param x x coordinate of the position variable to encode
     *  @param y y coordinate of the position variable to encode
     *  @param offset initial value for the subset of position variables
     *         (past or future subset)
     *  @return the integer indentifer of the variable  b_[x,y] in the formula
    **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }

    /**
     * Perform the inverse computation to the previous function.
     * That is, from the identifier t_[x,y] to the coordinates  (x,y)
     *  that it represents
     *
     * @param lineal identifier of the variable
     * @param offset offset associated with the subset of variables that
     *        lineal belongs to
     * @return array with x and y coordinates
    **/
    public int[] linealToCoord(int lineal, int offset)
    {
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = ((lineal-1) % WorldDim) + 1;
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }

    /**
     * Check if position x,y is within the limits of the
     * WorldDim x WorldDim   world
     *
     * @param pos x,y coordinate
     * @return true if (x,y) is within the limits of the world
     **/
    public boolean withinLimits(Position pos) {
        return (pos.getX() >= 1 && pos.getX() <= WorldDim && pos.getY() >= 1 && pos.getY() <= WorldDim);
    }
}
