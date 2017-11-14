package uk.ac.bris.cs.scotlandyard.ai;

import uk.ac.bris.cs.gamekit.graph.*;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;
import java.util.function.Consumer;


@ManagedAI("Sions Angel")
public class SionsAngel implements PlayerFactory {
    private LinkedList<Integer> queue = new LinkedList<>();
    private int parents[] = new int[200];
    private int location, currentRound;
    private int threshold;

    @Override
    public Player createPlayer(Colour colour) {
        return (ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) -> {
            this.location = location;
            currentRound = view.getCurrentRound();
            callback.accept(minimax(view, 2)); // Calling minimax to choose a move to be made.
        };
    }

    // Works out the upper fifth of the list of values given
    private int getUpperFifth(List<Integer> values) {
        Collections.sort(values);
        return values.get((values.size() * 4) / 5);
    }

    // This method implements the minimax algorithm. Calls the first min which will starts the recursive calls.
    private Move minimax(ScotlandYardView view, int moves) {
        ScotlandYardModelTwo model = createModel(view); // Creating a model for min to use.
        int highScore = Integer.MIN_VALUE; // The default high score.
        List<Spectator> spectators = new ArrayList<>();
        spectators.addAll(model.getSpectators());
        Move bestMove = new PassMove(Colour.Black); // The default best move.
        int score;
        List<Move> validMoves = new ArrayList<>();
        if (!model.getRounds().get(currentRound)) {
            validMoves.addAll(model.generateMoves(Colour.Black, model.getPlayerLocation(Colour.Black))); // Only look at single moves when the current round is not a reveal round.
        } else {
            validMoves.addAll(model.validMoves(Colour.Black)); // Look at double moves if the current round is a reveal round.
            if (model.getPlayerTickets(Colour.Black, Ticket.Double) > 0) {
                validMoves.removeAll(model.generateMoves(Colour.Black, model.getPlayerLocation(Colour.Black))); // Remove single moves so only double moves should be checked on reveal rounds.
            }
        }
        List<Integer> scores = new ArrayList<>();
        for (Move move: validMoves) { // Find the score of all valid moves and use this to find the correct threshold to prune with
            ScotlandYardModelTwo clone = update(model, move); // Update the model with the given move.
            scores.add(mrXScore(clone, clone.validMoves(Colour.Black)));
        }
        threshold = getUpperFifth(scores); // Find the correct threshold
        validMoves = filterDuplicateLocationTickets(validMoves); // Filter all moves with the same locations
        MovingVisitor visitor = new MovingVisitor();
        for (Move move : validMoves) {
            ScotlandYardModelTwo clone = update(model, move); // Update the model with the given move.
            move.visit(visitor);
            currentRound++; // Increment the round so that min has the correct currentRound to perform checks with
            if (!visitor.singleMove) currentRound++; // Increment the round a second time if a double move is being made
            score = min(clone, moves - 1); // Run min with the cloned view and a decremented moves value
            if (score > highScore) {
                highScore = score; // Update the high score if needed
                bestMove = move; // Update the best move if needed
            }
            currentRound--; // Decrement the round so that the next run of the for loop has the correct currentRound
            if (!visitor.singleMove) currentRound--; // Decrement another time if a double move was made.
        }
        bestMove.visit(visitor);
        return selectBestTicketForLocation(visitor.location, model); // Select the best ticket for the move and return the move using this ticket
    }

    // Check if mrX is captured given a list of moves and mrX's location
    private boolean mrXCaptured(List<Move> round, int mrXLocation) {
        MovingVisitor visitor = new MovingVisitor();
        for (Move move : round) {
            move.visit(visitor);
            if(mrXLocation == visitor.location) return true;
        }
        return false;
    }

    // Filter all moves that have duplicate locations
    private List<Move> filterDuplicateLocationTickets(List<Move> moves) {
        MovingVisitor visitor = new MovingVisitor();
        List<Move> newMoves = new ArrayList<>();
        List<Integer> destinations = new ArrayList<>();
        for (Move move : moves) {
            move.visit(visitor);
            if (!destinations.contains(visitor.location)) {
                destinations.add(visitor.location);
                newMoves.add(move);
            }
        }
        return newMoves;
    }

    // Select the move that uses the lowest priority ticket to get to a given destination
    private Move selectBestTicketForLocation(int moveLocation, ScotlandYardModel model) {
        List<Move> moves = new ArrayList<>();
        moves.addAll(model.validMoves(Colour.Black));
        MovingVisitor visitor = new MovingVisitor();
        List<Move> locationDifferingMoves = new ArrayList<>(); //Remove all moves that don't move to the same location as the best move
        for (Move move : moves) {
            move.visit(visitor);
            if (moveLocation != visitor.location) {
                locationDifferingMoves.add(move);
            }
        }
        moves.removeAll(locationDifferingMoves);

        //Score each move that is left, then pick the best
        int bestScore = 0;
        Move bestMove = moves.get(0);
        for (Move move : moves) {
            int tempScore = 0;
            move.visit(visitor);
            if (visitor.singleMove) tempScore +=1000; // Prioritising single moves over double moves to get to the same destination
            if (currentRound != 0 && model.getRounds().get(currentRound - 1)) {
                if (visitor.firstMove.ticket() == Ticket.Secret) tempScore += 20;
            }
            // Makes sure that we use the lower priority tickets for the first move as mrX will get more score if he does
            if (visitor.firstMove.ticket() == Ticket.Taxi) tempScore += 15;
            else if (visitor.firstMove.ticket() == Ticket.Bus) tempScore += 10;
            else if (visitor.firstMove.ticket() == Ticket.Underground) tempScore += 5;
            if (!visitor.singleMove) { // If double move
                if (model.getRounds().get(currentRound)) {
                    if (visitor.secondMove.ticket() == Ticket.Secret) tempScore += 20; // Prioritise secret moves on reveal rounds
                }
                // Makes sure that we use the lower priority tickets for the same second as mrX will get more score if he does
                if (visitor.secondMove.ticket() == Ticket.Taxi) tempScore += 15;
                else if(visitor.secondMove.ticket() == Ticket.Bus) tempScore += 10;
                else if(visitor.secondMove.ticket() == Ticket.Underground) tempScore += 5;
            }
            if (tempScore > bestScore) {
                bestScore = tempScore; // Update the high score if needed
                bestMove = move; // Update the best move if needed
            }
        }
        return bestMove; // Return the best move
    }

    // Returns the minimum of the max calls or the minimum of the values returned by mrXScore
    private int min(ScotlandYardModelTwo model, int moves) {
        if (model.isGameOver()) return mrXScore(model, model.validMoves(Colour.Black)); // If game is over just return mrXScore. Don't keep searching the tree
        int minimum = Integer.MAX_VALUE;
        int score;
        LinkedList<List<Move>> roundQueue = new LinkedList<>();
        LinkedList<List<Move>> listOfLists = new LinkedList<>();
        for (Colour colour : model.getPlayers()) {
            if (colour != Colour.Black) {
                List<Move> validMovesList = new ArrayList<>();
                validMovesList.addAll(model.validMoves(colour));
                validMovesList = filterDuplicateLocationTickets(validMovesList);
                listOfLists.add(validMovesList);
            }
        }
        generatePermutations(listOfLists, roundQueue, 0, new ArrayList<>()); // Generating a list of all possible combinations of moves from the detectives
        roundQueue = filterDuplicateLocationRounds(roundQueue); // Filter all moves with the same locations
        for (List<Move> round : roundQueue) { // Iterating through all possible combinations of moves from the detectives
            if (mrXCaptured(round, model.getPlayerLocation(Colour.Black))) return -1000; // If mrX is captured just return the lowest possible score
            ScotlandYardModelTwo temp = updateList(model, round);
            score = mrXScore(temp, temp.validMoves(Colour.Black));
            if (moves == 0 || score < threshold) return score; // If the bottom of the tree has been reached or if the move to get here was too weak in the first place then stop searching the tree
            else score = max(temp, moves - 1); // Continue searching the tree
            if (score < minimum) minimum = score; // Update the minimum if necessary
        }
        return minimum; // Return the minimum score
    }

    // Filter all moves with the same locations
    private LinkedList<List<Move>> filterDuplicateLocationRounds(LinkedList<List<Move>> roundQueue) {
        List<List<Move>> duplicateRounds = new ArrayList<>();
        for (List<Move> round: roundQueue) {
            List<Integer> locations = new ArrayList();
            MovingVisitor visitor = new MovingVisitor();
            for (Move move: round) {
                move.visit(visitor);
                if (locations.contains(visitor.location)) duplicateRounds.add(round);
                else locations.add(visitor.location);
            }
        }
        for (List<Move> round: duplicateRounds) {
            roundQueue.remove(round);
        }
        return roundQueue;
    }

    private int max(ScotlandYardModelTwo model, int moves) {
        if (model.isGameOver()) return mrXScore(model, model.validMoves(Colour.Black)); // If game is over just return mrXScore. Don't keep searching the tree
        int maximum = Integer.MIN_VALUE;
        int score;
        List<Move> validMoves = new ArrayList<>();
        if (!model.getRounds().get(currentRound)) {
            validMoves.addAll(model.generateMoves(Colour.Black, model.getPlayerLocation(Colour.Black))); // Only look at single moves when the current round is not a reveal round.
        } else {
            validMoves.addAll(model.validMoves(Colour.Black)); // Look at double moves if the current round is a reveal round.
        }
        validMoves = filterDuplicateLocationTickets(validMoves); // Filter all moves with the same locations
        for (Move move : validMoves) {
            ScotlandYardModelTwo temp = update(model, move);
            MovingVisitor visitor = new MovingVisitor();
            move.visit(visitor);
            currentRound++; // Increment the round so that min has the correct currentRound to perform checks with
            if (!visitor.singleMove) currentRound++; // Increment the round a second time if a double move is being made
            score = mrXScore(temp, temp.validMoves(Colour.Black));
            if (moves == 0  || score < threshold) { // If the bottom of the tree has been reached or if the move to get here was too weak in the first place then stop searching the tree
                currentRound--; // Decrement the round so that the next run of the for loop has the correct currentRound
                if (!visitor.singleMove) currentRound--; // Decrement another time if a double move was made.
                return score;
            } else {
                score = min(temp, moves - 1); // Continue searching the tree
            }
            if(score > maximum) maximum = score; // Update the maximum if necessary
            currentRound--; // Decrement the round so that the next run of the for loop has the correct currentRound
            if (!visitor.singleMove) currentRound--; // Decrement another time if a double move was made.
        }
        return maximum; // Return the maximum score
    }

    // This function scores mrXs current location.
    private int mrXScore(ScotlandYardModelTwo model, Set<Move> validMoves) {
        int score = 0;
        if (model.isGameOver()) {
            if (model.getWinningPlayers().contains(Colour.Black)) { // Check whether or not Black or the Detectives have won
                score = 10000;
                return score;
            } else {
                score = -10000;
                return score;
            }
        }
        score += validMoves.size() * 2; // Prioritises moves with larger amounts of valid moves
        for (Colour colour : model.getPlayers()) {
            if (colour != Colour.Black) {
                int d = distance(model.getGraph(), model.getPlayerLocation(Colour.Black), model.getPlayerLocation(colour)); // Calling the distance method
                if (d==1) {
                    score -= 150; // If a player is one move away then getting away from them has very high priority
                }
                score += 40 * (d-3) / d; // This function scores his distance from other players. Closer players lose him a lot more score
            }
        }
        return score;
    }

    // This method returns the distance from the start and end nodes given
    private int distance(Graph<Integer, Transport> graph, int start, int end) {
        queue.clear();
        queue.add(start);
        for (int i = 0; i < parents.length; i++) { // Reset the parents array back to 0 for when distance is called again
            parents[i] = 0;
        }
        parents[start] = -1;
        return bfs(graph, queue.peek(), end).size();
    }

    // This method returns the list of nodes taken between two nodes
    private List<Integer> bfs(Graph<Integer, Transport> graph, int head, int end) {
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(head));
        if (pathTo(head).size() > 3) return pathTo(head); // Limits the search depth of bfs
        for (Edge<Integer, Transport> neighbour : edges) {
            if (parents[neighbour.destination().value()] == 0) {
                parents[neighbour.destination().value()] = head;
                queue.addFirst(neighbour.destination().value());
            }
            if (neighbour.destination().value() == end) return pathTo(end);
        }
        return bfs(graph, queue.pollLast(), end);
    }

    // Given a list of nodes, returns the route from the start node to the end node
    private List<Integer> pathTo(int end) {
        List<Integer> route = new ArrayList<>();
        int currentIndex = end;
        while (currentIndex != -1) {
            route.add(currentIndex);
            currentIndex = parents[currentIndex];
        }
        return route;
    }

    // Creates a ScotlandYardModelTwo from a given ScotlandYardView with equivalent locations, tickets and rounds
    private ScotlandYardModelTwo createModel(ScotlandYardView view) {
        List<PlayerConfiguration.Builder> players = new ArrayList<>();
        for (Colour colour : view.getPlayers()) {
            Map<Ticket, Integer> map = new HashMap<>();
            for (Ticket ticket : Ticket.values()) {
                map.put(ticket, view.getPlayerTickets(colour,ticket));
            }
            if (colour == Colour.Black) {
                players.add(new PlayerConfiguration.Builder(colour).at(location).with(map).using(new MinPlayer()));
            } else {
                players.add(new PlayerConfiguration.Builder(colour).at(view.getPlayerLocation(colour)).with(map).using(new MinPlayer()));
            }
        }

        PlayerConfiguration mrX = players.get(0).build();
        PlayerConfiguration firstDetective = players.get(1).build();

        PlayerConfiguration restOfDetectives[] = new PlayerConfiguration[view.getPlayers().size() - 2];
        for (int i = 2; i < players.size(); i++) {
            restOfDetectives[i - 2] = players.get(i).build();
        }

        return new ScotlandYardModelTwo(view.getRounds(), view.getGraph(), mrX, firstDetective, restOfDetectives);
    }

    // Updates a given model with a lsit of moves, one by one
    private ScotlandYardModelTwo updateList(ScotlandYardModelTwo model, List<Move> moves) {
        for (Move move : moves) {
            model = update(model, move);
        }
        return model;
    }

    // Updates a given model with a single move.
    private ScotlandYardModelTwo update(ScotlandYardModelTwo model, Move move) {
        MovingVisitor visitor = new MovingVisitor();
        move.visit(visitor);
        List<PlayerConfiguration> players = new ArrayList<>();
        List<Integer> locations = new ArrayList<>();
        List<Map<Ticket, Integer>> maps = new ArrayList<>();

        for (Colour colour : model.getPlayers()) {
            Map<Ticket, Integer> map = new HashMap<>();
            for (Ticket ticket : Ticket.values()) {
                map.put(ticket, model.getPlayerTickets(colour, ticket));
            }
            maps.add(map);
            locations.add(model.getPlayerLocation(colour));
        } // After this for loop, we have a list of maps with all the old tickets, and a list of all old locations.

        for (Colour colour : model.getPlayers()) {
            if (move.colour() == colour) {
                if (move.colour() != Colour.Black) {
                    if (!visitor.passMove) {
                        locations.set(model.getPlayers().indexOf(colour), visitor.location);
                        maps.get(model.getPlayers().indexOf(colour)).put(visitor.firstMove.ticket(), model.getPlayerTickets(colour, visitor.firstMove.ticket()) - 1);
                        maps.get(0).put(visitor.firstMove.ticket(), model.getPlayerTickets(Colour.Black, visitor.firstMove.ticket()) + 1);
                    }
                } else {
                    locations.set(0, visitor.location);
                    maps.get(0).put(visitor.firstMove.ticket(), model.getPlayerTickets(colour, visitor.firstMove.ticket()) - 1);
                    if (!visitor.singleMove) maps.get(0).put(visitor.secondMove.ticket(), model.getPlayerTickets(colour, visitor.secondMove.ticket()) - 1);
                }
            }
        }

        for (Colour colour : model.getPlayers()) {
            players.add(new PlayerConfiguration.Builder(colour).at(locations.get(model.getPlayers().indexOf(colour))).with(maps.get(model.getPlayers().indexOf(colour))).using(new MinPlayer()).build());
        }

        PlayerConfiguration mrX = players.get(0);
        PlayerConfiguration firstDetective = players.get(1);

        PlayerConfiguration restOfDetectives[] = new PlayerConfiguration[model.getPlayers().size() - 2];
        for (int i = 2; i < players.size(); i++) {
            restOfDetectives[i - 2] = players.get(i);
        }

        return new ScotlandYardModelTwo(model.getRounds().subList(0, model.getRounds().size()), model.getGraph(), mrX, firstDetective, restOfDetectives);
    }

    // Given work out all possible combinations from the list of lists of moves given and return it in the second argument given
    private void generatePermutations(List<List<Move>> Lists, List<List<Move>> result, int depth, List<Move> current) {
        if (depth != Lists.size()) {
            for (int i = 0; i < Lists.get(depth).size(); ++i) {
                current.add(Lists.get(depth).get(i));
                generatePermutations(Lists, result, depth + 1, current);
                current = new ArrayList<>(current);
                current = current.subList(0, depth);
            }
        } else result.add(current);
    }

}
