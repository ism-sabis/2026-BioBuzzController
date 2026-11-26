package org.firstinspires.ftc.teamcode;

/** Launcher wheel, gate servo and intake, including the timed ball-feed sequence. */
public final class Shooter {
    private final RobotHardware robot;
    private final FeedSequencer feed = new FeedSequencer();
    private boolean spinning;
    private boolean initialGateOpen = true;
    private boolean manualGateOpen;
    private boolean manualGateBypassReady;

    public Shooter(RobotHardware robot) {
        this.robot = robot;
    }

    /** Spins the launcher up and feeds this many balls, one pulse each, once it is at speed. */
    public void startFeed(int balls) {
        spinning = true;
        initialGateOpen = false;
        manualGateOpen = false;
        manualGateBypassReady = false;
        feed.start(balls);
    }

    /** Drops the remaining balls of a sequence. The launcher keeps spinning. */
    public void cancelFeed() {
        feed.cancel();
    }

    /** Cancels feeding, closes the gate and lets the launcher coast down. */
    public void shutDown() {
        feed.cancel();
        initialGateOpen = false;
        manualGateOpen = false;
        manualGateBypassReady = false;
        spinning = false;
    }

    public void setSpinning(boolean on) {
        spinning = on;
    }

    public boolean isSpinning() {
        return spinning;
    }

    /** Normal manual gate request; opening still waits for launcher-ready speed. */
    public void setManualGate(boolean open) {
        initialGateOpen = false;
        manualGateOpen = open;
        manualGateBypassReady = false;
    }

    /** Manual gate control for bench tuning; opening is allowed at any launcher speed. */
    public void setManualGateForTesting(boolean open) {
        if (open || manualGateOpen) initialGateOpen = false;
        manualGateOpen = open;
        manualGateBypassReady = open;
    }

    public boolean gateOpen() {
        return initialGateOpen || feed.gateOpen() || (feed.isIdle() && manualGateOpen
                && (manualGateBypassReady || ready()));
    }

    public double rpm() {
        double ticksPerRev = ticksPerOutputRevolution();
        if (ticksPerRev <= 0.0) return 0.0;
        return Math.abs(robot.launcher.getVelocity()) * 60.0 / ticksPerRev;
    }

    public boolean ready() {
        return rpm() >= RobotConfig.LAUNCHER_TARGET_RPM * RobotConfig.LAUNCHER_READY_RPM_FRACTION;
    }

    public FeedSequencer.State feedState() {
        return feed.state();
    }

    public int ballsRemaining() {
        return feed.ballsRemaining();
    }

    /** Call once per loop. */
    public void update(double nowSeconds) {
        if (spinning) {
            // The hub's own velocity loop holds the speed; the motor coasts when switched off.
            robot.launcher.setVelocity(RobotConfig.LAUNCHER_TARGET_RPM
                    * ticksPerOutputRevolution() / 60.0);
        } else {
            robot.launcher.setPower(0.0);
        }
        feed.update(ready(), nowSeconds, RobotConfig.FEED_OPEN_SECONDS,
                RobotConfig.FEED_GAP_SECONDS);
        robot.launcherGate.setPosition(Ramp.clamp01(gateOpen()
                ? RobotConfig.GATE_OPEN : RobotConfig.GATE_CLOSED));
        robot.intake.setPower(feed.gateOpen() ? RobotConfig.INTAKE_POWER : 0.0);
    }

    private static double ticksPerOutputRevolution() {
        return RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION * RobotConfig.LAUNCHER_GEAR_RATIO;
    }
}
