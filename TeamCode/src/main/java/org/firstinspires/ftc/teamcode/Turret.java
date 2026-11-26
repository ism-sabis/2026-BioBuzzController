package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;

/** Turret: profiled angle control for presets, Limelight aiming, and manual target jogging. */
public final class Turret {
    /** Two readings of the same camera frame differ by less than this; new frames by more. */
    private static final double SAME_FRAME_MS = 3.0;

    private final RobotHardware robot;
    private double targetDegrees;
    private double lastFrameArrivalMs = Double.NEGATIVE_INFINITY;
    private boolean tagVisible;
    private int lastTagId = -1;
    private double lastTx;
    private double lastPowerRequest;
    private boolean profileInitialized;
    private double profilePositionDegrees;
    private double profileVelocityDegreesPerSecond;
    private long lastProfileUpdateNanos;
    private long lastManualCommandNanos;

    public Turret(RobotHardware robot) {
        this.robot = robot;
        targetDegrees = TurretMath.boundTarget(degrees(), RobotConfig.TURRET_MIN_DEGREES,
                RobotConfig.TURRET_MAX_DEGREES);
    }

    public double degrees() {
        return TurretMath.ticksToDegrees(robot.turret.getCurrentPosition(),
                RobotConfig.TURRET_TICKS_PER_REVOLUTION);
    }

    /** Encoder velocity in turret-output degrees per second, signed like {@link #degrees()}. */
    public double measuredVelocityDegreesPerSecond() {
        double ticksPerRevolution = Math.max(1.0,
                Math.abs(RobotConfig.TURRET_TICKS_PER_REVOLUTION));
        return robot.turret.getVelocity() * 360.0 / ticksPerRevolution;
    }

    public double target() {
        return targetDegrees;
    }

    public void setTarget(double degrees) {
        targetDegrees = TurretMath.boundTarget(degrees, RobotConfig.TURRET_MIN_DEGREES,
                RobotConfig.TURRET_MAX_DEGREES);
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

    /** Last normalized power sent to the turret motor, for tuning telemetry. */
    public double lastPowerRequest() {
        return lastPowerRequest;
    }

    public double profilePositionDegrees() {
        return profilePositionDegrees;
    }

    public double profileVelocityDegreesPerSecond() {
        return profileVelocityDegreesPerSecond;
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
        if (robot.limelight == null) return;
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
                RobotConfig.TURRET_MIN_DEGREES, RobotConfig.TURRET_MAX_DEGREES);
    }

    /**
     * Uses the stick to jog the requested angle (+ is the positive encoder direction). This only
     * changes the target; update() still runs the same profiled position controller and encoder
     * feedback used by automatic aiming. Releasing the stick leaves the target where it was.
     */
    public void manual(double stick) {
        long now = System.nanoTime();
        double elapsed = lastManualCommandNanos == 0L ? 0.0
                : (now - lastManualCommandNanos) / 1.0e9;
        lastManualCommandNanos = now;
        double dt = elapsed > 0.1 ? 0.0 : Math.max(0.0, Math.min(0.05, elapsed));

        double rate = Math.abs(RobotConfig.TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC);
        targetDegrees = clamp(targetDegrees + stick * rate * dt,
                RobotConfig.TURRET_MIN_DEGREES, RobotConfig.TURRET_MAX_DEGREES);
    }

    /** Drives every automatic target move with the acceleration-limited profile. */
    public void update() {
        updateProfiled();
    }

    /**
     * Automatic motion: accelerate, cruise when distance permits, then brake using the remaining
     * stopping distance. Separate acceleration and braking rates let the turret build speed quickly
     * while starting to slow earlier for a precise stop. Position feedback follows the profile.
     */
    public void updateProfiled() {
        double current = degrees();
        long now = System.nanoTime();
        if (!profileInitialized) {
            profilePositionDegrees = current;
            profileVelocityDegreesPerSecond = 0.0;
            lastProfileUpdateNanos = now;
            profileInitialized = true;
        }

        double dt = (now - lastProfileUpdateNanos) / 1.0e9;
        lastProfileUpdateNanos = now;
        dt = Math.max(0.001, Math.min(0.05, dt));

        double maxVelocity = Math.max(1.0, Math.abs(
                RobotConfig.TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC));
        double acceleration = Math.max(1.0, Math.abs(
                RobotConfig.TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ));
        double brakingAcceleration = Math.max(1.0, Math.abs(
                RobotConfig.TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ));
        double distanceRemaining = targetDegrees - profilePositionDegrees;
        double desiredVelocity = 0.0;
        if (Math.abs(distanceRemaining) > 0.01) {
            double stoppingVelocity = Math.sqrt(
                    2.0 * brakingAcceleration * Math.abs(distanceRemaining));
            desiredVelocity = Math.copySign(Math.min(maxVelocity, stoppingVelocity),
                    distanceRemaining);
        }

        double previousVelocity = profileVelocityDegreesPerSecond;
        boolean reversing = previousVelocity * desiredVelocity < 0.0;
        boolean slowing = Math.abs(desiredVelocity) < Math.abs(previousVelocity);
        double rateLimit = slowing || reversing ? brakingAcceleration : acceleration;
        profileVelocityDegreesPerSecond = moveToward(previousVelocity, desiredVelocity,
                rateLimit * dt);
        double averageVelocity = (previousVelocity + profileVelocityDegreesPerSecond) * 0.5;
        double nextProfilePosition = profilePositionDegrees + averageVelocity * dt;
        if (distanceRemaining == 0.0
                || distanceRemaining * (targetDegrees - nextProfilePosition) <= 0.0) {
            profilePositionDegrees = targetDegrees;
            profileVelocityDegreesPerSecond = 0.0;
        } else {
            profilePositionDegrees = nextProfilePosition;
        }

        double maxPower = Math.max(0.0, Math.min(1.0, RobotConfig.TURRET_MAX_POWER));
        double finalError = targetDegrees - current;
        double feedforward = profileVelocityDegreesPerSecond / maxVelocity * maxPower;
        // If inertia carries the turret past its target, the motion profile can still be moving
        // toward the old target. Do not let that stale feedforward cancel the position correction.
        if (feedforward * finalError <= 0.0) feedforward = 0.0;
        double profileError = profilePositionDegrees - current;
        double power = feedforward + profileError * RobotConfig.TURRET_KP;
        power = Math.max(-maxPower, Math.min(maxPower, power));

        if (Math.abs(finalError) <= Math.abs(RobotConfig.TURRET_TOLERANCE_DEGREES)
                && Math.abs(profileVelocityDegreesPerSecond) <= 1.0) {
            profilePositionDegrees = targetDegrees;
            profileVelocityDegreesPerSecond = 0.0;
            power = 0.0;
        } else if (Math.abs(finalError) > Math.abs(RobotConfig.TURRET_TOLERANCE_DEGREES)) {
            double floor = Math.min(Math.abs(RobotConfig.TURRET_MIN_POWER), maxPower);
            if (Math.abs(power) < floor) {
                power = Math.copySign(floor, power == 0.0 ? finalError : power);
            }
        }

        lastPowerRequest = TurretMath.limitPower(power, current, RobotConfig.TURRET_MIN_DEGREES,
                RobotConfig.TURRET_MAX_DEGREES);
        robot.turret.setPower(lastPowerRequest);
    }

    private static double moveToward(double current, double target, double maxChange) {
        if (current < target) return Math.min(target, current + maxChange);
        return Math.max(target, current - maxChange);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void stop() {
        profileInitialized = false;
        profileVelocityDegreesPerSecond = 0.0;
        lastPowerRequest = 0.0;
        robot.turret.setPower(lastPowerRequest);
    }
}
