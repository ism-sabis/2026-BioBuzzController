package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;

/** Turret: goes to a target angle, tracks a goal tag with the Limelight, or follows a stick. */
public final class Turret {
    /** Two readings of the same camera frame differ by less than this; new frames by more. */
    private static final double SAME_FRAME_MS = 3.0;

    private final RobotHardware robot;
    private double targetDegrees;
    private double lastFrameArrivalMs = Double.NEGATIVE_INFINITY;
    private boolean tagVisible;
    private int lastTagId = -1;
    private double lastTx;

    public Turret(RobotHardware robot) {
        this.robot = robot;
        targetDegrees = TurretMath.boundTarget(degrees(), RobotConfig.TURRET_LIMIT_DEGREES);
    }

    public double degrees() {
        return TurretMath.ticksToDegrees(robot.turret.getCurrentPosition(),
                RobotConfig.TURRET_TICKS_PER_REVOLUTION);
    }

    public double target() {
        return targetDegrees;
    }

    public void setTarget(double degrees) {
        targetDegrees = TurretMath.boundTarget(degrees, RobotConfig.TURRET_LIMIT_DEGREES);
    }

    /** True if the chosen tag was in the latest camera frame. */
    public boolean tagVisible() {
        return tagVisible;
    }

    public int lastTagId() {
        return lastTagId;
    }

    public double lastTx() {
        return lastTx;
    }

    /**
     * Points the target at a goal tag. The target only moves when a new camera frame arrives:
     * re-using an old frame while the turret is already turning made it overshoot. With no tag
     * in view the last target is kept.
     *
     * @param minTagId lowest AprilTag ID to aim at (inclusive)
     * @param maxTagId highest AprilTag ID to aim at (inclusive)
     */
    public void trackTag(int minTagId, int maxTagId) {
        tagVisible = false;
        LLResult result = robot.limelight.getLatestResult();
        if (result == null || !result.isValid()) return;
        double stalenessMs = result.getStaleness();
        if (stalenessMs > RobotConfig.LIMELIGHT_MAX_STALENESS_MS) return;

        LLResultTypes.FiducialResult best = null;
        for (LLResultTypes.FiducialResult tag : result.getFiducialResults()) {
            int id = tag.getFiducialId();
            if (id < minTagId || id > maxTagId) continue;
            if (best == null || tag.getTargetArea() > best.getTargetArea()) best = tag;
        }
        if (best == null) return;
        tagVisible = true;
        lastTagId = best.getFiducialId();
        lastTx = best.getTargetXDegrees();

        double arrivalMs = System.nanoTime() / 1.0e6 - stalenessMs;
        if (arrivalMs - lastFrameArrivalMs < SAME_FRAME_MS) return;
        lastFrameArrivalMs = arrivalMs;
        targetDegrees = TurretMath.aimTarget(degrees(), lastTx, RobotConfig.LIMELIGHT_AIM_GAIN,
                RobotConfig.TURRET_LIMIT_DEGREES);
    }

    /**
     * Turns by stick (+ is the positive encoder direction) inside the soft limits. The turret
     * then holds wherever it was released.
     */
    public void manual(double stick) {
        double current = degrees();
        robot.turret.setPower(TurretMath.limitPower(stick * RobotConfig.TURRET_MANUAL_POWER,
                current, RobotConfig.TURRET_LIMIT_DEGREES));
        targetDegrees = TurretMath.boundTarget(current, RobotConfig.TURRET_LIMIT_DEGREES);
    }

    /** Drives toward the target. Call every loop in which manual() is not called. */
    public void update() {
        double current = degrees();
        double power = TurretMath.positionPower(targetDegrees - current, RobotConfig.TURRET_KP,
                RobotConfig.TURRET_MAX_POWER, RobotConfig.TURRET_TOLERANCE_DEGREES);
        robot.turret.setPower(TurretMath.limitPower(power, current,
                RobotConfig.TURRET_LIMIT_DEGREES));
    }

    public void stop() {
        robot.turret.setPower(0.0);
    }
}
