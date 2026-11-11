package org.firstinspires.ftc.teamcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FeedSequencerTest {
    private static final double OPEN = 0.22;
    private static final double GAP = 0.16;
    private static final double STEP = 0.01;

    /** Runs the sequencer for a while and counts how many separate gate pulses happen. */
    private static int countPulses(FeedSequencer feed, double seconds, ReadyAt launcher) {
        int pulses = 0;
        boolean wasOpen = false;
        for (double t = 0.0; t < seconds; t += STEP) {
            feed.update(launcher.ready(t, feed.gateOpen()), t, OPEN, GAP);
            if (feed.gateOpen() && !wasOpen) pulses++;
            wasOpen = feed.gateOpen();
        }
        return pulses;
    }

    private interface ReadyAt {
        boolean ready(double seconds, boolean gateOpen);
    }

    @Test
    public void idleUntilStarted() {
        FeedSequencer feed = new FeedSequencer();
        assertEquals(0, countPulses(feed, 1.0, (t, open) -> true));
        assertTrue(feed.isIdle());
    }

    @Test
    public void waitsForTheLauncherToBeReady() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(1);
        assertEquals(0, countPulses(feed, 1.0, (t, open) -> false));
        assertEquals(FeedSequencer.State.WAIT_READY, feed.state());
        assertEquals(1, feed.ballsRemaining());
    }

    @Test
    public void oneBallIsOnePulseEvenWhenSpeedDipsDuringThePulse() {
        // The launcher slows down whenever the gate is open, as it does when a ball hits it.
        FeedSequencer feed = new FeedSequencer();
        feed.start(1);
        assertEquals(1, countPulses(feed, 3.0, (t, open) -> !open));
        assertTrue(feed.isIdle());
        assertEquals(0, feed.ballsRemaining());
    }

    @Test
    public void fourBallsAreFourPulses() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(4);
        assertEquals(4, countPulses(feed, 5.0, (t, open) -> !open));
        assertTrue(feed.isIdle());
    }

    @Test
    public void pulseLastsTheFullOpenTime() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(1);
        feed.update(true, 10.0, OPEN, GAP);
        assertTrue(feed.gateOpen());
        feed.update(false, 10.0 + OPEN - 0.01, OPEN, GAP);
        assertTrue(feed.gateOpen());
        feed.update(false, 10.0 + OPEN, OPEN, GAP);
        assertFalse(feed.gateOpen());
    }

    @Test
    public void nextBallWaitsForTheGapAndForSpeedToRecover() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(2);
        feed.update(true, 0.0, OPEN, GAP);
        feed.update(true, OPEN, OPEN, GAP);
        assertEquals(FeedSequencer.State.GAP, feed.state());
        feed.update(true, OPEN + GAP - 0.01, OPEN, GAP);
        assertFalse(feed.gateOpen());
        feed.update(false, OPEN + GAP, OPEN, GAP);
        assertEquals(FeedSequencer.State.WAIT_READY, feed.state());
        feed.update(false, OPEN + GAP + 0.5, OPEN, GAP);
        assertFalse(feed.gateOpen());
        feed.update(true, OPEN + GAP + 0.6, OPEN, GAP);
        assertTrue(feed.gateOpen());
    }

    @Test
    public void cancelClosesTheGateAndClearsTheQueue() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(3);
        feed.update(true, 0.0, OPEN, GAP);
        assertTrue(feed.gateOpen());
        feed.cancel();
        assertFalse(feed.gateOpen());
        assertEquals(0, countPulses(feed, 2.0, (t, open) -> true));
    }

    @Test
    public void aNewRequestReplacesTheOldOne() {
        FeedSequencer feed = new FeedSequencer();
        feed.start(4);
        feed.start(1);
        assertEquals(1, countPulses(feed, 3.0, (t, open) -> true));
    }
}
