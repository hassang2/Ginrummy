package com.example;

import java.time.Clock;
import java.util.HashMap;

public class Competition {

    private static final int NUMBER_OF_GAMES = 500;
    public static void main(String[] args){

        GinRummy game;
        HashMap<PlayerStrategy, Integer> gameWins = new HashMap<>();
        PlayerStrategy player1 = new StrategyA();
        PlayerStrategy player2 = new StrategyC();

        gameWins.put(player1, 0);
        gameWins.put(player2, 0);

        for (int i = 0; i < NUMBER_OF_GAMES; i++) {
            long sTime = System.nanoTime();
            game = new GinRummy(player1, player2);
            game.playGame();
            gameWins.put(game.getWinner(), gameWins.get(game.getWinner()) + 1);
            long eTime = System.nanoTime();
            long duration = (eTime - sTime) / 10000;
            System.out.println("Game " + (i+1) + " :  " + game.getWinner() + " won in " + duration);
        }

        System.out.println("player1 won " + gameWins.get(player1) + " games");
        System.out.println("player2 won " + gameWins.get(player2) + " games");

    }
}
