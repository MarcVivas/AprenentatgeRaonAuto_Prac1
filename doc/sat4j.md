# SAT4J

# API DOCS

http://www.sat4j.org/maven234/apidocs/index.html

## Minimal needed modules

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;

/**
*   SAT solver object that stores the logical boolean formula with the rules
*   and current knowledge about not possible locations for Treasure
**/
    ISolver solver;
    
    
     solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        
        
## Gamma for Barcenas World

    /**
    * This function builds the initial logical formula of the agent and stores it
    * into the solver object.
    *
    *  @return returns the solver object where the formulas has been stored
    **/
    public ISolver buildGamma() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        int totalNumVariables;

        totalNumVariables = WorldLinealDim * 3;
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequencial order,
        // the variable indentifiers of all the variables
        actualLiteral = 1;

        pastBarcenas(); // Barcenas t-1, from 1,1 to n,n (1 clause)
        futureBarcenas(); // Barcenas t+1, from 1,1 to n,n (1 clause)
        pastBarcenasToFutureBarcenas(); // Barcenas t-1 -> Barcenas t+1 (nxn clauses)
        smells_implications(   ); // Smells implications (nxnxnxn clauses)

        notInFirstPosition(); // Not in the 1,1 clauses (2 clauses)

        return solver;
    }


    /**
    * Add the clauses that say that Barcenas must be in some position
    * with respect to the variables that talk about past positions
    **/
    public void pastBarcenas() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        BarcenasPastOffset = actualLiteral;
        VecInt pastClause = new VecInt();
        for (int i = 0; i < WorldLinealDim; i++) {
            pastClause.insertFirst(actualLiteral);
            actualLiteral++;
        }
        solver.addClause(pastClause);
    }

    /**
    * Add the clauses that say that Barcenas must be in some position
    * with respect to the variables that talk about future positions
    **/
    public void futureBarcenas() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        BarcenasFutureOffset = actualLiteral;
        VecInt futureClause = new VecInt();
        for (int i = 0; i < WorldLinealDim; i++) {
            futureClause.insertFirst(actualLiteral);
            actualLiteral++;
        }
        solver.addClause(futureClause);
    }

    /**
    * Add the clauses that say that if in the past we reached the conclusion
    * that Barcenas cannot be in a position (x,y), then this should be also true
    * in the future
    **/
    public void pastBarcenasToFutureBarcenas() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        for (int i = 0; i < WorldLinealDim; i++) {
            VecInt clause = new VecInt();
            clause.insertFirst(i + 1);
            clause.insertFirst(-(i + BarcenasFutureOffset));
            solver.addClause(clause);
        }
    }

    /**
    * Add the clauses that say that Barcenas can never (past and future) be in
    *  the first position
    **/
    public void notInFirstPosition() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        VecInt notInFuture = new VecInt();
        VecInt notInPast = new VecInt();
        notInFuture.insertFirst(-BarcenasFutureOffset);
        notInPast.insertFirst(-BarcenasPastOffset);
        solver.addClause(notInFuture);
        solver.addClause(notInPast);
    }

    /**
    * Add the cluses related to implications between smell sensor evidence and
    * forbidden positions of clauses
    **/
    public void smells_implications( ) throws
            UnsupportedEncodingException, FileNotFoundException, IOException, ContradictionException
    {
       // Store the identifier for the first variable of the
       // smell subset of variables
        SmellsOffset = actualLiteral;


        for (int k = 0; k < WorldLinealDim; k++) {
            int[] smell_coords =  linealToCoord(actualLiteral, SmellsOffset);
            int s_x = smell_coords[0];
            int s_y = smell_coords[1];
            // System.out.println( " Smell rules for "+s_x+","+s_y+" position" );
            for (int b_x = 1; b_x < WorldDim + 1; b_x++) {
                for (int b_y = 1; b_y < WorldDim + 1; b_y++) {
                    // If do not smell arround (sx,sy)
                    if ((b_x == s_x && b_y == s_y)
                            || (b_x + 1 == s_x && b_y == s_y)
                            || (b_x - 1 == s_x && b_y == s_y)
                            || (b_y + 1 == s_y && b_x == s_x)
                            || (b_y - 1 == s_y && b_x == s_x)) {
                        VecInt clause = new VecInt();
                        clause.insertFirst(actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);

                    } else {
                        VecInt clause = new VecInt();
                        clause.insertFirst(-actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);
                    }
                }
            }
            actualLiteral++;
        }
    }


    /**
     * Convert a coordinate pair (x,y) to the integer value  b_[x,y]
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
     * That is, from the identifier b_[x,y] to the coordinates  (x,y)
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



