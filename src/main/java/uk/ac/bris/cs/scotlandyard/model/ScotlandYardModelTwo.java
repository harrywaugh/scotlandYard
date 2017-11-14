package uk.ac.bris.cs.scotlandyard.model;

import uk.ac.bris.cs.gamekit.graph.Graph;

import java.util.List;


public class ScotlandYardModelTwo extends ScotlandYardModel {
    public ScotlandYardModelTwo(List<Boolean> rounds, Graph<Integer, Transport> graph,
                                PlayerConfiguration mrX, PlayerConfiguration firstDetective,
                                PlayerConfiguration... restOfTheDetectives){
        super(rounds, graph, mrX, firstDetective, restOfTheDetectives);
    }
    @Override
    public int getPlayerLocation(Colour colour){
        return super.player(colour).location();
    }

}
