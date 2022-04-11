package apryraz.eworld;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;


public class EnvelopeWorldEnv {
    /**
     * world dimension
     **/
    int WorldDim;
    /**
     * Locations of the envelopes
     */
    HashSet<Position> envelopesPositions;


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
     * @param envelopeFile name of the file that should contain all
     *                     the envelope locations in a single line.
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
            // Generate a new message using the sensors outputs
            Position currentPos = new Position(Integer.parseInt(msg.getComp(1)), Integer.parseInt(msg.getComp(2)));
            String sensorsOutput = getSensorsOutput(currentPos);
            ans = new AMessage(sensorsOutput, Integer.toString(currentPos.getX()), Integer.toString(currentPos.getY()), "");
        }
        return ans;
    }

    /**
     * This function returns a codification of the sensors output
     *
     * @param currentPos Position x,y of the agent
     * @return "1" -> Sensor 1 has detected an envelope
     *         "2" -> Sensor 2 has detected an envelope
     *         "3" -> Sensor 3 has detected an envelope
     *         "12" -> Sensor 1 and 2 have detected envelopes
     *         "23" -> Sensor 2 and 3 have detected envelopes
     *         "13" -> Sensor 1 and 3 have detected envelopes
     *         "123" -> Sensor 1, 2 and 3 have detected envelopes
     *         "" -> The sensors haven't detected any envelopes
     */
    public String getSensorsOutput(Position currentPos){
        String output = "";

        // Sensor 1 scope
        Position right = new Position(currentPos.getX() + 1, currentPos.getY());
        Position left = new Position(currentPos.getX() - 1, currentPos.getY());
        Position down = new Position(currentPos.getX(), currentPos.getY() - 1);
        Position up = new Position(currentPos.getX(), currentPos.getY() + 1);

        // Check if in the scope of sensor 1 there's an envelope
        if(envelopesPositions.contains(right) || envelopesPositions.contains(left) || envelopesPositions.contains(down) || envelopesPositions.contains(up)){
            output += "1";
        }

        // Sensor 2 scope
        Position leftDown = new Position(currentPos.getX() - 1, currentPos.getY() - 1);
        Position rightDown = new Position(currentPos.getX() + 1, currentPos.getY() - 1);
        Position leftUp = new Position(currentPos.getX() - 1, currentPos.getY() + 1);
        Position rightUp = new Position(currentPos.getX() + 1, currentPos.getY() + 1);

        // Check if in the scope of sensor 2 there's an envelope
        if(envelopesPositions.contains(leftDown) || envelopesPositions.contains(rightDown) || envelopesPositions.contains(leftUp) || envelopesPositions.contains(rightUp)){
            output += "2";
        }

        // Sensor 3 scope (is the current position of the agent)
        // Check if in the scope of sensor 3 there's an envelope
        if(envelopesPositions.contains(currentPos)){
            output += "3";
        }

        return output;
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
