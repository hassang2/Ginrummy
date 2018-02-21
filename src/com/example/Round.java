package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Round {

    private static final int HAND_SIZE = 10;
    private static final int UNDER_CUT_SCORE = 25;
    private static final int GIN_SCORE = 25;

    private Card discard;
    private PlayerStrategy playerTurn;
    private HashMap<PlayerStrategy, Set<Card>> hands = new HashMap<>();
    private ArrayList<Card> deck = new ArrayList<>(Card.getAllCards());
    private PlayerStrategy player1;
    private PlayerStrategy player2;
    private int endScore = 0;
    private PlayerStrategy winner = null;

    public Round(PlayerStrategy p2, PlayerStrategy p1){
        if (Math.random() < .5) {
            player1 = p1;
            player2 = p2;
        } else {
            player1 = p2;
            player2 = p1;
        }
    }

    public void initRound(){
        player1.reset();
        player2.reset();
        deck = new ArrayList<>(Card.getAllCards());
        hands = new HashMap<>();
        playerTurn = null;
        discard = null;
        deck = Utility.shuffle(deck);
        dealInitialHands();
    }

    public void playRound() {
        //handling the beginning of the game
        discard = deck.remove(0);
        if (!player1.willTakeTopDiscard(discard)) {
            if (!player2.willTakeTopDiscard(discard)) {
                Card card = deck.remove(0);

                discard = player1.drawAndDiscard(card);
                hands.get(player1).add(card);
                if (!hands.get(player1).remove(discard)) {
                    System.out.println("player doesn't own " + discard.getRank() + " " + discard.getSuit() + " in init game1");
                    System.exit(0);
                }

                playerTurn = player2;
            } else {

                hands.get(player2).add(discard);
                discard = player2.drawAndDiscard(discard);
                if (!hands.get(player2).remove(discard)) {

                    System.out.println("player2 doesn't own this in init game2");
                    System.exit(0);
                }

                playerTurn = player1;
            }
        } else {
            hands.get(player1).add(discard);
            discard = player1.drawAndDiscard(discard);

            if (!hands.get(player1).remove(discard)) {
                System.out.println("player1 doesn't own this in init game3");
                System.exit(0);
            }
            playerTurn = player2;
        }

        //regular game
        while (deck.size() > 0) {

            if (playerTurn.willTakeTopDiscard(discard)) {
                Card previousDiscard = discard;
                discard = playerTurn.drawAndDiscard(previousDiscard);
                hands.get(playerTurn).add(previousDiscard);


                //if player attempts to discard something they don't have
                if (!hands.get(playerTurn).remove(discard)) {
                    System.out.println("player doesn't own this card");
                    System.exit(0);
                }
                playerTurn.opponentEndTurnFeedback(true, previousDiscard, discard);

            } else {
                Card nextCard = deck.remove(0);
                Card previousDiscard = discard;
                discard = playerTurn.drawAndDiscard(nextCard);

                hands.get(playerTurn).add(nextCard);

                //if player attempt to discard something they don't have
                if (!hands.get(playerTurn).remove(discard)) {
                    System.out.println("player doesn't own " + discard.getRank() + " " + discard.getSuit());
                    System.exit(0);
                }
                playerTurn.opponentEndTurnFeedback(false, previousDiscard, discard);
            }

            if (playerTurn.knock()) {
                if (!Utility.validMelds(hands.get(playerTurn), playerTurn)) {
                    System.out.println("player doesn't own this card in melds");
                    System.exit(0);
                } else {
                    endScore = calculateScore(playerTurn);
                    break;
                }
            }

            playerTurn = playerTurn.equals(player1) ? player2 : player1;
        }


        player1.opponentEndRoundFeedback(new ArrayList<>(hands.get(player2)), player2.getMelds());
        player2.opponentEndRoundFeedback(new ArrayList<>(hands.get(player1)), player1.getMelds());
    }

    /**
     * calculates the scores that should be awarded to each player
     * @param knockingPlayer the player who knocked.
     */
    private int calculateScore(PlayerStrategy knockingPlayer){
        //knocking player
        Set<Card> knockingPlayerDeadwoods = Utility.extractDeadwood(hands.get(knockingPlayer), knockingPlayer.getMelds());
        int knockingPlayerDeadwoodCount = Utility.calculateDeadwoodCount(knockingPlayerDeadwoods);

        //other player
        PlayerStrategy otherPlayer = knockingPlayer.equals(player1) ? player2 : player1;
        Set<Card> otherPlayerDeadwoods = Utility.extractDeadwood(hands.get(otherPlayer), otherPlayer.getMelds());
        int otherPlayerDeadwoodCount = Utility.calculateDeadwoodCount(otherPlayerDeadwoods);

        //if it is Gin
        if (knockingPlayerDeadwoodCount == 0){
            winner = knockingPlayer;
            return GIN_SCORE + otherPlayerDeadwoodCount;
        } else {
            otherPlayerDeadwoods = Utility.layoff(knockingPlayer.getMelds(), otherPlayerDeadwoods);
            otherPlayerDeadwoodCount = Utility.calculateDeadwoodCount(otherPlayerDeadwoods);

            if (knockingPlayerDeadwoodCount <= otherPlayerDeadwoodCount) {
                winner = knockingPlayer;
                return otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount;
            } else {
                //Undercut
                winner = otherPlayer;
                return UNDER_CUT_SCORE + knockingPlayerDeadwoodCount - otherPlayerDeadwoodCount;
            }
        }
    }

    /**
     * @return the score that should awarded to the winner
     */
    public int getRoundScore(){
        return endScore;
    }

    public PlayerStrategy getWinner() {
        return winner;
    }

    /**
     * deals initial hand to each player
     */
    private void dealInitialHands(){

        //deal hand to player 1
        ArrayList<Card> hand1List = new ArrayList<>();
        Set<Card> hand1Set = new HashSet<>();

        for (int i = 0; i < HAND_SIZE; i++) {
            Card card = deck.remove(0);
            hand1List.add(card);
            hand1Set.add(card);
        }
        player1.receiveInitialHand(hand1List);

        hands.put(player1, hand1Set);

        //deal hand to player 2
        ArrayList<Card> hand2List = new ArrayList<>();
        Set<Card> hand2Set = new HashSet<>();

        for (int i = 0; i < HAND_SIZE; i++) {
            Card card = deck.remove(0);
            hand2List.add(card);
            hand2Set.add(card);
        }

        player2.receiveInitialHand(hand2List);
        hands.put(player2, hand2Set);

    }
}
