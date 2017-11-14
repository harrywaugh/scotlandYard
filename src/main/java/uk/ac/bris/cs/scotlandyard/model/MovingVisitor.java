package uk.ac.bris.cs.scotlandyard.model;

public class MovingVisitor implements MoveVisitor {
    public int location;
    public boolean passMove, singleMove;
    public TicketMove firstMove, secondMove;

    public void visit(PassMove move) {
        passMove = true;
    }

    public void visit(TicketMove move) {
        firstMove = move;
        location = move.destination();
        singleMove = true;
        passMove =  false;

    }

    public void visit(DoubleMove move) {
        singleMove = false;
        passMove =  false;
        location = move.finalDestination();
        firstMove = move.firstMove();
        secondMove = move.secondMove();
    }
}
