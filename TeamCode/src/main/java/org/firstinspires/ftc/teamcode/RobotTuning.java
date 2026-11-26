package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.Locale;

/**
 * Bring-up utility controlled entirely with Gamepad 1. Use D-pad up/down to select a value and
 * left/right to adjust it; X selects fine steps and Y selects coarse steps. The ramp servos tune as
 * a mirrored pair. Hold the bottom Cross/X button (FTC A) to spin up the launcher; hold Circle/O
 * (FTC B) to test the gate at any launcher speed, or hold right trigger plus Circle to flip between
 * turret test angles. Hold right trigger with the D-pad to change the target without changing a value.
 * Start it with the slide retracted and the turret at home: it zeroes both encoders.
 * Adjusted values are live for the current controller session; copy the reported Java values into
 * RobotConfig so they survive an app restart. Keep the robot clear when enabling manual outputs.
 */
@TeleOp(name = "BioBuzz Tuning", group = "BioBuzz")
public class RobotTuning extends OpMode {
    private RobotHardware robot;
    private Slide slide;
    private Turret turret;
    private Shooter shooter;
    private int selected;
    private double rampLeftPosition;
    private double rampRightPosition;
    private double hoodPosition;
    private boolean previousUp;
    private boolean previousDown;
    private boolean previousLeft;
    private boolean previousRight;
    private boolean previousTurretLeft;
    private boolean previousTurretRight;
    private boolean previousTurretHome;
    private boolean previousTurretFlip;
    private boolean nextTestFlipPositive = true;

    private final String[] names = {
            "SLIDE_MAX_TICKS", "SLIDE_FLOWER_TICKS", "SLIDE_KP",
            "TURRET_TICKS_PER_REVOLUTION", "TURRET_MIN_DEGREES", "TURRET_FLOWER_DEGREES",
            "TURRET_KP", "RAMP_STOW_PAIR", "RAMP_LEVEL_PAIR", "GATE_CLOSED", "GATE_OPEN",
            "LAUNCHER_TICKS_PER_REVOLUTION",
            "LAUNCHER_TARGET_RPM", "FEED_OPEN_SECONDS", "FEED_GAP_SECONDS",
            "TURRET_MAX_POWER", "TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC",
            "TURRET_TOLERANCE_DEGREES", "TURRET_TEST_STEP_DEGREES", "TURRET_MIN_POWER",
            "TURRET_TEST_FLIP_DEGREES", "TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC",
            "TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ", "TURRET_MAX_DEGREES",
            "TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ"
    };

    @Override
    public void init() {
        robot = new RobotHardware(hardwareMap);
        robot.initialize();
        // Tuning always starts from a fresh zero: put the slide and turret at home first.
        robot.rezeroMechanisms();
        slide = new Slide(robot);
        turret = new Turret(robot);
        shooter = new Shooter(robot);
        if (robot.limelight != null) robot.limelight.start();
        rampLeftPosition = RobotConfig.RAMP_LEFT_STOW;
        rampRightPosition = RobotConfig.RAMP_RIGHT_STOW;
        hoodPosition = clamp01(robot.hood.getPosition());
        telemetry.addLine("One-controller tuning ready. D-pad selects/adjusts; X fine, Y coarse.");
        telemetry.update();
    }

    @Override
    public void loop() {
        robot.clearBulkCache();
        boolean turretTargetMode = gamepad1.right_trigger > 0.5;
        boolean turretFlipPressed = turretTargetMode && gamepad1.b;
        if (!turretTargetMode) {
            if (gamepad1.dpad_up && !previousUp) selected = (selected + names.length - 1) % names.length;
            if (gamepad1.dpad_down && !previousDown) selected = (selected + 1) % names.length;
            double step = gamepad1.y ? 10.0 : (gamepad1.x ? 0.1 : 1.0);
            if (gamepad1.dpad_right && !previousRight) adjust(step);
            if (gamepad1.dpad_left && !previousLeft) adjust(-step);
        }
        previousUp = gamepad1.dpad_up;
        previousDown = gamepad1.dpad_down;
        previousLeft = gamepad1.dpad_left;
        previousRight = gamepad1.dpad_right;

        if (gamepad1.left_trigger > 0.5) {
            double hoodStick = MecanumMath.deadband(-gamepad1.right_stick_y,
                    RobotConfig.STICK_DEADBAND);
            if (hoodStick != 0.0) {
                hoodPosition = clamp01(hoodPosition + hoodStick * 0.002);
                RobotConfig.HOOD_POSITION = hoodPosition;
                robot.hood.setPosition(hoodPosition);
            }
        } else {
            double rampNudge = gamepad1.right_stick_y * 0.005;
            if (gamepad1.left_bumper) {
                moveRampPairFromLeft(rampLeftPosition + rampNudge);
            } else if (gamepad1.right_bumper) {
                moveRampPairFromRight(rampRightPosition + rampNudge);
            }
        }
        robot.rampLeft.setPosition(rampLeftPosition);
        robot.rampRight.setPosition(rampRightPosition);

        // Slide remains manual power; turret stick input edits the angle target only.
        if (gamepad1.left_stick_button) {
            slide.manual(MecanumMath.deadband(-gamepad1.left_stick_y, RobotConfig.STICK_DEADBAND));
        } else {
            slide.stop();
        }
        if (!gamepad1.right_stick_button) {
            if (turretTargetMode && gamepad1.dpad_left && !previousTurretLeft) {
                turret.setTarget(turret.target() - RobotConfig.TURRET_TEST_STEP_DEGREES);
            }
            if (turretTargetMode && gamepad1.dpad_right && !previousTurretRight) {
                turret.setTarget(turret.target() + RobotConfig.TURRET_TEST_STEP_DEGREES);
            }
            if (turretTargetMode && gamepad1.dpad_up && !previousTurretHome) turret.setTarget(0.0);
            if (turretFlipPressed && !previousTurretFlip) {
                double direction = nextTestFlipPositive ? 1.0 : -1.0;
                double flipAngle = Math.abs(RobotConfig.TURRET_TEST_FLIP_DEGREES);
                double testMin = RobotConfig.TURRET_MIN_DEGREES
                        + RobotConfig.TURRET_TEST_LIMIT_MARGIN_DEGREES;
                double testMax = RobotConfig.TURRET_MAX_DEGREES
                        - RobotConfig.TURRET_TEST_LIMIT_MARGIN_DEGREES;
                if (testMin > testMax) {
                    double midpoint = (RobotConfig.TURRET_MIN_DEGREES
                            + RobotConfig.TURRET_MAX_DEGREES) * 0.5;
                    testMin = midpoint;
                    testMax = midpoint;
                }
                turret.setTarget(Math.max(testMin, Math.min(testMax, direction * flipAngle)));
                nextTestFlipPositive = !nextTestFlipPositive;
            }
        }
        previousTurretLeft = gamepad1.dpad_left;
        previousTurretRight = gamepad1.dpad_right;
        previousTurretHome = gamepad1.dpad_up;
        previousTurretFlip = turretFlipPressed;
        if (gamepad1.right_stick_button) {
            turret.manual(MecanumMath.deadband(gamepad1.right_stick_x, RobotConfig.STICK_DEADBAND));
        }
        turret.update();

        // Bench tuning lets Circle open the gate at any RPM; releasing it selects GATE_CLOSED.
        shooter.setSpinning(gamepad1.a);
        shooter.setManualGateForTesting(gamepad1.b && !turretTargetMode);
        shooter.update(System.nanoTime() / 1.0e9);

        telemetry.addData("Selected", "%s (%d/%d)", names[selected], selected + 1, names.length);
        telemetry.addData("Current", "%.4f", value(selected));
        telemetry.addLine("D-pad up/down selects; left/right adjusts. X fine, Y coarse; default step=1.");
        telemetry.addLine("Hold RT + D-pad L/R for small turret steps; RT + D-pad up returns to zero.");
        telemetry.addLine("Hold RT + Circle to flip between wide targets inside both turret limits.");
        telemetry.addLine("LB/RB + right stick Y jog both mirrored ramp servos together.");
        telemetry.addLine("Ramp pair uses complementary targets; D-pad edits stow/level together.");
        telemetry.addLine("Hold LT + right stick Y to jog hood; range is 0 down to 1 up.");
        telemetry.addLine("Press and hold a stick down while moving it to jog slide/turret.");
        telemetry.addLine("Hold bottom Cross/X (FTC A) to spin launcher.");
        telemetry.addLine("Hold Circle/O (FTC B) without RT to test gate open; release to close.");
        telemetry.addLine("Hold R3 + right stick X to jog the target angle; release to hold it.");
        telemetry.addData("Slide encoder", slide.position());
        double turretAngle = turret.degrees();
        double turretTarget = turret.target();
        telemetry.addData("Turret encoder ticks", robot.turret.getCurrentPosition());
        telemetry.addData("Turret angle", "%.2f deg", turretAngle);
        telemetry.addData("Turret target", "%.2f deg", turretTarget);
        telemetry.addData("Turret error", "%.2f deg", turretTarget - turretAngle);
        telemetry.addData("Profile position / speed", "%.2f deg / %.1f deg/s",
                turret.profilePositionDegrees(), turret.profileVelocityDegreesPerSecond());
        double measuredTurretVelocity = turret.measuredVelocityDegreesPerSecond();
        telemetry.addData("Measured turret speed", "%.1f deg/s", measuredTurretVelocity);
        telemetry.addData("Profile velocity error", "%.1f deg/s",
                turret.profileVelocityDegreesPerSecond() - measuredTurretVelocity);
        telemetry.addData("Turret power request / minimum", "%.3f / %.3f",
                turret.lastPowerRequest(), RobotConfig.TURRET_MIN_POWER);
        telemetry.addData("Motion max / accel / brake", "%.0f deg/s, %.0f / %.0f deg/s^2",
                RobotConfig.TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC,
                RobotConfig.TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ,
                RobotConfig.TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ);
        telemetry.addData("Hood position", "%.4f (RobotConfig.HOOD_POSITION = %.4f)",
                hoodPosition, RobotConfig.HOOD_POSITION);
        telemetry.addData("Launcher RPM", "%.0f (ready=%s)", shooter.rpm(), shooter.ready());
        telemetry.addData("Ramp servo positions", "L %.3f / R %.3f", rampLeftPosition, rampRightPosition);
        if (selected == 7 || selected == 8) {
            telemetry.addData("Copy values", "%s", copyValue());
        } else {
            telemetry.addData("Copy value", "RobotConfig.%s = %s;", names[selected], copyValue());
        }
        telemetry.addData("Limelight", robot.limelight == null ? "not configured" : "mapped");
        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAllMotors();
        robot.launcherGate.setPosition(Ramp.clamp01(RobotConfig.GATE_CLOSED));
        if (robot.limelight != null) robot.limelight.stop();
    }

    private void adjust(double delta) {
        switch (selected) {
            case 0: RobotConfig.SLIDE_MAX_TICKS = Math.max(1, RobotConfig.SLIDE_MAX_TICKS + wholeStep(delta)); break;
            case 1: RobotConfig.SLIDE_FLOWER_TICKS = Math.max(0, RobotConfig.SLIDE_FLOWER_TICKS + wholeStep(delta)); break;
            case 2: RobotConfig.SLIDE_KP = Math.max(0.0, RobotConfig.SLIDE_KP + delta * 0.0001); break;
            case 3: RobotConfig.TURRET_TICKS_PER_REVOLUTION = Math.max(1.0, RobotConfig.TURRET_TICKS_PER_REVOLUTION + delta); break;
            case 4: RobotConfig.TURRET_MIN_DEGREES = Math.max(
                    RobotConfig.TURRET_PHYSICAL_MIN_DEGREES + RobotConfig.TURRET_LIMIT_MARGIN_DEGREES,
                    Math.min(RobotConfig.TURRET_MAX_DEGREES - 1.0,
                            RobotConfig.TURRET_MIN_DEGREES + delta)); break;
            case 5: RobotConfig.TURRET_FLOWER_DEGREES += delta; break;
            case 6: RobotConfig.TURRET_KP = Math.max(0.0, RobotConfig.TURRET_KP + delta * 0.0001); break;
            case 7:
                setRampStowPairFromLeft(RobotConfig.RAMP_LEFT_STOW + delta * 0.01);
                break;
            case 8:
                setRampLevelPairFromLeft(RobotConfig.RAMP_LEFT_LEVEL + delta * 0.01);
                break;
            case 9: RobotConfig.GATE_CLOSED = clamp01(RobotConfig.GATE_CLOSED + delta * 0.01); break;
            case 10: RobotConfig.GATE_OPEN = clamp01(RobotConfig.GATE_OPEN + delta * 0.01); break;
            case 11: RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION = Math.max(1.0, RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION + delta); break;
            case 12: RobotConfig.LAUNCHER_TARGET_RPM = Math.max(0.0, RobotConfig.LAUNCHER_TARGET_RPM + delta * 25.0); break;
            case 13: RobotConfig.FEED_OPEN_SECONDS = Math.max(0.01, RobotConfig.FEED_OPEN_SECONDS + delta * 0.01); break;
            case 14: RobotConfig.FEED_GAP_SECONDS = Math.max(0.01, RobotConfig.FEED_GAP_SECONDS + delta * 0.01); break;
            case 15: RobotConfig.TURRET_MAX_POWER = clamp01(RobotConfig.TURRET_MAX_POWER + delta * 0.05); break;
            case 16: RobotConfig.TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC = Math.max(0.0,
                    RobotConfig.TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC + delta * 10.0); break;
            case 17: RobotConfig.TURRET_TOLERANCE_DEGREES = Math.max(0.0,
                    RobotConfig.TURRET_TOLERANCE_DEGREES + delta * 0.1); break;
            case 18: RobotConfig.TURRET_TEST_STEP_DEGREES = Math.max(0.1,
                    RobotConfig.TURRET_TEST_STEP_DEGREES + delta); break;
            case 19: RobotConfig.TURRET_MIN_POWER = Math.min(RobotConfig.TURRET_MAX_POWER,
                    clamp01(RobotConfig.TURRET_MIN_POWER + delta * 0.01)); break;
            case 20: RobotConfig.TURRET_TEST_FLIP_DEGREES = Math.max(0.1,
                    RobotConfig.TURRET_TEST_FLIP_DEGREES + delta * 5.0); break;
            case 21: RobotConfig.TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC = Math.max(1.0,
                    RobotConfig.TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC + delta * 10.0); break;
            case 22: RobotConfig.TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ = Math.max(1.0,
                    RobotConfig.TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ + delta * 10.0); break;
            case 23: RobotConfig.TURRET_MAX_DEGREES = Math.min(
                    RobotConfig.TURRET_PHYSICAL_MAX_DEGREES - RobotConfig.TURRET_LIMIT_MARGIN_DEGREES,
                    Math.max(RobotConfig.TURRET_MIN_DEGREES + 1.0,
                            RobotConfig.TURRET_MAX_DEGREES + delta)); break;
            case 24: RobotConfig.TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ = Math.max(1.0,
                    RobotConfig.TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ + delta * 10.0); break;
            default: break;
        }
    }

    private double value(int index) {
        switch (index) {
            case 0: return RobotConfig.SLIDE_MAX_TICKS;
            case 1: return RobotConfig.SLIDE_FLOWER_TICKS;
            case 2: return RobotConfig.SLIDE_KP;
            case 3: return RobotConfig.TURRET_TICKS_PER_REVOLUTION;
            case 4: return RobotConfig.TURRET_MIN_DEGREES;
            case 5: return RobotConfig.TURRET_FLOWER_DEGREES;
            case 6: return RobotConfig.TURRET_KP;
            case 7: return RobotConfig.RAMP_LEFT_STOW;
            case 8: return RobotConfig.RAMP_LEFT_LEVEL;
            case 9: return RobotConfig.GATE_CLOSED;
            case 10: return RobotConfig.GATE_OPEN;
            case 11: return RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION;
            case 12: return RobotConfig.LAUNCHER_TARGET_RPM;
            case 13: return RobotConfig.FEED_OPEN_SECONDS;
            case 14: return RobotConfig.FEED_GAP_SECONDS;
            case 15: return RobotConfig.TURRET_MAX_POWER;
            case 16: return RobotConfig.TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC;
            case 17: return RobotConfig.TURRET_TOLERANCE_DEGREES;
            case 18: return RobotConfig.TURRET_TEST_STEP_DEGREES;
            case 19: return RobotConfig.TURRET_MIN_POWER;
            case 20: return RobotConfig.TURRET_TEST_FLIP_DEGREES;
            case 21: return RobotConfig.TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC;
            case 22: return RobotConfig.TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ;
            case 23: return RobotConfig.TURRET_MAX_DEGREES;
            case 24: return RobotConfig.TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ;
            default: return 0.0;
        }
    }

    private String copyValue() {
        if (selected == 0 || selected == 1) return Integer.toString((int) value(selected));
        if (selected == 7) {
            return String.format(Locale.US,
                    "RobotConfig.RAMP_LEFT_STOW = %.4f; RobotConfig.RAMP_RIGHT_STOW = %.4f;",
                    RobotConfig.RAMP_LEFT_STOW, RobotConfig.RAMP_RIGHT_STOW);
        }
        if (selected == 8) {
            return String.format(Locale.US,
                    "RobotConfig.RAMP_LEFT_LEVEL = %.4f; RobotConfig.RAMP_RIGHT_LEVEL = %.4f;",
                    RobotConfig.RAMP_LEFT_LEVEL, RobotConfig.RAMP_RIGHT_LEVEL);
        }
        return String.format(Locale.US, "%.4f", value(selected));
    }

    private void setRampStowPairFromLeft(double leftPosition) {
        RobotConfig.RAMP_LEFT_STOW = clamp01(leftPosition);
        RobotConfig.RAMP_RIGHT_STOW = 1.0 - RobotConfig.RAMP_LEFT_STOW;
        rampLeftPosition = RobotConfig.RAMP_LEFT_STOW;
        rampRightPosition = RobotConfig.RAMP_RIGHT_STOW;
    }

    private void setRampLevelPairFromLeft(double leftPosition) {
        RobotConfig.RAMP_LEFT_LEVEL = clamp01(leftPosition);
        RobotConfig.RAMP_RIGHT_LEVEL = 1.0 - RobotConfig.RAMP_LEFT_LEVEL;
        rampLeftPosition = RobotConfig.RAMP_LEFT_LEVEL;
        rampRightPosition = RobotConfig.RAMP_RIGHT_LEVEL;
    }

    private void moveRampPairFromLeft(double leftPosition) {
        rampLeftPosition = clamp01(leftPosition);
        rampRightPosition = 1.0 - rampLeftPosition;
    }

    private void moveRampPairFromRight(double rightPosition) {
        rampRightPosition = clamp01(rightPosition);
        rampLeftPosition = 1.0 - rampRightPosition;
    }

    /** Tick values only move in whole steps, so a fine step still changes them by one. */
    private static int wholeStep(double delta) {
        int rounded = (int) Math.round(delta);
        if (rounded != 0) return rounded;
        return delta > 0.0 ? 1 : -1;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
