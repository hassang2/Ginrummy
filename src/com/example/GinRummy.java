package com.example;

import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

public class GinRummy {

    private static final int HAND_SIZE = 10;
    private static final int SCORE_TO_WIN = 50;
    private static final int UNDER_CUT_SCORE = 25;
    private static final int GIN_SCORE = 25;



    private HashMap<PlayerStrategy, Integer> scores = new HashMap<>();
    private HashMap<PlayerStrategy, Set<Card>> hands = new HashMap<>();
    private ArrayList<Card> deck = new ArrayList<>(Card.getAllCards());

    private Card discard;
    private PlayerStrategy winner = null;
    private PlayerStrategy player1;
    private PlayerStrategy player2;


    public GinRummy(PlayerStrategy p1, PlayerStrategy p2){
        scores.put(p1, 0);
        scores.put(p2, 0);

        if (Math.random() < .5) {
            player1 = p1;
            player2 = p2;
        } else {
            player1 = p2;
            player2 = p1;
        }
    }

    private void initGame(){
        player1.reset();
        player2.reset();
        deck = new ArrayList<>(Card.getAllCards());
        deck = Utility.shuffle(deck);
        winner = null;
        dealInitialHands();
    }

    public void playGame(){

        while(getWinner() == null) {
            PlayerStrategy playerTurn;

            initGame();

//            printCards(deck.toArray(new Card[deck.size()]));
            //handling the beginning of the game
            discard = deck.remove(0);

            if (!player1.willTakeTopDiscard(discard)) {
                if (!player2.willTakeTopDiscard(discard)) {
                    Card card = deck.remove(0);

                    discard = player1.drawAndDiscard(card);
                    hands.get(player1).add(card);
                    if (!hands.get(player1).remove(discard)) {
                        System.out.println("player doesn't own " + discard.getRank() + " " + discard.getSuit()  + " in init game1");
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
//                    System.out.println(playerTurn.getClass() + " knocked ");
                    if (!Utility.validMelds(hands.get(playerTurn), playerTurn)) {
                        System.out.println("player doesn't own this card in melds");
                        System.exit(0);
                    } else {
                        calculateScores(playerTurn);
                        break;
                    }
                }

                playerTurn = playerTurn.equals(player1) ? player2 : player1;
            }

            player1.opponentEndRoundFeedback(new ArrayList<>(hands.get(player2)), player2.getMelds());
            player2.opponentEndRoundFeedback(new ArrayList<>(hands.get(player1)), player1.getMelds());

            if (scores.get(player1) >= SCORE_TO_WIN) {
                winner = player1;

            } else if (scores.get(player2) >= SCORE_TO_WIN) {
                winner = player2;
            }
        }
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




    /**
     * calcualtes the scores that should be awarded to each player
     * @param player the player who knocked.
     */
    private void calculateScores(PlayerStrategy player){
        //knocking player
        Set<Card> knockingPlayerDeadwoods = Utility.extractDeadwood(hands.get(player), player.getMelds());
        int knockingPlayerDeadwoodCount = Utility.calculateDeadwoodCount(knockingPlayerDeadwoods);

        //other player
        PlayerStrategy otherPlayer = player.equals(player1) ? player2 : player1;
        Set<Card> otherPlayerDeadwoods = Utility.extractDeadwood(hands.get(otherPlayer), otherPlayer.getMelds());
        int otherPlayerDeadwoodCount = Utility.calculateDeadwoodCount(otherPlayerDeadwoods);

        //if it is Gin
        if (knockingPlayerDeadwoodCount == 0){
            scores.put(player, scores.get(player) + GIN_SCORE + otherPlayerDeadwoodCount);

        } else {
            otherPlayerDeadwoods = Utility.layoff(player.getMelds(), otherPlayerDeadwoods);
            otherPlayerDeadwoodCount = Utility.calculateDeadwoodCount(otherPlayerDeadwoods);

            if (knockingPlayerDeadwoodCount <= otherPlayerDeadwoodCount) {
                scores.put(player, scores.get(player) + otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount);
            } else {
                //Undercut
                scores.put(otherPlayer, scores.get(otherPlayer) + UNDER_CUT_SCORE
                         + knockingPlayerDeadwoodCount - otherPlayerDeadwoodCount);
            }
        }
    }





    public PlayerStrategy getWinner(){
        return winner;
    }

    public static void printCards(Set<Card> cards){
        for (Card card : cards) {
            System.out.print(card.getRank() + " " + card.getSuit() + " ");
        }
        System.out.print("\n");

    }
    public static void printCards(Card[] cards){
        for (Card card : cards){
            System.out.print(card.getRank() + " " + card.getSuit() + " ");
        }
        System.out.print("\n");
    }

    public static void printCards(ArrayList<Meld> melds){
        for (Meld meld : melds){
            printCards(meld.getCards());
        }
    }
}
