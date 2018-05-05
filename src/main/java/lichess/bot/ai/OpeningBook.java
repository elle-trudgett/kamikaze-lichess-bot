package lichess.bot.ai;

import chesslib.Piece;
import chesslib.Square;
import chesslib.move.Move;

import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Suicide chess opening database
 */
public class OpeningBook {
    private long nodesLoaded = 0;
    private BookNode rootNode;

    public OpeningBook() {
        System.out.println("Loading opening book");
        try (GZIPInputStream gzis = new GZIPInputStream(getClass().getResourceAsStream("book.in.gz"))) {
            rootNode = loadPNSNode(gzis);
            System.out.println("Book loaded.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<Move> findBestMove(BookNode n) {
        if (n == null) {
            return Optional.empty();
        }

        // Find best move for this state
        double bestChildRatio = 0;
        Move bestMove = null;
        for (BookNode child : n.children) {
            if (child.ratio > bestChildRatio) {
                bestChildRatio = child.ratio;
                bestMove = child.move;
            }
        }

        return Optional.ofNullable(bestMove);
    }

    public BookNode applyMove(BookNode n, Move m) {
        if (n == null) {
            return null;
        }

        for (BookNode child : n.children) {
            if (child.move.equals(m)) {
                return child;
            }
        }
        return null;
    }

    private BookNode loadPNSNode(GZIPInputStream gzis) throws IOException {
        byte[] buf = new byte[13];
        short[] numBuf = new short[13];
        int bufFilled = 0;
        while (bufFilled < 13) {
            int read = gzis.read(buf, bufFilled, 13 - bufFilled);
            if (read == -1) {
                System.out.println("Reached end of stream");
                break;
            }
            bufFilled += read;
        }

        if (bufFilled != 13) {
            System.out.println("Buffer not filled, size = " + bufFilled);
        }

        for (int i = 0; i < 13; i++) {
            numBuf[i] = (short) (buf[i] & 0xFF); // make it unsigned
        }

        BookNode node = new BookNode();
        Piece promo = Piece.NONE;
        switch(Math.abs(buf[2])) {
            case 0:
                promo = Piece.NONE;
                break;
            case 1:
                promo = Piece.WHITE_PAWN;
                System.out.println("This shouldn't happen");
                break;
            case 2:
                promo = Piece.WHITE_KNIGHT;
                break;
            case 3:
                promo = Piece.WHITE_BISHOP;
                break;
            case 4:
                promo = Piece.WHITE_ROOK;
                break;
            case 5:
                promo = Piece.WHITE_QUEEN;
                break;
            case 6:
                promo = Piece.WHITE_KING;
                break;
            default:
                System.out.println("What");
                break;
        }
        if (buf[2] < 0) {
            promo = Piece.make(promo.getPieceSide().flip(), promo.getPieceType());
        }
        node.move = new Move(pnsSquare(buf[0]), pnsSquare(buf[1]), promo);
        node.enPassantSquare = buf[3];
        node.proof = numBuf[4] + (numBuf[5] << 8) + (numBuf[6] << 16) + (numBuf[7] << 24);
        node.disproof = numBuf[8] + (numBuf[9] << 8) + (numBuf[10] << 16) + (numBuf[11] << 24);
        int numChildren = numBuf[12];
        node.children = new BookNode[numChildren];

        nodesLoaded++;

        if (nodesLoaded % 100000 == 0) {
            System.out.println("Nodes loaded: " + nodesLoaded);
        }

        for (int i = 0; i < numChildren; i++) {
            try {
                node.children[i] = loadPNSNode(gzis);
            } catch (Exception e) {
                System.out.println(node.move);
                throw e;
            }
            node.children[i].parent = node;
            node.size += node.children[i].size;
        }
        recomputeRatio(node);
        return node;
    }

    private void recomputeRatio(BookNode node) {
        if (node.children.length == 0) {
            node.ratio = 1.0 * node.proof / node.disproof;
        } else {
            node.ratio = Double.POSITIVE_INFINITY;
            for (BookNode child : node.children) {
                if (1 / child.ratio < node.ratio) {
                    node.ratio = 1 / child.ratio;
                }
            }
        }
    }

    private Square pnsSquare(byte value) {
        int rankFlippedOrdinal = 8 * (7 - (value / 8)) + (value % 8);
        if (rankFlippedOrdinal < 0) {
            System.out.println("What");
        }
        return Square.squareAt(rankFlippedOrdinal);
    }

    public BookNode getRootNode() {
        return rootNode;
    }

    public static class BookNode {
        public Move move;
        public byte enPassantSquare;
        public int proof;
        public int disproof;
        public double ratio;
        public BookNode parent;
        public long size = 1;
        public BookNode[] children;
    }
}
