package lichess.bot.ai;

import chesslib.Board;
import chesslib.Piece;
import chesslib.PieceType;
import chesslib.Side;
import chesslib.move.Move;
import chesslib.move.MoveGenerator;
import chesslib.move.MoveList;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements https://en.wikipedia.org/wiki/Monte_Carlo_tree_search#Principle_of_operation.
 *
 * Build a tree of possible moves and assign scores to states based on playouts. Expands leaf nodes with a best-first
 * strategy, which means it spends more time evaluating moves that look good (for both sides.)
 */
public class MonteCarloTreeSearch {
    private static final Random RANDOM = new SecureRandom();
    private static final double EXPLORATION_CONSTANT = Math.sqrt(2.0);
    private static final double THREAT_CONSTANT = 1.0;
    private static final int MAX_MOVE_DEPTH = 100;

    private Node root;
    private Map<Board, Move> forcedSequences = new HashMap<>();

    public MonteCarloTreeSearch(Board initialState) {
        root = new Node(initialState.clone(), null);
    }

    private boolean search() {
        // Selection: start from root R and descend down the tree to a leaf node L.
        Node leaf = findExpandableLeafNode(root);

        Node finalNode;
        Side winner;

        if (leaf == null) {
            System.out.println("No more non-terminal leaf nodes to expand");
            return true;
        }

        // Expansion: unless L ends the game with a win/loss for either player,
        // create one (or more) child nodes and choose node C from one of them.
        createChildNodes(leaf);
        if (leaf.children.isEmpty()) {
            System.out.println("No children of this board. Done");
            return false;
        }
        Node candidate = (Node) leaf.children.values().toArray()[RANDOM.nextInt(leaf.children.size())];

        // Simulation: play a random playout from node C.
        winner = playout(candidate.board);
        //System.out.println("Playout from child state of leaf resulted in a win for " + winner);
        finalNode = candidate;

        // Backpropagation: use the result of the playout to update information in the nodes on the path from C to R.
        boolean forced = finalNode.terminalState;
        Map<Board, Move> potentialForcedSequence = new HashMap<>();

        Node n = finalNode;
        Side mySide = root.board.getSideToMove();
        while (n != null) {
            if (n.children.size() > 1 && n.board.getSideToMove() != mySide) {
                forced = false;
            }
            if (forced && n != root) {
                potentialForcedSequence.put(n.parent.board, n.movePlayedToGetToThisState);
            }
            n.simulationCount++;
            if (winner != null) {
                if (winner == n.board.getSideToMove()) {
                    n.wins += 1; // Positive score for winning
                } else {
                    n.wins -= 1; // Negative score for losing
                }
            } else {
                // Zero score for draws
            }

            // Ensure tree integrity.
            assert n.parent != null || n.equals(root);

            n = n.parent;
        }

        if (forced && winner == mySide) {
            for (Board board : potentialForcedSequence.keySet()) {
                forcedSequences.put(board, potentialForcedSequence.get(board));
            }
        }

        return false;
    }

    /**
     * Play randomly to the end of the game from state and return the winner (or null if draw.)
     */
    private Side playout(Board fromState) {
        int movesTested = 0;
        Board state = fromState.clone();
        while (!isWinningState(state)) {
            makeRandomMove(state);
            if (gameIsDraw(state)) {
                return null;
            }
            movesTested++;
            if (movesTested > MAX_MOVE_DEPTH) {
                return null; // Assume draw if it goes this long.
            }
        }

        return state.getSideToMove();
    }

    private void makeRandomMove(Board state) {
        List<Move> allPossibleMoves = getAllPossibleMoves(state);
        state.doMove(allPossibleMoves.get(RANDOM.nextInt(allPossibleMoves.size())));
    }

    static boolean gameIsDraw(Board state) {
        Board flipState = state.clone();
        flipState.setSideToMove(state.getSideToMove().flip());

        List<Move> allPossibleMoves = getAllPossibleMoves(state);
        if (allPossibleMoves.size() == 0) {
            return false; // Someone can't move, it's game over
        }

        for (Move possibleMove : allPossibleMoves) {
            Piece pieceToMove = state.getPiece(possibleMove.getFrom());
            if (pieceToMove.getPieceType() != PieceType.BISHOP) {
                return false; // As long as you can move a non-bishop piece, it's not a draw (yet)
            }
        }

        // Now simulate the position for the other player
        List<Move> allPossibleFlippedMoves = getAllPossibleMoves(flipState);
        for (Move possibleMove : allPossibleFlippedMoves) {
            Piece pieceToMove = state.getPiece(possibleMove.getFrom());
            if (pieceToMove.getPieceType() != PieceType.BISHOP) {
                return false; // As long as you can move a non-bishop piece, it's not a draw (yet)
            }
        }

        // Now we know we only have bishop moves for both sides. It's only a draw if there's exactly 1 bishop on each side
        if (state.getPieceLocation(Piece.WHITE_BISHOP).size() != 1) {
            return false;
        }
        if (state.getPieceLocation(Piece.BLACK_BISHOP).size() != 1) {
            return false;
        }

        // And they're on different colored squares.
        return state.getPieceLocation(Piece.WHITE_BISHOP).get(0).isLightSquare() != state.getPieceLocation(Piece.BLACK_BISHOP).get(0).isLightSquare();
    }

    private void createChildNodes(Node n) {
        List<Move> possibleMoves = getAllPossibleMoves(n.board);
        for (Move move : possibleMoves) {
            Board newState = n.board.clone();
            newState.doMove(move);
            Node newNode = new Node(newState, n);
            if (gameIsDraw(newState) || isWinningState(newState)) {
                newNode.terminalState = true;
                newNode.simulationCount = 1;
                newNode.wins = 1;
            }
            newNode.movePlayedToGetToThisState = move;
            newNode.numberOfThreats = getNumberOfThreats(newState);
            n.children.put(move, newNode);
        }

        for (Node c : n.children.values()) {
            assert c.board.getSideToMove() == n.board.getSideToMove().flip();
        }
    }

    static int getNumberOfThreats(Board board) {
        int attacks = 0;
        for (Move move : getAllPossibleMoves(board)) {
            if (move.isCapture(board)) {
                attacks++;
            }
        }
        return attacks;
    }

    private boolean isWinningState(Board board) {
        return getAllPossibleMoves(board).isEmpty();
    }

    private Node findExpandableLeafNode(Node n) {
        if (n.children.isEmpty()) {
            return n;
        } else {
            // Using https://en.wikipedia.org/wiki/Monte_Carlo_tree_search#Exploration_and_exploitation
            // With help on the algorithm from http://teytaud.over-blog.com/article-35709049.html
            List<ChildOptionWithUCTValue> childOptions = n.children.values().stream().map(node -> new ChildOptionWithUCTValue(node, 0.0)).collect(Collectors.toList());

            // Do not consider terminal states for expansion.
            childOptions = childOptions.stream().filter(x -> !x.childState.terminalState).collect(Collectors.toList());

            if (childOptions.isEmpty()) {
                return null; // This node has no expandable children nodes.
            }

            // Calculate UCT values for all child options.
            for (ChildOptionWithUCTValue childOptionWithUCTValue : childOptions) {
                Node childNode = childOptionWithUCTValue.childState;

                double exploitationComponent = (childNode.wins + childNode.simulationCount) / (double) (childNode.simulationCount * 2);
                double explorationComponent = Math.sqrt(Math.log(n.simulationCount) / childNode.simulationCount);
                double threatComponent = 1 - (1 / (double)(childNode.numberOfThreats + 1));
                double uctValue = exploitationComponent + EXPLORATION_CONSTANT * explorationComponent + THREAT_CONSTANT * threatComponent;

                // https://www.youtube.com/watch?v=UXW2yZndl7U
                // Mathematically, division by zero is infinite, which will be prioritized over any other value.
                // This has the effect of always evaluating unvisited children before visited children.
                if (Double.isNaN(uctValue)) {
                    uctValue = Double.POSITIVE_INFINITY;
                }

                childOptionWithUCTValue.uct = uctValue;
            }

            // Shuffle to randomize things of equal UCT value, then sort descending to go for most promising first
            Collections.shuffle(childOptions);
            childOptions.sort(Comparator.comparingDouble(x -> x.uct));
            Collections.reverse(childOptions);

            for (ChildOptionWithUCTValue childOption : childOptions) {
                Node leafNodeFromChild = findExpandableLeafNode(childOption.childState);
                if (leafNodeFromChild != null) {
                    return leafNodeFromChild;
                }
            }

            return null; // This node has no expandable leaf nodes.
        }
    }

    public boolean isGameGoingToEndSoon() {
        if (forcedSequences.size() > 0) {
            return true;
        }

        if (root.terminalState || root.children.values().stream().allMatch(n -> n.terminalState)) {
            return true;
        }

        return false;
    }

    private static class ChildOptionWithUCTValue {
        public Node childState;
        public double uct;

        public ChildOptionWithUCTValue(Node childState, double uct) {
            this.childState = childState;
            this.uct = uct;
        }
    }

    /*
     * This could probably be improved with an LRU cache
     */
    private static List<Move> getAllPossibleMoves(Board state) {
        MoveList pseudoLegalMoves = MoveGenerator.generatePseudoLegalMoves(state);
        List<Move> captureMoves = new ArrayList<>();
        List<Move> nonCaptureMoves = new ArrayList<>();

        for (Move pseudoLegalMove : pseudoLegalMoves) {
            if (pseudoLegalMove.isCapture(state)) {
                captureMoves.add(pseudoLegalMove);
            } else {
                nonCaptureMoves.add(pseudoLegalMove);
            }
        }

        if (!captureMoves.isEmpty()) {
            return captureMoves;
        }

        return nonCaptureMoves;
    }

    public void applyMove(Move moveMade) {
        if (root.children.containsKey(moveMade)) {
            root = root.children.get(moveMade);
            root.parent = null;
        } else {
            root.board.doMove(moveMade);
        }
    }

    public Move findBestMove(long searchLimit) {
        return findBestMove(Duration.ofDays(1), searchLimit);
    }

    public Move findBestMove(Duration timeLimit) {
        return findBestMove(timeLimit, Long.MAX_VALUE);
    }

    public Move findBestMove(Duration timeLimit, long searchLimit) {
        if (forcedSequences.containsKey(root.board)) {
            Move move = forcedSequences.get(root.board);
            System.out.println("Using forced sequence move " + move);
            return move;
        }

        System.out.println("Starting to find best move");
        printTree();
        search(); // Propagates child nodes if they don't exist yet at this new root node.

        if (root.children.size() == 1) {
            System.out.println("Only 1 move available, playing it.");
            return root.children.keySet().iterator().next();
        }

        Instant start = Instant.now();
        Instant end = start.plus(timeLimit);

        int searchesDone = 0;
        while (Instant.now().isBefore(end)) {
            boolean finished = search();
            searchesDone++;
            if (searchesDone >= searchLimit || finished) {
                break;
            }
            if (forcedSequences.size() > 0) {
                // We found a way to a guaranteed win
                System.out.println("Forced sequences found: " + forcedSequences.entrySet().stream().map(entry -> entry.getKey().getFen() + " :: " + entry.getValue()).collect(Collectors.toList()));
                return findBestMove(1);
            }
        }

        System.out.println(searchesDone + " searches done in " + (Instant.now().toEpochMilli() - start.toEpochMilli() + "ms"));

        System.out.println("After searching, tree looks like this:");
        printTree();

        List<Move> bestMoves = new ArrayList<>();
        double bestMoveExpectedWinrate = 0;
        boolean found = false;
        for (Map.Entry<Move, Node> child : root.children.entrySet()) {
            Node childNode = child.getValue();
            double childExpectedWinrate = (-childNode.wins + childNode.simulationCount) / (double) (childNode.simulationCount * 2);
            if (!found || childExpectedWinrate >= bestMoveExpectedWinrate) {
                if (childExpectedWinrate > bestMoveExpectedWinrate) {
                    bestMoves.clear(); // Found a new best
                }
                bestMoves.add(child.getKey());
                bestMoveExpectedWinrate = childExpectedWinrate;
                found = true;
            }
        }

        if (bestMoves.isEmpty()) {
            return null;
        } else {
            System.out.println("There are " + bestMoves.size() + " strongest moves with my expected winrate being " + String.format("%.1f", bestMoveExpectedWinrate * 100) + "%");
            System.out.println(bestMoves);
            return bestMoves.get(RANDOM.nextInt(bestMoves.size()));
        }
    }

    private void printTree() {
        System.out.println("Root to move: " + root.board.getSideToMove() + " Score: " + root.wins + ", SimCount: " + root.simulationCount);
        System.out.println("Children: " + root.children.size());
        for (Map.Entry<Move, Node> moveNodeEntry : root.children.entrySet()) {
            Node node = moveNodeEntry.getValue();
            System.out.println("* [play " + root.board.getPiece(moveNodeEntry.getKey().getFrom()) + " - " + moveNodeEntry.getKey() + "] then " + node.board.getSideToMove() + " will have " + String.format("%.1f", (100 * (node.wins + node.simulationCount)) / (double) (node.simulationCount * 2)) + "% chance of winning (" + node.simulationCount + " simulations)");
        }
    }

    public double evaluation() {
        return (root.wins + root.simulationCount) / (double)(root.simulationCount * 2);
    }

    private class Node {
        public final Board board;
        public long wins = 0;
        public long simulationCount = 0;
        public boolean terminalState = false;
        public Move movePlayedToGetToThisState = null;
        public Node parent = null;
        public Map<Move, Node> children = new HashMap<>();
        public int numberOfThreats;

        public Node(Board boardState, Node parent) {
            this.board = boardState;
            this.parent = parent;
        }
    }
}
