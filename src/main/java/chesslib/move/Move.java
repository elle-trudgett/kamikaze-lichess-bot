/*
 * Copyright 2017 Ben-Hur Carlos Vieira Langoni Junior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chesslib.move;

import chesslib.*;

public class Move implements BoardEvent {

    private final Square from;
    private final Square to;
    private final Piece promotion;
    private String san;

    public Move(Square from, Square to) {
        this(from, to, Piece.NONE);
    }

    public Move(Square from, Square to, Piece promotion) {
        this.promotion = promotion;
        this.from = from;
        this.to = to;
    }

    public Move(String move, Side side) {
        this(Square.valueOf(move.substring(0, 2).toUpperCase()),
                Square.valueOf(move.substring(2, 4).toUpperCase()),
                move.length() < 5 ? Piece.NONE : Side.WHITE.equals(side) ?
                        Constants.getPieceByNotation(
                                move.substring(4, 5).toUpperCase()) :
                        Constants.getPieceByNotation(
                                move.substring(4, 5).toLowerCase()));
    }

    public Square getFrom() {
        return from;
    }

    public Square getTo() {
        return to;
    }

    public Piece getPromotion() {
        return promotion;
    }

    public boolean isCapture(Board b) {
        if (b.getPiece(from).getPieceType() == PieceType.PAWN && from.getFile() != to.getFile()) {
            return true; // en passant
        }

        return b.getPiece(from) != Piece.NONE && b.getPiece(to) != Piece.NONE &&
                b.getPiece(from).getPieceSide() == b.getPiece(to).getPieceSide().flip();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Move)) {
            return false;
        }
        Move move = (Move) obj;
        return move.getFrom().equals(getFrom()) &&
                move.getTo().equals(getTo()) &&
                move.getPromotion().equals(getPromotion());

    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        String promo = "";
        if (!Piece.NONE.equals(promotion)) {
            promo = Constants.getPieceNotation(promotion);
        }
        return from.toString().toLowerCase() +
                to.toString().toLowerCase() +
                promo.toLowerCase();
    }

    public BoardEventType getType() {
        return BoardEventType.ON_MOVE;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

}
