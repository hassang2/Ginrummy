package com.example;

import com.sun.jndi.toolkit.url.Uri;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UtilityTest {

    private ArrayList<Card> testCardsArray;
    private Set<Card> cards;

    @Before
    public void setUp() throws Exception {

        testCardsArray = new ArrayList<>(Card.getAllCards());
        cards = new HashSet<>(testCardsArray.subList(0, 10));

    }
    @Test
    public void validMeldsNull() {
        Assert.assertTrue(Utility.validMelds(cards, new StrategyA()));
    }


    @Test
    public void extractDeadwoodNoMeld() {
        Set<Card> testSet = new HashSet<>(testCardsArray.subList(0, 2));
        Set<Card> deadwood = Utility.extractDeadwood(testSet, null);
        Set<Card> expectedSet = new HashSet<>();
        expectedSet.add(testCardsArray.get(0));
        expectedSet.add(testCardsArray.get(1));

        Assert.assertEquals(expectedSet, deadwood);
    }

    @Test
    public void extractDeadwoodAllMeld() {
        Set<Card> testSet = new HashSet<>(testCardsArray.subList(0, 2));
        ArrayList<Meld> melds = new ArrayList<>();
        melds.add(new RunMeld(testCardsArray.subList(0, 2).toArray(new Card[2])));
        Set<Card> deadwood = Utility.extractDeadwood(testSet, melds);

        Assert.assertEquals(0, deadwood.size());
    }

    @Test
    public void calculateDeadwoodCount() {
        int deadwoodCount = Utility.calculateDeadwoodCount(testCardsArray.subList(0, 2));
        Assert.assertEquals(testCardsArray.get(0).getPointValue() + testCardsArray.get(1).getPointValue(), deadwoodCount);
    }

    @Test
    public void shuffle() {
        ArrayList<Card> shuffledCards = Utility.shuffle(cards);
        Assert.assertFalse(cards.equals(shuffledCards));
    }

    @Test
    public void layoffNone() {
        Set<Card> testSet = new HashSet<>(testCardsArray.subList(0, 1));
        ArrayList<Meld> melds = new ArrayList<>();
        melds.add(new RunMeld(testCardsArray.subList(1, 2).toArray(new Card[1])));

        Set<Card> afterLayoff = Utility.layoff(melds, testSet);
        Assert.assertEquals(testSet, afterLayoff);
    }
}