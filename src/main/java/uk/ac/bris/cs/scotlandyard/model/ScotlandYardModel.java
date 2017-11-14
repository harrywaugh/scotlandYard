package uk.ac.bris.cs.scotlandyard.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import static uk.ac.bris.cs.scotlandyard.model.Colour.Black;

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private Collection<Spectator> spectators = new CopyOnWriteArrayList<>();
    private Graph<Integer, Transport> graph;
    private List<Boolean> rounds;
    private MrXPlayer mrX;
    private List<ScotlandYardPlayer> players = new CopyOnWriteArrayList<>();
    private List<Colour> colours = new CopyOnWriteArrayList<>();
    private int currentPlayer, currentRound;
    private boolean gameOver;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

        gameOver = false;
	    currentPlayer = 0;
        currentRound = 0;
		this.graph = Objects.requireNonNull(graph);
		this.rounds = Objects.requireNonNull(rounds);
        if (rounds.isEmpty()) throw new IllegalArgumentException("Empty rounds.");
        if (graph.isEmpty()) throw new IllegalArgumentException("Empty graph.");
        if (mrX.colour != Black) throw new IllegalArgumentException("MrX should be Black.");
        this.mrX = new MrXPlayer((Objects.requireNonNull(mrX)));
        players.add(this.mrX);
        colours.add(mrX.colour);
        List<Integer> locations = new CopyOnWriteArrayList<>();
        locations.add(mrX.location);
        ArrayList<PlayerConfiguration> detectives = new ArrayList<>();
        detectives.add(firstDetective);
        for (PlayerConfiguration configuration : restOfTheDetectives) {
            detectives.add(Objects.requireNonNull(configuration));
        }
        for (PlayerConfiguration detective : detectives) {
            if (colours.contains(detective.colour)) throw new IllegalArgumentException("Duplicate colour.");
            if (locations.contains(detective.location)) throw new IllegalArgumentException("Duplicate location.");
            colours.add(detective.colour);
            locations.add(detective.location);
            players.add(new ScotlandYardPlayer(detective));
            for (Ticket ticket : Ticket.values()) {
                if (!detective.tickets.containsKey(ticket)) throw new IllegalArgumentException("A detective does not have all tickets.");
            }
            if (detective.tickets.get(Ticket.Secret) > 0 || detective.tickets.get(Ticket.Double) > 0)
            	throw new IllegalArgumentException("Detectives cannot have secret or double tickets.");
        }
        for (Ticket ticket : Ticket.values()) {
            if (!this.mrX.tickets().containsKey(ticket)) throw new IllegalArgumentException("mrX does not have all tickets.");
        }
        isGameOver();
    }

	@Override
	public void registerSpectator(Spectator spectator) {
	    if (!spectators.contains(spectator)) spectators.add(Objects.requireNonNull(spectator));
        else throw new IllegalArgumentException("Spectator already exists.");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
        if (spectators.contains(spectator)) spectators.remove(spectator);
        else if (spectator == null) throw new NullPointerException("Spectator is null.");
        else throw new IllegalArgumentException("Spectator doesn't exist.");
	}

	protected ScotlandYardPlayer player(Colour colour) {
	    return players.get(colours.indexOf(colour));
	}

	public Set<Move> validMoves(Colour colour) {
	    Set<Move> moves = new HashSet<>();
	    if (colour == Black) {
			Set<TicketMove> firstMoves = new HashSet<>();
			firstMoves.addAll(generateMoves(Black, mrX.location()));
            moves.addAll(firstMoves);
	        if (mrX.hasTickets(Ticket.Double) && currentRound < rounds.size() - 1) {
                for (TicketMove first: firstMoves) {
                    Set<TicketMove> secondMoves = new HashSet<>();
                    secondMoves.addAll(generateMoves(Black, first.destination()));
                    for (TicketMove second: secondMoves) {
                        if (first.ticket() == second.ticket()) {
                            if (mrX.hasTickets(first.ticket(), 2)) {
                                moves.add(new DoubleMove(Black, first, second));
                            }
                        } else if (mrX.hasTickets(first.ticket()) && mrX.hasTickets(second.ticket())) {
                                moves.add(new DoubleMove(Black, first, second));
                        }
                    }
                }
            }
        } else {
            moves.addAll(generateMoves(colour, player(colour).location()));
            if (moves.isEmpty()) moves.add(new PassMove(colour));
        }
        return Collections.unmodifiableSet(moves);
    }

    public Set<TicketMove> generateMoves(Colour colour, int startLocation) {
        Set<TicketMove> moves = new HashSet<>();
        Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(startLocation));
        List<Integer> locations = new ArrayList<>();
        for (ScotlandYardPlayer player : players) {
            if (!player.isMrX()) locations.add(player.location());
        }
        for (Edge<Integer, Transport> edge : edges) {
            if (player(colour).hasTickets(Ticket.fromTransport(edge.data())) && !locations.contains(edge.destination().value())) {
                moves.add(new TicketMove(colour, Ticket.fromTransport(edge.data()), edge.destination().value()));
                if (colour == Black && mrX.hasTickets(Ticket.Secret)) {
                    moves.add(new TicketMove(Black, Ticket.Secret, edge.destination().value()));
                }
            }
        }
        return moves;
    }

	@Override
	public void startRotate() {
        if (!isGameOver()) {
            currentPlayer = 0;
            mrX.player().makeMove(this, mrX.location(), validMoves(Black), this);
        } else throw new IllegalStateException("Game is over");
    }

	@Override
	public Collection<Spectator> getSpectators() {
	    return Collections.unmodifiableCollection(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
	    return Collections.unmodifiableList(colours);
	}

    private boolean areDetectivesStuck() {
        for (ScotlandYardPlayer player : players) {
            if (player != mrX && !generateMoves(player.colour(), player.location()).isEmpty()) return false;
        }
        return true;
    }

    private boolean isMrXStuck() {
        return validMoves(Black).isEmpty();
    }

    private boolean isMrXCaptured() {
        for (ScotlandYardPlayer player : players) {
            if (player != mrX && player.location() == mrX.location()) {
                return true;
            }
        }
        return false;
    }

	@Override
	public Set<Colour> getWinningPlayers() {
	    Set<Colour> winners = new HashSet<>();
		if (isMrXCaptured() || isMrXStuck()) {
		    winners.addAll(colours);
		    winners.remove(Black);
        } else if (currentRound == rounds.size() || areDetectivesStuck()) winners.add(Black);
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public int getPlayerLocation(Colour colour) {
	    if (colour == Black) {
            if (isRevealRound()) mrX.updateLastKnownLocation();
            return mrX.getLastKnownLocation();
        }
        return player(colour).location();
	}

	@Override
	public int getPlayerTickets(Colour colour, Ticket ticket) {
	    return player(colour).tickets().get(ticket);
	}

	@Override
	public boolean isGameOver() {
        gameOver = (currentRound == rounds.size() || areDetectivesStuck() || isMrXStuck() || isMrXCaptured());
	    return gameOver;
	}

	@Override
	public Colour getCurrentPlayer() {
	    return colours.get(currentPlayer);
	}

	@Override
	public int getCurrentRound() {
		return currentRound ;
	}

	@Override
	public boolean isRevealRound() {
        if (currentRound == 0) return false;
		return rounds.get(currentRound - 1);
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
	    return new ImmutableGraph<>(graph);
	}

    private void mrXMoveLocation() {
        if (rounds.get(currentRound)) mrX.updateLastKnownLocation();
    }

    private int mrXMoveLocation(MovingVisitor visitor) {
        if (rounds.get(currentRound + 1)) return visitor.location;
        else return mrX.getLastKnownLocation();
    }

    @Override
    public void accept(Move move) {
        if (validMoves(move.colour()).contains(move)) {
            MovingVisitor visitor = new MovingVisitor();
            move.visit(visitor);
            if (currentPlayer != 0) {
                for (Spectator s : spectators ) {
                    s.onMoveMade(this, move);
                }
                if (!visitor.passMove) {
                    mrX.addTicket(visitor.firstMove.ticket());
                    player(move.colour()).removeTicket(visitor.firstMove.ticket());
                    player(move.colour()).location(visitor.location);
                }
            } else {
                mrX.removeTicket(visitor.firstMove.ticket());
                mrX.location(visitor.firstMove.destination());
                mrXMoveLocation();
                if (!visitor.singleMove) {
                    mrX.removeTicket(visitor.secondMove.ticket());
                    mrX.removeTicket(Ticket.Double);
                    for (Spectator s : spectators ) {
                        s.onMoveMade(this, new DoubleMove(Black, visitor.firstMove.ticket(), mrX.getLastKnownLocation(), visitor.secondMove.ticket(), mrXMoveLocation(visitor)));
                    }
                }
                currentRound++;
                for (Spectator s : spectators ) {
                    s.onRoundStarted(this, currentRound);
                    s.onMoveMade(this, new TicketMove(Black, visitor.firstMove.ticket(), mrX.getLastKnownLocation() ));
                }
                if (!visitor.singleMove) {
                    mrX.location(visitor.location);
                    mrXMoveLocation();
                    currentRound++;
                    for (Spectator s : spectators ) {
                        s.onRoundStarted(this, currentRound);
                        s.onMoveMade(this, new TicketMove(Black, visitor.secondMove.ticket(), mrX.getLastKnownLocation()));
                    }
                }
            }
            currentPlayer++;
            if (currentPlayer != colours.size() && !isMrXCaptured()) {
                player(getCurrentPlayer()).player().makeMove(this, player(getCurrentPlayer()).location(), validMoves(getCurrentPlayer()), this);
            } else if (isGameOver()) {
                for (Spectator s : spectators) {
                    s.onGameOver(this, getWinningPlayers());
                }
            } else {
                for (Spectator s : spectators) {
                    s.onRotationComplete(this);
                }
            }
        } else throw new IllegalArgumentException("Move is not valid");
    }
}
