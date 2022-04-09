package apryraz.eworld;

import java.util.Objects;

public class Position {
 /**


 **/
    public int x, y;

    public Position(int row, int col) {
        x = row;
        y = col;
    }



    @Override
    public boolean equals(Object o){
        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        // Check if o is an instance of Position or not
        if (!(o instanceof Position)) {
            return false;
        }

        // Cast o to Position so that we can compare data members
        Position c = (Position) o;

        // Compare the data and return accordingly
        return x == c.getX() && y == c.getY();
    }
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public int getX() {
        return x;
    }
    public int getY(){
        return y;
    }
}
