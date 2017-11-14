package uk.ac.bris.cs.scotlandyard.model;

public class MrXPlayer extends ScotlandYardPlayer {
    private int lastKnownLocation;
    MrXPlayer(PlayerConfiguration mrx){
        super(mrx);
        lastKnownLocation = 0;
    }

    public int getLastKnownLocation(){
        return lastKnownLocation;
    }

    public void updateLastKnownLocation(){
        this.lastKnownLocation = super.location();
    }

}
