package com.example;
import java.util.*;

/**
 * Knocks as fast as possible
 */
public class StrategyA implements PlayerStrategy {

    private List<Meld> melds = new ArrayList<>();
    private Set<Card> hand = new HashSet<>();
    private Set<Card> opponentHand = new HashSet<>();
    private Set<Card> deadwood = new HashSet<>();
    private Set<Card> discards = new HashSet<>();
    private int deadwoodCount = 0;

    private static final int DEADWOOD_COUNT_TO_KNOCK = 10;
    private static final int SCORE_FOR_MELD = 7;
    private static final int SCORE_PENALTY_FOR_LARGE_MELD = 4;
    private static final int SCORE_FOR_POSSIBLE_RUN = 4;
    private static final int SCORE_FOR_POSSIBLE_SET = 4;
    private static final int SCORE_FOR_OPPONENT_CARD = 2;
    private static final int LARGE_MELD_THRESHOLD = 3;
    private static final int SCORE_PENALTY_FOR_IN_DISCARD = 2;

    @Override
    public void receiveInitialHand(List<Card> cards) {
        hand = new HashSet<>(cards);
        updateHand();
    }

    @Override
    public boolean willTakeTopDiscard(Card card) {
        Set<Card> tempHand = new HashSet<>(hand);
        tempHand.add(card);
        int newDeadwoodCount = calculateTotalValue(hand) - calculateTotalValue(findBestMeldSet(tempHand));

        return newDeadwoodCount < deadwoodCount;
    }

    @Override
    public Card drawAndDiscard(Card drawnCard) {
        hand.add(drawnCard);

        updateHand();

        //if we have no deadwood after picking up the new card
        if (deadwood.size() == 0) {

            Iterator<Card> cardIterator = hand.iterator();
            Card lowestValueCard = cardIterator.next();
            while(cardIterator.hasNext()){
                Card nextCard = cardIterator.next();
                if (nextCard.getPointValue() < lowestValueCard.getPointValue()){
                    lowestValueCard = nextCard;
                }
            }
            hand.remove(lowestValueCard);
            updateHand();
            return lowestValueCard;
        }

        HashMap<Card, Integer> cardScores = computeCardScores();

        Card worstCard = findWorstCard(cardScores);

        hand.remove(worstCard);
        updateHand();
        return worstCard;
    }

    @Override
    public boolean knock() {
        return deadwoodCount <= DEADWOOD_COUNT_TO_KNOCK;
    }

    @Override
    public void opponentEndTurnFeedback(boolean drewDiscard, Card previousDiscardTop, Card opponentDiscarded) {
        if (drewDiscard) {
            opponentHand.add(previousDiscardTop);
            discards.remove(previousDiscardTop);
        }
        opponentHand.remove(opponentDiscarded);
        discards.add(opponentDiscarded);
    }

    @Override
    public void opponentEndRoundFeedback(List<Card> opponentHand, List<Meld> opponentMelds) {

    }

    @Override
    public List<Meld> getMelds() {
        return melds;
    }

    @Override
    public void reset() {
        hand = new HashSet<>();
        opponentHand = new HashSet<>();
        deadwood = new HashSet<>();
        deadwoodCount = 0;
        melds = new ArrayList<>();
        discards = new HashSet<>();
    }

    private void updateHand(){
        melds = findBestMeldSet(hand);
        deadwood = extractDeadwood(hand, melds);
        deadwoodCount = calculateTotalValue(deadwood);
    }

    private static Card findWorstCard(HashMap<Card, Integer> cardScores){
        Card worstCard = null;
        int worstScore = Integer.MAX_VALUE;
        for (Card card : cardScores.keySet()) {
            if (cardScores.get(card) < worstScore) {
                worstCard = card;
                worstScore = cardScores.get(card);
            } else if (cardScores.get(card) == worstScore){
                //worstCard.getRankValue will never give null because it's insured above
                worstCard = (card.getRankValue() < worstCard.getRankValue()) ? worstCard : card;
                worstScore = cardScores.get(worstCard);
            }
        }
        
        return worstCard;
    }

    private HashMap<Card, Integer> computeCardScores(){
        ArrayList<Card> rankSortedCards = sortByRank(deadwood);
        ArrayList<Card> suitSortedCards = sortBySuit(deadwood);

        HashMap<Card, Integer> cardScores = new HashMap<>();

        for (Card card : hand) {
            cardScores.put(card, 0);
        }

        //2 of same rank
        for (int i = 0; i < rankSortedCards.size() - 1; i++) {
            if (rankSortedCards.get(i).getRankValue() == rankSortedCards.get(i+1).getRankValue()){
                cardScores.put(rankSortedCards.get(i), cardScores.get(rankSortedCards.get(i)) + SCORE_FOR_POSSIBLE_SET);
            }
        }
        //2 run of suit
        for (int i = 0; i < suitSortedCards.size() - 1; i++) {
            if (suitSortedCards.get(i).getSuit().equals(suitSortedCards.get(i+1).getSuit())
                    && suitSortedCards.get(i).getRankValue() + 1 == suitSortedCards.get(i+1).getRankValue()){
                cardScores.put(suitSortedCards.get(i), cardScores.get(suitSortedCards.get(i)) + SCORE_FOR_POSSIBLE_RUN);
            }
        }

        //don't drop what the opponent has in hand
        for (Card card : deadwood){
            if (opponentHand.contains(card)){
                cardScores.put(card, cardScores.get(card) + SCORE_FOR_OPPONENT_CARD);
            }
        }

        //give meld cards good score so they don't get dropped
        //drop from melds if the meld has 4+ cards
        for (Meld meld : melds){
            for (Card card : meld.getCards()){
                cardScores.put(card, cardScores.get(card) + SCORE_FOR_MELD);
            }
            if (meld instanceof RunMeld && meld.getCards().length > LARGE_MELD_THRESHOLD){
                ArrayList<Card> suitSortedMeld = sortBySuit(meld.getCards());
                cardScores.put(suitSortedMeld.get(0), cardScores.get(suitSortedMeld.get(0)) - 1);
                cardScores.put(suitSortedMeld.get(suitSortedMeld.size() - 1), cardScores.get(
                        suitSortedMeld.get(suitSortedMeld.size() - 1)) - SCORE_PENALTY_FOR_LARGE_MELD);
            } else if (meld instanceof SetMeld && meld.getCards().length > LARGE_MELD_THRESHOLD){
                for (Card card : meld.getCards()) {
                    cardScores.put(card, cardScores.get(card) - SCORE_PENALTY_FOR_LARGE_MELD);
                }
            }
        }

        /* For Competition */

        //2 of same rank
        ArrayList<ArrayList<Card>> almostMeld = new ArrayList<>();
        ArrayList<Card> deadwoodRankSorted = new ArrayList<>(sortByRank(deadwood));
        for (int i = 0; i <  deadwoodRankSorted.size() - 1; i++) {
            if (deadwoodRankSorted.get(i).getRankValue() == deadwoodRankSorted.get(i+1).getRankValue()){
                almostMeld.add(new ArrayList<>());
                almostMeld.get(almostMeld.size() - 1).add(deadwoodRankSorted.get(i));
                almostMeld.get(almostMeld.size() - 1).add(deadwoodRankSorted.get(i+1));

                i += 1;
            }
        }
        //2 run of suit
        ArrayList<Card> deadwoodSuitSorted = new ArrayList<>(sortBySuit(deadwood));

        for (int i = 0; i < deadwoodSuitSorted.size() - 1; i++) {
            if (suitSortedCards.get(i).getSuit().equals(suitSortedCards.get(i+1).getSuit())
                    && suitSortedCards.get(i).getRankValue() + 1 == suitSortedCards.get(i+1).getRankValue()){
                almostMeld.add(new ArrayList<>());
                almostMeld.get(almostMeld.size() - 1).add(deadwoodSuitSorted.get(i));
                almostMeld.get(almostMeld.size() - 1).add(deadwoodSuitSorted.get(i+1));
                i += 1;
            }
        }

//        ArrayList<Card> inDiscard = new ArrayList<>();
        for (ArrayList<Card> cards : almostMeld){
            int possibleMeldsInDiscard = numberPossibleMeldCards(discards, cards);
            for (int i = 0; i < possibleMeldsInDiscard; i++) {
                cardScores.put(cards.get(0), cardScores.get(cards.get(0)) - SCORE_PENALTY_FOR_IN_DISCARD);
            }
        }

        return cardScores;
    }
    /**
     * extracts the deadwood cards from a given set of cards and melds
     * @param cards the cards we want to extract deadwoods from
     * @param melds the melds in the cards
     * @return the set of deadwood cards
     */
    private static Set<Card> extractDeadwood(Set<Card> cards, List<Meld> melds){
        Set<Card> cardCopy = new HashSet<>(cards);
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
    private static int calculateTotalValue(Collection<Card> cards){
        Iterator<Card> cardIterator = cards.iterator();

        int count = 0;
        while (cardIterator.hasNext()){
            count += cardIterator.next().getPointValue();
        }

        return count;
    }



    /**
     * creates the best possible set of melds for the given card. This minimizes the deadwood count.
     * @param cards the cards we want to get melds from.
     * @return an arraylist of set of melds
     */
    private static ArrayList<Meld> findBestMeldSet(Set<Card> cards){
        ArrayList<Meld> allMelds = getAllMelds(cards);

        ArrayList<ArrayList<Meld>> allValidMeldSets = new ArrayList<>();
        findAllValidMeldSets(allMelds, allValidMeldSets);
        return pickBestMeldSet(allValidMeldSets);
    }

    /**
     * recursive function that finds all the valid meld sets that are in allMeldSets
     * @param melds possible valid meld set
     * @param allMeldSets all the meld sets
     */

    private static void findAllValidMeldSets(ArrayList<Meld> melds, ArrayList<ArrayList<Meld>> allMeldSets){
        if (isValidMeldSet(melds)){
            allMeldSets.add(melds);
        } else {
            ArrayList<Meld> meldsSubset;
            for (Meld meld: melds){
                meldsSubset = new ArrayList<>(melds);
                meldsSubset.remove(meld);
                //maybe it wont work because I'm not assigning this ?
                findAllValidMeldSets(meldsSubset, allMeldSets);
            }
        }
    }

    private static ArrayList<Meld> pickBestMeldSet(ArrayList<ArrayList<Meld>> meldSets){
        ArrayList<Meld> bestMeldSet = meldSets.get(0);
        int maximumCount = calculateTotalValue(meldSets.get(0));
        for (int i = 1; i < meldSets.size() ; i++) {
            int possibleBestMeldSetCount = calculateTotalValue(meldSets.get(i));
            if (possibleBestMeldSetCount > maximumCount) {
                maximumCount = possibleBestMeldSetCount;
                bestMeldSet = meldSets.get(i);
            }
        }

        return bestMeldSet;
    }

    private static int calculateTotalValue(ArrayList<Meld> melds) {
        int count = 0;
        for (Meld meld : melds) {
            for (Card card : meld.getCards()) {
                count += card.getPointValue();
            }
        }
        return count;
    }

    /**
     * true if the meldSet is valid, meaning there is no card which exists in multiple melds.
     * @param melds the meld set
     * @return whether the meld set is valid or not
     */
    private static boolean isValidMeldSet(ArrayList<Meld> melds){
        Set<Card> handSet = new HashSet<>();
        for (Meld meld : melds){
            for (Card card : meld.getCards()){
                if (!handSet.add(card)){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * gets all the possible melds from the given cards
     * idea is from https://gist.github.com/yalue/2622575
     * but the code is mine.
     *
     * @param cards the cards we want to know all the melds
     * @return arraylist of all possible melds with the given cards
     */
    private static ArrayList<Meld> getAllMelds(Set<Card> cards){
        ArrayList<Card> rankSortedCards = sortByRank(cards);
        ArrayList<Card> suitSortedCards = sortBySuit(cards);
        ArrayList<Meld> allMelds = new ArrayList<>();

        //4 of same rank + all 3 size combinations
        for (int i = 0; i < rankSortedCards.size() - 3; i++) {
            List<Card> cardSubset = rankSortedCards.subList(i, i+4);
            Meld possibleMeld = Meld.buildSetMeld(cardSubset);
            if (possibleMeld != null){
                allMelds.add(possibleMeld);

                ArrayList<Card> threeSubset1 = new ArrayList<>();

                threeSubset1.add(cardSubset.get(0));
                threeSubset1.add(cardSubset.get(1));
                threeSubset1.add(cardSubset.get(3));
                allMelds.add(Meld.buildSetMeld(threeSubset1));

                ArrayList<Card> threeSubset2 = new ArrayList<>();
                threeSubset2.add(cardSubset.get(0));
                threeSubset2.add(cardSubset.get(2));
                threeSubset2.add(cardSubset.get(3));
                allMelds.add(Meld.buildSetMeld(threeSubset2));

            }
        }

        //3 of same rank
        for (int i = 0; i < rankSortedCards.size() - 2; i++) {
            Meld possibleMeld = Meld.buildSetMeld(rankSortedCards.subList(i, i+2));
            if (possibleMeld != null){
                allMelds.add(possibleMeld);
            }
        }

        //3 run of same suit
        for (int i = 0; i < suitSortedCards.size() - 2; i++) {
            Meld possibleMeld = Meld.buildRunMeld(suitSortedCards.subList(i, i+2));
            if (possibleMeld != null){
                allMelds.add(possibleMeld);
            }
        }

        //4 run of same suit
        for (int i = 0; i < suitSortedCards.size() - 3; i++) {
            Meld possibleMeld = Meld.buildRunMeld(suitSortedCards.subList(i, i+3));
            if (possibleMeld != null){
                allMelds.add(possibleMeld);
            }
        }

        //5 run of same suit
        for (int i = 0; i < suitSortedCards.size() - 4; i++) {
            Meld possibleMeld = Meld.buildRunMeld(suitSortedCards.subList(i, i+4));
            if (possibleMeld != null){
                allMelds.add(possibleMeld);
            }
        }
        // LOOP11");

        return allMelds;
    }

    private static ArrayList<Card> sortByRank(Set<Card> cards){
        ArrayList<Card> cardList= new ArrayList<>(cards);
        Collections.sort(cardList);
        return cardList;
    }

    private static ArrayList<Card> sortBySuit(Set<Card> cards){
        ArrayList<Card> cardList = new ArrayList<>(cards);
        ArrayList<Card> sortedCards = new ArrayList<>();
        ArrayList<Card> cardsForSuit;

        for (Card.CardSuit suit : Card.CardSuit.values()) {
            cardsForSuit = new ArrayList<>();
            for (Card card : cardList) {
                if (card.getSuit() == suit){
                    cardsForSuit.add(card);
                }
            }
            Collections.sort(cardsForSuit);
            sortedCards.addAll(cardsForSuit);
        }
        return sortedCards;
    }

    private static ArrayList<Card> sortBySuit(Card[] cards){
        ArrayList<Card> sortedCards = new ArrayList<>();
        ArrayList<Card> cardsForSuit;

        for (Card.CardSuit suit : Card.CardSuit.values()) {
            cardsForSuit = new ArrayList<>();
            for (Card card : cards) {
                if (card.getSuit() == suit){
                    cardsForSuit.add(card);
                }
            }
            Collections.sort(cardsForSuit);
            sortedCards.addAll(cardsForSuit);
        }
        return sortedCards;
    }

    public Set<Card> getHand() {
        return hand;
    }

    public Set<Card> getDeadwood() {
        return deadwood;
    }

    /* After Deadline. For Competition */
    private static int numberPossibleMeldCards(Collection<Card> discards, ArrayList<Card> almostMelds){
        int numberOfCards = 0;
        if (almostMelds.get(0).getRankValue() == almostMelds.get(1).getRankValue()){
            for (Card card : discards){
                if (card.getRankValue() == almostMelds.get(0).getRankValue()){
                    numberOfCards++;
                }
            }
            return numberOfCards;
        } else {
            for (Card card : discards){
                if (card.getSuit().equals(almostMelds.get(0).getSuit())){
                    if (card.getRankValue() + 1 == almostMelds.get(0).getRankValue()) {
                        numberOfCards++;
                    } else if (card.getRankValue() - 1 == almostMelds.get(1).getRankValue()) {
                        numberOfCards++;
                    }
                }
            }
            return numberOfCards;
        }
    }
}
