package apryraz.eworld;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;


public class EnvelopeWorldEnv {
    /**
     * world dimension
     **/
    int WorldDim;
    /**
     * Location of the envelopes
     */
    Set<Position> envelopesPositions;


    /**
     * Class constructor
     *
     * @param dim          dimension of the world
     * @param envelopeFile File with list of envelopes locations
     **/
    public EnvelopeWorldEnv(int dim, String envelopeFile) {

        WorldDim = dim;
        envelopesPositions = new HashSet<>();
        loadEnvelopeLocations(envelopeFile);
    }

    /**
     * Load the set of envelopes locations
     *
     * @param envelopeFile name of the file that should contain a
     *                     set of envelope locations in a single line.
     *
     *
     * */
    public void loadEnvelopeLocations(String envelopeFile) {
        String[] envelopesList;
        String envelopes = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(envelopeFile));
            System.out.println("ENVELOPES FILE OPENED ...");
            envelopes = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Envelopes file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeWorldEnv.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        envelopesList = envelopes.split(" ");

        // Add the positions into the set
        for(int i = 0; i < envelopesList.length; i++){
            int xPos = Integer.parseInt(String.valueOf(envelopesList[i].charAt(0)));
            int yPos = Integer.parseInt(String.valueOf(envelopesList[i].charAt(2)));
            envelopesPositions.add(new Position(xPos, yPos));
        }

    }


    /**
     * Process a message received by the EFinder agent,
     * by returning an appropriate answer
     * It should answer to moveto and detectsat messages
     *
     * @param msg message sent by the Agent
     * @return a msg with the answer to return to the agent
     **/
    public AMessage acceptMessage(AMessage msg) {
        AMessage ans = new AMessage("voidmsg", "", "", "");

        msg.showMessage();
        if (msg.getComp(0).equals("moveto")) {
            Position nextPos = new Position(Integer.parseInt(msg.getComp(1)), Integer.parseInt(msg.getComp(2)));
            if (withinLimits(nextPos)) {
                ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2), "");
            } else
                ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2), "");

        } else {
            Position currentPos = new Position(Integer.parseInt(msg.getComp(1)), Integer.parseInt(msg.getComp(2)));
            ans = new AMessage("detectsat", msg.getComp(1), msg.getComp(2), "");

            // YOU MUST ANSWER HERE TO THE OTHER MESSAGE TYPE:
            //   ( "detectsat", "x" , "y", "" )
            //
        }
        return ans;

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
