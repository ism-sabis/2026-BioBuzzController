package org.firstinspires.ftc.teamcode;

/** Backboard slide: goes to a target in encoder ticks, or follows a manual stick. */
public final class Slide {
    private final RobotHardware robot;
    private int targetTicks;

    public Slide(RobotHardware robot) {
        this.robot = robot;
        targetTicks = clampTicks(position());
    }

    public int position() {
        return robot.backboardSlide.getCurrentPosition();
    }

    public int target() {
        return targetTicks;
    }

    public void setTarget(int ticks) {
        targetTicks = clampTicks(ticks);
    }

    /**
     * Moves by stick (+ extends) inside the soft limits. The slide then holds wherever it was
     * released instead of running back to its old target.
     */
    public void manual(double stick) {
        double power = stick * RobotConfig.SLIDE_MANUAL_POWER;
        int ticks = position();
        if ((ticks <= 0 && power < 0.0) || (ticks >= RobotConfig.SLIDE_MAX_TICKS && power > 0.0)) {
            power = 0.0;
        }
        robot.backboardSlide.setPower(power);
        targetTicks = clampTicks(ticks);
    }

    /** Drives toward the target. Call every loop in which manual() is not called. */
    public void update() {
        double error = clampTicks(targetTicks) - position();
        robot.backboardSlide.setPower(TurretMath.positionPower(error, RobotConfig.SLIDE_KP,
                RobotConfig.SLIDE_MAX_POWER, RobotConfig.SLIDE_TOLERANCE_TICKS));
    }

    public void stop() {
        robot.backboardSlide.setPower(0.0);
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(RobotConfig.SLIDE_MAX_TICKS, ticks));
    }
}
