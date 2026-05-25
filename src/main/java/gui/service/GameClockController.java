package gui.service;

import chess.game.GameSession;
import chess.game.GameState;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class GameClockController {

    private Timeline gameClock;

    public void start(GameSession session, Runnable onTick) {

        if (session == null || !session.getConfig().hasTimedGame()) {
            return;
        }

        stop();

        gameClock = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {

                    if (session.getCurrentState() == GameState.RUNNING) {
                        session.updateClock();
                        onTick.run();
                    }

                })
        );

        gameClock.setCycleCount(Animation.INDEFINITE);
        gameClock.play();
    }

    public void stop() {
        if (gameClock != null) {
            gameClock.stop();
        }
    }
}