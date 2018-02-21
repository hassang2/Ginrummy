package com.example;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class StrategyATest {

    StrategyA strategy;
    private ArrayList<Card> testCardsArray;
    private Set<Card> cards;

    @Before
    public void setup(){
        strategy = new StrategyA();
        testCardsArray = new ArrayList<>(Card.getAllCards());
        cards = new HashSet<>(testCardsArray.subList(0, 10));
    }

    @Test
    public void receiveInitialHandSize() {
        strategy.receiveInitialHand(testCardsArray.subList(0, 10));
        Assert.assertEquals(10, strategy.getHand().size());
    }

    @Test
    public void willTakeTopDiscard() {
        try{
            strategy.willTakeTopDiscard(testCardsArray.get(0));
            Assert.assertTrue(true);
        } catch(Exception e){
            fail();
        }
    }

    @Test
    public void drawAndDiscardNull() {
        Card discard = strategy.drawAndDiscard(testCardsArray.get(0));
        Assert.assertNull(discard);
    }

    @Test
    public void drawAndDiscardCorrectDiscard() {
        Card discard = strategy.drawAndDiscard(testCardsArray.get(0));
        Assert.assertFalse(strategy.getHand().contains(discard));
    }

    @Test
    public void knock() {
        try {
            strategy.knock();
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void opponentEndTurnFeedbackTrue() {
        try {
            strategy.opponentEndTurnFeedback(true, testCardsArray.get(3), testCardsArray.get(9));
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void opponentEndTurnFeedbackFalse() {
        try {
            strategy.opponentEndTurnFeedback(false, testCardsArray.get(8), testCardsArray.get(3));
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void opponentEndRoundFeedbackEmptyMeld() {
        try {
            strategy.opponentEndRoundFeedback(testCardsArray.subList(10, 20), new ArrayList<>());
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void opponentEndRoundFeedbackOneMeld() {
        ArrayList<Meld> melds = new ArrayList<>();
        Meld meld = new RunMeld(testCardsArray.subList(5, 9).toArray(new Card[4]));
        melds.add(meld);
        try {
            strategy.opponentEndRoundFeedback(testCardsArray.subList(10, 20), melds);
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void getMelds() {
        try {
            strategy.opponentEndTurnFeedback(false, testCardsArray.get(8), testCardsArray.get(3));
            Assert.assertTrue(true);
        } catch (Exception e){
            fail();
        }
    }

    @Test
    public void getMeldsNull() {
        Assert.assertNotNull(strategy.getMelds());
    }

    @Test
    public void resetHand() {
        strategy.reset();
        Assert.assertEquals(0, strategy.getHand().size());
    }

    @Test
    public void resetMeld() {
        strategy.reset();
        Assert.assertEquals(0, strategy.getMelds().size());
    }

    @Test
    public void resetDeadwood() {
        strategy.reset();
        Assert.assertEquals(0, strategy.getDeadwood().size());
    }
}