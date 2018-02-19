package com.example;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StrategyC implements PlayerStrategy {

    Set<Card> hand = new HashSet<>();

    @Override
    public void receiveInitialHand(List<Card> hand) {

    }

    @Override
    public boolean willTakeTopDiscard(Card card) {
        if (Math.random() < .5){
            return false;
        }
        hand.add(card);
        return true;
    }

    @Override
    public Card drawAndDiscard(Card drawnCard) {
        hand.remove(drawnCard);
        return drawnCard;
    }

    @Override
    public boolean knock() {
        return true;
    }

    @Override
    public void opponentEndTurnFeedback(boolean drewDiscard, Card previousDiscardTop, Card opponentDiscarded) {

    }

    @Override
    public void opponentEndRoundFeedback(List<Card> opponentHand, List<Meld> opponentMelds) {

    }

    @Override
    public List<Meld> getMelds() {
        return null;
    }

    @Override
    public void reset() {

    }
}
