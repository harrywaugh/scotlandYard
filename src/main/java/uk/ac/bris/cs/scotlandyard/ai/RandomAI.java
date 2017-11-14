package uk.ac.bris.cs.scotlandyard.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

/**
 * An AI that randomly picks a move
 */
@ManagedAI(value = "Random")
public class RandomAI implements PlayerFactory {

	private final Random random = new Random();

	@Override
	public Player createPlayer(Colour colour) {
		return (ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) -> {
			callback.accept(new ArrayList<>(moves).get(random.nextInt(moves.size())));
		};
	}

}
