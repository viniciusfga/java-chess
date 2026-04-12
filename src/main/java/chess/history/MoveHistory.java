package chess.history;

import java.util.Stack;

public class MoveHistory {

    private Stack<Move> undoStack = new Stack<>();
    private Stack<Move> redoStack = new Stack<>();

    public void push(Move move) {
        undoStack.push(move);
        redoStack.clear();
    }

    public Move undo() {
        if (undoStack.isEmpty()) return null;
        Move m = undoStack.pop();
        redoStack.push(m);
        return m;
    }

    public Move redo() {
        if (redoStack.isEmpty()) return null;
        Move m = redoStack.pop();
        undoStack.push(m);
        return m;
    }
}