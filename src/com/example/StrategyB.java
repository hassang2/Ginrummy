package com.example;
import java.util.*;

/**
 * Knocks as fast as possible
 */
public class StrategyB implements PlayerStrategy {

    private List<Meld> melds = new ArrayList<>();
    private Set<Card> hand = new HashSet<>();
    private Set<Card> opponentHand = new HashSet<>();
    private Set<Card> deadwood = new HashSet<>();
    private int deadwoodCount = 0;



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

        HashMap<Card, Integer> cardScores = getDeadwoodCardScores(deadwood);

        Card worseCard = findWorstCard(cardScores);

        hand.remove(worseCard);
        updateHand();
        return worseCard;
    }

    @Override
    public boolean knock() {
        return deadwoodCount <= 10;
    }

    @Override
    public void opponentEndTurnFeedback(boolean drewDiscard, Card previousDiscardTop, Card opponentDiscarded) {
        if (drewDiscard) {
            opponentHand.add(previousDiscardTop);
        }
        opponentHand.remove(opponentDiscarded);
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
    }

    private void updateHand(){
        melds = findBestMeldSet(hand);
        deadwood = extractDeadwood(hand, melds);
        deadwoodCount = calculateTotalValue(deadwood);
    }

    private static Card findWorstCard(HashMap<Card, Integer> cardScores){
        Card worseCard = null;
        int worseScore = Integer.MAX_VALUE;
        for (Card card : cardScores.keySet()) {
            if (cardScores.get(card) < worseScore) {
                worseCard = card;
                worseScore = cardScores.get(card);
            }
        }

        return worseCard;
    }

    private static HashMap<Card, Integer> getDeadwoodCardScores(Set<Card> deadwoods){
        ArrayList<Card> deadwoodsArrayList = new ArrayList<>(deadwoods);
        ArrayList<Card> rankSortedCards = sortByRank(deadwoods);
        ArrayList<Card> suitSortedCards = sortBySuit(deadwoods);

        HashMap<Card, Integer> cardScores = new HashMap<>();

        for (int i = 0; i < deadwoodsArrayList.size(); i++) {
            cardScores.put(deadwoodsArrayList.get(i), 0);
        }
        //2 of same rank
        for (int i = 0; i < rankSortedCards.size() - 1; i++) {
            if (rankSortedCards.get(i).equals(rankSortedCards.get(i+1))){
                cardScores.put(rankSortedCards.get(i), cardScores.get(rankSortedCards.get(i)) + 1);
            }
        }
        //2 run of suit
        for (int i = 0; i < suitSortedCards.size() - 1; i++) {
            if (suitSortedCards.get(i).equals(suitSortedCards.get(i+1))){
                cardScores.put(suitSortedCards.get(i), cardScores.get(suitSortedCards.get(i)) + 1);
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

        ArrayList<ArrayList<Meld>> allValidMeldSets = findAllValidMeldSets(allMelds, new ArrayList<>());
        return pickBestMeldSet(allValidMeldSets);
    }

    private static ArrayList<ArrayList<Meld>> findAllValidMeldSets(ArrayList<Meld> melds, ArrayList<ArrayList<Meld>> allMeldSets){

        if (isValidMeldSet(melds)){
            allMeldSets.add(melds);
        } else {
            for (Meld meld: melds){
                ArrayList<Meld> meldsSubset = new ArrayList<>(melds);
                meldsSubset.remove(meld);
                //maybe it wont work because I'm not assigning this ?
                findAllValidMeldSets(meldsSubset, allMeldSets);
            }
        }
        return allMeldSets;
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
}
