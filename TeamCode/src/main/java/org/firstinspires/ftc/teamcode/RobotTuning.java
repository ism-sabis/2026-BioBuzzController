package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.Locale;

/**
 * Bring-up utility. Use Gamepad 2 D-pad up/down to select a value and left/right to adjust it.
 * X selects fine steps, Y selects coarse steps. Hold Gamepad 1 A to spin up the launcher.
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
    private boolean previousUp;
    private boolean previousDown;
    private boolean previousLeft;
    private boolean previousRight;

    private final String[] names = {
            "SLIDE_MAX_TICKS", "SLIDE_FLOWER_TICKS", "SLIDE_KP",
            "TURRET_TICKS_PER_REVOLUTION", "TURRET_LIMIT_DEGREES", "TURRET_FLOWER_DEGREES",
            "TURRET_KP", "RAMP_LEFT_STOW", "RAMP_LEFT_LEVEL", "RAMP_RIGHT_STOW",
            "RAMP_RIGHT_LEVEL", "GATE_CLOSED", "GATE_OPEN", "LAUNCHER_TICKS_PER_REVOLUTION",
            "LAUNCHER_TARGET_RPM", "FEED_OPEN_SECONDS", "FEED_GAP_SECONDS"
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
        robot.limelight.start();
        rampLeftPosition = RobotConfig.RAMP_LEFT_STOW;
        rampRightPosition = RobotConfig.RAMP_RIGHT_STOW;
        telemetry.addLine("Tuning mode ready. D-pad selects/adjusts; X fine, Y coarse.");
        telemetry.update();
    }

    @Override
    public void loop() {
        robot.clearBulkCache();
        if (gamepad2.dpad_up && !previousUp) selected = (selected + names.length - 1) % names.length;
        if (gamepad2.dpad_down && !previousDown) selected = (selected + 1) % names.length;
        double step = gamepad2.y ? 10.0 : (gamepad2.x ? 0.1 : 1.0);
        if (gamepad2.dpad_right && !previousRight) adjust(step);
        if (gamepad2.dpad_left && !previousLeft) adjust(-step);
        previousUp = gamepad2.dpad_up;
        previousDown = gamepad2.dpad_down;
        previousLeft = gamepad2.dpad_left;
        previousRight = gamepad2.dpad_right;

        if (gamepad2.left_bumper) {
            rampLeftPosition = clamp01(rampLeftPosition + gamepad2.right_stick_y * 0.005);
        }
        if (gamepad2.right_bumper) {
            rampRightPosition = clamp01(rampRightPosition + gamepad2.right_stick_y * 0.005);
        }
        robot.rampLeft.setPosition(rampLeftPosition);
        robot.rampRight.setPosition(rampRightPosition);

        // Same soft limits and power scaling as TeleOp's manual mode.
        if (gamepad2.left_stick_button) {
            slide.manual(MecanumMath.deadband(-gamepad2.left_stick_y, RobotConfig.STICK_DEADBAND));
        } else {
            slide.stop();
        }
        if (gamepad2.right_stick_button) {
            turret.manual(MecanumMath.deadband(gamepad2.right_stick_x, RobotConfig.STICK_DEADBAND));
        } else {
            turret.stop();
        }

        // Same launcher control as TeleOp. The gate opens to GATE_OPEN only at ready speed.
        shooter.setSpinning(gamepad1.a);
        shooter.setManualGate(gamepad2.b);
        shooter.update(System.nanoTime() / 1.0e9);

        telemetry.addData("Selected", "%s (%d/%d)", names[selected], selected + 1, names.length);
        telemetry.addData("Current", "%.4f", value(selected));
        telemetry.addLine("D-pad left/right adjusts; X fine (0.1), Y coarse (10); otherwise step=1.");
        telemetry.addLine("Hold LB/RB + right stick Y to jog left/right ramp servos.");
        telemetry.addLine("Hold Gamepad 2 left/right stick button to enable slide/turret manual control.");
        telemetry.addLine("Hold Gamepad 2 B to test gate open after launcher reaches ready speed.");
        telemetry.addData("Slide encoder", slide.position());
        telemetry.addData("Turret encoder", "%d ticks = %.1f deg",
                robot.turret.getCurrentPosition(), turret.degrees());
        telemetry.addData("Launcher RPM", "%.0f (ready=%s)", shooter.rpm(), shooter.ready());
        telemetry.addData("Ramp servo positions", "L %.3f / R %.3f", rampLeftPosition, rampRightPosition);
        telemetry.addData("Copy value", "RobotConfig.%s = %s;", names[selected], copyValue());
        telemetry.update();
    }

    @Override
    public void stop() {
        robot.stopAllMotors();
        robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
        robot.limelight.stop();
    }

    private void adjust(double delta) {
        switch (selected) {
            case 0: RobotConfig.SLIDE_MAX_TICKS = Math.max(1, RobotConfig.SLIDE_MAX_TICKS + wholeStep(delta)); break;
            case 1: RobotConfig.SLIDE_FLOWER_TICKS = Math.max(0, RobotConfig.SLIDE_FLOWER_TICKS + wholeStep(delta)); break;
            case 2: RobotConfig.SLIDE_KP = Math.max(0.0, RobotConfig.SLIDE_KP + delta * 0.0001); break;
            case 3: RobotConfig.TURRET_TICKS_PER_REVOLUTION = Math.max(1.0, RobotConfig.TURRET_TICKS_PER_REVOLUTION + delta); break;
            case 4: RobotConfig.TURRET_LIMIT_DEGREES = Math.max(1.0, RobotConfig.TURRET_LIMIT_DEGREES + delta); break;
            case 5: RobotConfig.TURRET_FLOWER_DEGREES += delta; break;
            case 6: RobotConfig.TURRET_KP = Math.max(0.0, RobotConfig.TURRET_KP + delta * 0.0001); break;
            case 7: RobotConfig.RAMP_LEFT_STOW = clamp01(RobotConfig.RAMP_LEFT_STOW + delta * 0.01); rampLeftPosition = RobotConfig.RAMP_LEFT_STOW; break;
            case 8: RobotConfig.RAMP_LEFT_LEVEL = clamp01(RobotConfig.RAMP_LEFT_LEVEL + delta * 0.01); rampLeftPosition = RobotConfig.RAMP_LEFT_LEVEL; break;
            case 9: RobotConfig.RAMP_RIGHT_STOW = clamp01(RobotConfig.RAMP_RIGHT_STOW + delta * 0.01); rampRightPosition = RobotConfig.RAMP_RIGHT_STOW; break;
            case 10: RobotConfig.RAMP_RIGHT_LEVEL = clamp01(RobotConfig.RAMP_RIGHT_LEVEL + delta * 0.01); rampRightPosition = RobotConfig.RAMP_RIGHT_LEVEL; break;
            case 11: RobotConfig.GATE_CLOSED = clamp01(RobotConfig.GATE_CLOSED + delta * 0.01); break;
            case 12: RobotConfig.GATE_OPEN = clamp01(RobotConfig.GATE_OPEN + delta * 0.01); break;
            case 13: RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION = Math.max(1.0, RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION + delta); break;
            case 14: RobotConfig.LAUNCHER_TARGET_RPM = Math.max(0.0, RobotConfig.LAUNCHER_TARGET_RPM + delta * 25.0); break;
            case 15: RobotConfig.FEED_OPEN_SECONDS = Math.max(0.01, RobotConfig.FEED_OPEN_SECONDS + delta * 0.01); break;
            case 16: RobotConfig.FEED_GAP_SECONDS = Math.max(0.01, RobotConfig.FEED_GAP_SECONDS + delta * 0.01); break;
            default: break;
        }
    }

    private double value(int index) {
        switch (index) {
            case 0: return RobotConfig.SLIDE_MAX_TICKS;
            case 1: return RobotConfig.SLIDE_FLOWER_TICKS;
            case 2: return RobotConfig.SLIDE_KP;
            case 3: return RobotConfig.TURRET_TICKS_PER_REVOLUTION;
            case 4: return RobotConfig.TURRET_LIMIT_DEGREES;
            case 5: return RobotConfig.TURRET_FLOWER_DEGREES;
            case 6: return RobotConfig.TURRET_KP;
            case 7: return RobotConfig.RAMP_LEFT_STOW;
            case 8: return RobotConfig.RAMP_LEFT_LEVEL;
            case 9: return RobotConfig.RAMP_RIGHT_STOW;
            case 10: return RobotConfig.RAMP_RIGHT_LEVEL;
            case 11: return RobotConfig.GATE_CLOSED;
            case 12: return RobotConfig.GATE_OPEN;
            case 13: return RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION;
            case 14: return RobotConfig.LAUNCHER_TARGET_RPM;
            case 15: return RobotConfig.FEED_OPEN_SECONDS;
            case 16: return RobotConfig.FEED_GAP_SECONDS;
            default: return 0.0;
        }
    }

    private String copyValue() {
        if (selected == 0 || selected == 1) return Integer.toString((int) value(selected));
        return String.format(Locale.US, "%.4f", value(selected));
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
