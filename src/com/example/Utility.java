package com.example;

import java.util.*;

public class Utility {

    private static final int MIN_DEADWOOD_COUNT_TO_KNOCK = 10;

    /**
     * checks if the player actually owns the cards in their melds and validate their presented melds.
     * This also checks if the deadwood count of the player is less than or equal to 10.
     * also checks for null melds and cards
     * @param playerHand the hand of the player
     * @param player the player's meld we want to validate
     * @return true if melds are valid and false if not
     */
    public static boolean validMelds(Set<Card> playerHand, PlayerStrategy player){
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
     * extracts the deadwood cards from a given set of cards and melds
     * @param cards the cards we want to extract deadwoods from
     * @param melds the melds in the cards
     * @return the set of deadwood cards
     */
    public static Set<Card> extractDeadwood(Set<Card> cards, List<Meld> melds){
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
    public static int calculateDeadwoodCount(Collection<Card> cards){
        Iterator<Card> cardIterator = cards.iterator();

        int count = 0;
        while (cardIterator.hasNext()){
            count += cardIterator.next().getPointValue();
        }
        return count;
    }

    /**
     * shuffles the deck.
     * @param cards the cards in the deck we want to shuffle
     * @return the shuffled deck
     */
    public static ArrayList<Card> shuffle(Collection<Card> cards){
        ArrayList<Card> listOfCards = new ArrayList<>(cards);
        Collections.shuffle(listOfCards);
        return listOfCards;
    }

    /**
     * attemps to append the deadwoods in the given melds and
     * returs the new list of deadwoods
     * @param melds the melds we want to append to
     * @param deadwoods the cards we want to append
     * @return the set of deadwoods that couldn't be appended
     */
    public static Set<Card> layoff(List<Meld> melds, Set<Card> deadwoods){
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


}
