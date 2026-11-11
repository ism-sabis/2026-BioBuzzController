package org.firstinspires.ftc.teamcode;

/**
 * Decides when the gate is open while feeding a requested number of balls. It has no hardware
 * in it, so it can be tested on a laptop.
 *
 * A pulse only starts while the launcher is at speed. Once it has started it always runs for the
 * full open time and counts as one ball: launcher speed is expected to dip as the ball goes
 * through, and cutting the pulse short there used to feed extra balls.
 */
public final class FeedSequencer {
    public enum State { IDLE, WAIT_READY, OPEN, GAP }

    private State state = State.IDLE;
    private int ballsRemaining;
    private double stateStartedSeconds;

    /** Starts a new sequence, replacing whatever was left of the old one. */
    public void start(int balls) {
        ballsRemaining = Math.max(0, balls);
        state = ballsRemaining > 0 ? State.WAIT_READY : State.IDLE;
    }

    public void cancel() {
        ballsRemaining = 0;
        state = State.IDLE;
    }

    /** Call once per loop. */
    public void update(boolean launcherReady, double nowSeconds, double openSeconds,
                       double gapSeconds) {
        switch (state) {
            case WAIT_READY:
                if (launcherReady) {
                    state = State.OPEN;
                    stateStartedSeconds = nowSeconds;
                }
                break;
            case OPEN:
                if (nowSeconds - stateStartedSeconds >= openSeconds) {
                    ballsRemaining--;
                    state = ballsRemaining > 0 ? State.GAP : State.IDLE;
                    stateStartedSeconds = nowSeconds;
                }
                break;
            case GAP:
                if (nowSeconds - stateStartedSeconds >= gapSeconds) state = State.WAIT_READY;
                break;
            default:
                break;
        }
    }

    /** True while a ball should be passing: gate open and intake running. */
    public boolean gateOpen() {
        return state == State.OPEN;
    }

    public boolean isIdle() {
        return state == State.IDLE;
    }

    public State state() {
        return state;
    }

    public int ballsRemaining() {
        return ballsRemaining;
    }
}
