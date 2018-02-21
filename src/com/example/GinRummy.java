package com.example;

import java.util.*;

public class GinRummy {

    private static final int SCORE_TO_WIN = 50;

    private HashMap<PlayerStrategy, Integer> scores = new HashMap<>();
    private PlayerStrategy winner = null;
    private PlayerStrategy player1;
    private PlayerStrategy player2;


    public GinRummy(PlayerStrategy p1, PlayerStrategy p2){
        player1 = p1;
        player2 = p2;
        scores.put(p1, 0);
        scores.put(p2, 0);
    }

    public void playGame(){

        while(getWinner() == null) {

            Round round = new Round(player1, player2);
            round.initRound();
            round.playRound();
            PlayerStrategy roundWinner = round.getWinner();
            if (roundWinner != null) {
                scores.put(roundWinner, scores.get(roundWinner) + round.getRoundScore());
            }

            if (scores.get(player1) >= SCORE_TO_WIN) {
                winner = player1;

            } else if (scores.get(player2) >= SCORE_TO_WIN) {
                winner = player2;
            }
        }
    }

    public PlayerStrategy getWinner(){
        return winner;
    }
}
