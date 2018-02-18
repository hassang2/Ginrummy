package com.example;

import java.util.*;
import java.util.logging.Handler;

public class GinRummy {


    public void main(String[] args){
        //keep track of over all score of each matchup here
    }

    private static final int HAND_SIZE = 10;

    private HashMap<PlayerStrategy, Integer> scores = new HashMap<>();
    private HashMap<PlayerStrategy, Set<Card>> hands = new HashMap<>();
    private Set<Card> deck = Card.getAllCards();
    private Card discard;
    private boolean inProgress = false;
    private PlayerStrategy winner = null;
    private PlayerStrategy player1;
    private PlayerStrategy player2;


    public GinRummy(PlayerStrategy p1, PlayerStrategy p2){
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
        deck = shuffle(deck);
        inProgress = true;
        dealInitialHands();
    }

    public void playGame(){

        Iterator<Card> cardIterator = deck.iterator();
        PlayerStrategy playerTurn;

        initGame();

        //handling the beginning of the game
        discard = cardIterator.next();
        if (!player1.willTakeTopDiscard(discard)){
            if(!player2.willTakeTopDiscard(discard)){
                discard = player1.drawAndDiscard(cardIterator.next());
                playerTurn = player2;
            } else{
                playerTurn = player1;
            }
        } else {
            discard = player1.drawAndDiscard(discard);
            playerTurn = player2;
        }

        //regular game
        while(cardIterator.hasNext()){
            Card nextCard = cardIterator.next();
            if (playerTurn.willTakeTopDiscard(discard)){
                Card previousDiscard = discard;
                discard = playerTurn.drawAndDiscard(previousDiscard);

                //if player attempt sto discard something they don't have
                if (!hands.get(playerTurn).remove(discard)){
                    System.out.println("player doesn't own this card");
                    System.exit(0);
                }
                playerTurn.opponentEndTurnFeedback(true, previousDiscard, discard);

            } else {
                Card previousDiscard = discard;
                discard = playerTurn.drawAndDiscard(nextCard);

                //if player attempt to discard something they don't have
                if (!hands.get(playerTurn).remove(discard)){
                    System.out.println("player doesn't own this card in hand");

                    System.exit(0);
                }
                playerTurn.opponentEndTurnFeedback(false, previousDiscard, discard);
            }

            if (playerTurn.knock()){
                if(!validMelds(hands.get(playerTurn), playerTurn)){
                    System.out.println("player doesn't own this card in melds");
                    System.exit(0);
                } else {
                    calculateScores(playerTurn);
                    break;
                }
            }
            playerTurn = playerTurn.equals(player1) ? player2 : player1;
        }

        if (scores.get(player1) >= 50){
            winner = player1;

        } else if (scores.get(player2) >= 50){
            winner = player2;
        }

    }

    /**
     * deals initial hand to each player
     */
    private void dealInitialHands(){

        //deal hand to player 1
        Iterator<Card> cardIterator = deck.iterator();
        ArrayList<Card> hand1List = new ArrayList<Card>();
        Set<Card> hand1Set = new HashSet<>();

        for (int i = 0; i < HAND_SIZE; i++) {
            hand1List.add(cardIterator.next());
            hand1Set.add(cardIterator.next());
        }
        player1.receiveInitialHand(hand1List);
        hands.put(player1, hand1Set);

        //deal hand to player 2
        ArrayList<Card> hand2List = new ArrayList<Card>();
        Set<Card> hand2Set = new HashSet<>();

        for (int i = 0; i < HAND_SIZE; i++) {
            hand2List.add(cardIterator.next());
            hand2Set.add(cardIterator.next());
        }
        player2.receiveInitialHand(hand2List);
        hands.put(player2, hand2Set);
    }



    /**
     * shuffles the deck.
     * @param cards the cards in the deck we want to shuffle
     * @return the shuffled deck
     */
    private static Set<Card> shuffle(Collection<Card> cards){
        List<Card> listOfCards = new ArrayList<>(cards);
        Collections.shuffle(listOfCards);
        return new HashSet<>(listOfCards);
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
        if (player.getMelds().size() == 0){
            return false;
        }
        for (Meld meld: player.getMelds()){
            if (meld == null || meld.getCards().length == 0){
                return false;
            }

            for (Card card: meld.getCards()){
                if (!playerHand.contains(card)){
                    return false;
                }
            }
            if (meld instanceof RunMeld && Meld.buildRunMeld(meld.getCards()) == null){
                return false;
            } else if (meld instanceof SetMeld && Meld.buildSetMeld(meld.getCards()) == null){
                return false;
            }
        }

        Set<Card> deadwoods = extractDeadwood(playerHand, player.getMelds());
        int deadwoodScore = calculateDeadwoodCount(deadwoods);
        return deadwoodScore <= 10;
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

        if (knockingPlayerDeadwoodCount == 0){
            scores.put(player, scores.get(player) + 25 + otherPlayerDeadwoodCount);
        } else {
            otherPlayerDeadwoods = layoff(player.getMelds(), otherPlayerDeadwoods);
            otherPlayerDeadwoodCount = calculateDeadwoodCount(otherPlayerDeadwoods);

            if (knockingPlayerDeadwoodCount <= otherPlayerDeadwoodCount) {
                scores.put(player, scores.get(player) + otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount);
            } else {
                scores.put(otherPlayer, scores.get(otherPlayer) + 25
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
        for (Meld meld : melds) {
            for (Card card : meld.getCards()) {
                cards.remove(card);
            }
        }
        return cards;
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
        for (Card card : deadwoods){
            for (Meld meld : melds){
                if (meld.canAppendCard(card)){
                    meld.appendCard(card);
                    deadwoods.remove(card);
                }
            }
        }
        return deadwoods;
    }

    public PlayerStrategy getWinner(){
        return winner;
    }
}
