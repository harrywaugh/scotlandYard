package uk.ac.bris.cs.scotlandyard.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

import java.util.Set;
import java.util.function.Consumer;

public class MinPlayer implements Player {

    MinPlayer() {}

    @Override
    public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {}

}
