package gui.service;

import gui.audio.SoundManager;

public class MoveSoundService {

    public void playMoveSound(boolean soundEnabled, boolean isCheckmate, boolean isCheck,
                              boolean wasPromotion, boolean wasCapture) {
        if (!soundEnabled) return;

        if (isCheckmate) {
            SoundManager.playCheckmate();
        } else if (isCheck) {
            SoundManager.playCheck();
        } else if (wasPromotion) {
            SoundManager.playPromote();
        } else if (wasCapture) {
            SoundManager.playCapture();
        } else {
            SoundManager.playMove();
        }
    }
}