package com.example;

import java.util.*;
import java.util.List;

public class GinRummy {


    public static void main(String[] args){
        GinRummy game;
        HashMap<PlayerStrategy, Integer> gameWins = new HashMap<>();
        PlayerStrategy player1 = new StrategyA();
        PlayerStrategy player2 = new StrategyB();

        gameWins.put(player1, 0);
        gameWins.put(player2, 0);

        for (int i = 0; i < 3; i++) {
            System.out.println("Game " + (i+1));
            game = new GinRummy(player1, player2);
            game.playGame();
            gameWins.put(game.getWinner(), gameWins.get(game.getWinner()) + 1);
        }

        System.out.println("player1 won " + gameWins.get(player1) + " games");
        System.out.println("player2 won " + gameWins.get(player2) + " games");

    }

    private static final int HAND_SIZE = 10;
    private static final int SCORE_TO_WIN = 50;
    private static final int UNDER_CUT_SCORE = 25;
    private static final int GIN_SCORE = 25;
    private static final int MIN_DEADWOOD_COUNT_TO_KNOCK = 10;



    private HashMap<PlayerStrategy, Integer> scores = new HashMap<>();
    private HashMap<PlayerStrategy, Set<Card>> hands = new HashMap<>();
    private ArrayList<Card> deck = new ArrayList<>(Card.getAllCards());
    Iterator<Card> deckIterator = deck.iterator();

    private Card discard;
    private boolean inProgress = false;
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
        deck = Card.getAllCards();
        deckIterator = deck.iterator();
        deck = shuffle(deck);
        inProgress = true;
        winner = null;
        dealInitialHands();
    }

    public void playGame(){

        while(getWinner() == null) {
            PlayerStrategy playerTurn;

            initGame();

            //handling the beginning of the game
            discard = deckIterator.next();
            deckIterator.remove();

            if (!player1.willTakeTopDiscard(discard)) {
                if (!player2.willTakeTopDiscard(discard)) {
                    Card card = deckIterator.next();
                    deckIterator.remove();

                    discard = player1.drawAndDiscard(card);
                    hands.get(player1).add(card);
                    if (!hands.get(player1).remove(discard)) {
                        System.out.println("player doesn't own this in init game");
                    }

                    playerTurn = player2;
                } else {

                    discard = player2.drawAndDiscard(discard);
                    hands.get(player2).add(discard);
                    if (!hands.get(player2).remove(discard)) {
                        System.out.println("player doesn't own this in init game");
                    }

                    playerTurn = player1;
                }
            } else {
                discard = player1.drawAndDiscard(discard);

                hands.get(player1).add(discard);
                if (!hands.get(player1).remove(discard)) {
                    System.out.println("player doesn't own this in init game");
                }
                playerTurn = player2;
            }

            //regular game
            while (deckIterator.hasNext()) {

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
                    Card nextCard = deckIterator.next();
                    deckIterator.remove();
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
                    if (!validMelds(hands.get(playerTurn), playerTurn)) {
                        System.out.println("player doesn't own this card in melds");
                        System.exit(0);
                    } else {
                        calculateScores(playerTurn);
                        break;
                    }
                }
                playerTurn = playerTurn.equals(player1) ? player2 : player1;
            }

            for (PlayerStrategy p : scores.keySet()){
                System.out.println(scores.get(p));
            }
            System.out.println();

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
            Card card = deckIterator.next();
            deckIterator.remove();
            hand1List.add(card);
            hand1Set.add(card);
        }
        player1.receiveInitialHand(hand1List);

        hands.put(player1, hand1Set);

        //deal hand to player 2
        ArrayList<Card> hand2List = new ArrayList<>();
        Set<Card> hand2Set = new HashSet<>();

        for (int i = 0; i < HAND_SIZE; i++) {
            Card card = deckIterator.next();
            deckIterator.remove();
            hand2List.add(card);
            hand2Set.add(card);
        }

        player2.receiveInitialHand(hand2List);
        hands.put(player2, hand2Set);

    }



    /**
     * shuffles the deck.
     * @param cards the cards in the deck we want to shuffle
     * @return the shuffled deck
     */
    private static ArrayList<Card> shuffle(Collection<Card> cards){
        ArrayList<Card> listOfCards = new ArrayList<>(cards);
        Collections.shuffle(listOfCards);
        return listOfCards;
    }

    /**
     * checks if the player actually owns the cards in their melds and validate their presented melds.
     * This also checks if the deadwood count of the player is less than or equal to 10.
     * also checks for null melds and cards
     * @param playerHand the hand of the player
     * @param player the player's meld we want to validate
     * @return true if melds are valid and false if not
     */
    private static boolean validMelds(Set<Card> playerHand, PlayerStrategy player){
        System.out.println(player.getClass());
        if (player.getMelds() == null || player.getMelds().size() == 0){
            return true;
        }
        for (Meld meld: player.getMelds()){
            if (meld == null || meld.getCards().length == 0){
                System.out.println("0 length meld");
                return false;
            }

            for (Card card: meld.getCards()){
                if (!playerHand.contains(card)){
                    System.out.println("doesn't own this");
                    return false;
                }
            }
            if (meld instanceof RunMeld && Meld.buildRunMeld(meld.getCards()) == null){
                System.out.println("not a run");

                return false;
            } else if (meld instanceof SetMeld && Meld.buildSetMeld(meld.getCards()) == null){
                System.out.println("not a set");

                return false;
            }
        }

        Set<Card> deadwoods = extractDeadwood(playerHand, player.getMelds());
        int deadwoodScore = calculateDeadwoodCount(deadwoods);
        return deadwoodScore <= MIN_DEADWOOD_COUNT_TO_KNOCK;
    }

    /**
     * calcualtes the scores that should be awarded to each player
     * @param player the player who knocked.
     */
    private void calculateScores(PlayerStrategy player){
        PlayerStrategy otherPlayer = player.equals(player1) ? player2 : player1;
        Set<Card> knockingPlayerDeadwoods = extractDeadwood(hands.get(player), player.getMelds());
        int knockingPlayerDeadwoodCount = calculateDeadwoodCount(knockingPlayerDeadwoods);

        Set<Card> otherPlayerDeadwoods = extractDeadwood(hands.get(otherPlayer), otherPlayer.getMelds());
        int otherPlayerDeadwoodCount = calculateDeadwoodCount(otherPlayerDeadwoods);

        //if it is Gin
        if (knockingPlayerDeadwoodCount == 0){
            scores.put(player, scores.get(player) + GIN_SCORE + otherPlayerDeadwoodCount);

        } else {
            otherPlayerDeadwoods = layoff(player.getMelds(), otherPlayerDeadwoods);
            otherPlayerDeadwoodCount = calculateDeadwoodCount(otherPlayerDeadwoods);

            if (knockingPlayerDeadwoodCount <= otherPlayerDeadwoodCount) {
                scores.put(player, scores.get(player) + otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount);
            } else {
                //Undercut
                scores.put(otherPlayer, scores.get(otherPlayer) + UNDER_CUT_SCORE
                         + knockingPlayerDeadwoodCount - otherPlayerDeadwoodCount);
            }
        }
    }

    /**
     * extracts the deadwood cards from a given set of cards and melds
     * @param cards the cards we want to extract deadwoods from
     * @param melds the melds in the cards
     * @return the set of deadwood cards
     */
    private static Set<Card> extractDeadwood(Set<Card> cards, List<Meld> melds){
        Set<Card> cardCopy = new HashSet<>(cards);
        if (melds == null || melds.size() == 0){
            return cards;
        }

        for (Meld meld : melds) {
            for (Card card : meld.getCards()) {
                cardCopy.remove(card);
            }
        }
        return cardCopy;
    }

    /**
     * calculates the deadwood count from the given deadwood cards
     * @param cards the deadwood cards
     * @return the total deadwood count of the cards
     */
    private static int calculateDeadwoodCount(Collection<Card> cards){
        Iterator<Card> cardIterator = cards.iterator();
        
        int count = 0;
        while (cardIterator.hasNext()){
            count += cardIterator.next().getPointValue();
        }
        return count;
    }

    /**
     * attemps to append the deadwoods in the given melds and
     * returs the new list of deadwoods
     * @param melds the melds we want to append to
     * @param deadwoods the cards we want to append
     * @return the set of deadwoods that couldn't be appended
     */
    private static Set<Card> layoff(List<Meld> melds, Set<Card> deadwoods){
        if (melds == null || melds.size() == 0){
            return deadwoods;
        }

        Set<Card> deadwoodsCopy = new HashSet<>(deadwoods);
        List<Meld> meldsCopy = new ArrayList<>(melds);

        for (Card card : deadwoods){
            for (Meld meld : meldsCopy){
                if (meld.canAppendCard(card)){
                    meld.appendCard(card);
                    deadwoodsCopy.remove(card);
                }
            }
        }
        return deadwoodsCopy;
    }

    public PlayerStrategy getWinner(){
        return winner;
    }

    public static void printCards(Set<Card> cards){
        Iterator<Card> it = cards.iterator();
        while(it.hasNext()){
            Card card = it.next();
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
