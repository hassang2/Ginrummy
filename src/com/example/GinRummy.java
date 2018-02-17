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

    public void playGames(int numberOfGames){

        Iterator<Card> cardIterator = deck.iterator();
        PlayerStrategy playerTurn;
        deck = Card.getAllCards();
        deck = shuffle(deck);
        inProgress = true;
        dealInitialHands();

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

                //if player attempt sto discard something they don't have
                if (!hands.get(playerTurn).remove(discard)){
                    System.exit(0);
                }
                playerTurn.opponentEndTurnFeedback(false, previousDiscard, discard);
            }

            if (playerTurn.knock()){
                if(!validMelds(hands.get(playerTurn), playerTurn)){
                    System.out.println("player doesn't own this card");
                    System.exit(0);
                } else {
                    calculateScores(playerTurn);
                }
            }
            playerTurn = playerTurn.equals(player1) ? player2 : player1;
        }

        TIE
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
        if (player.getMelds() == null){
            return false;
        }
        for (Meld meld: player.getMelds()){
            if (meld == null){
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
        return true;
    }

    /**
     * calcualtes the scores that should be awarded to each player
     * @param player the player who knocked.
     */
    private void calculateScores(PlayerStrategy player){
        PlayerStrategy otherPlayer = player.equals(player1) ? player2 : player1;
        Set<Card> knockingPlayerDeadwoods = extractDeadwood(hands.get(player), player.getMelds());
        Set<Card> otherPlayerDeadwoods = extractDeadwood(hands.get(otherPlayer), otherPlayer.getMelds());
        int knockingPlayerDeadwoodCount = calculateDeadwoodCount(knockingPlayerDeadwoods);
        int otherPlayerDeadwoodCount = calculateDeadwoodCount(otherPlayerDeadwoods);

        if (knockingPlayerDeadwoodCount <= otherPlayerDeadwoodCount){
            scores.put(player, scores.get(player) + otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount);
        } else {
            scores.put(player, scores.get(player) + otherPlayerDeadwoodCount - knockingPlayerDeadwoodCount);

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


    private static int calculateDeadwoodCount(Collection<Card> cards){
        Iterator<Card> cardIterator = cards.iterator();
        
        int count = 0;
        while (cardIterator.hasNext()){
            count += cardIterator.next().getPointValue();
        }
        return count;
    }

    public static Set<Card> getDeadwoodCards(Set<Card> cards){

    }

    public PlayerStrategy getWinner(){

    }
}
