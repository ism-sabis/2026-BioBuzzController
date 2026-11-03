package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.Locale;

/**
 * Bring-up utility. Use D-pad up/down to select a value and left/right to adjust it.
 * X selects fine steps, Y selects coarse steps. Hold Gamepad 1 A to spin up the launcher.
 * Adjusted values are live for the current controller session; copy the reported Java values into
 * RobotConfig so they survive an app restart. Keep the robot clear when enabling manual outputs.
 */
@TeleOp(name = "BioBuzz Tuning", group = "BioBuzz")
public class RobotTuning extends OpMode {
    private RobotHardware robot;
    private int selected;
    private double rampLeftPosition;
    private double rampRightPosition;
    private double gatePosition;
    private boolean previousUp;
    private boolean previousDown;
    private boolean previousLeft;
    private boolean previousRight;
    private boolean launcherPowerOn;

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
        robot.limelight.start();
        rampLeftPosition = RobotConfig.RAMP_LEFT_STOW;
        rampRightPosition = RobotConfig.RAMP_RIGHT_STOW;
        gatePosition = RobotConfig.GATE_CLOSED;
        telemetry.addLine("Tuning mode ready. D-pad selects/adjusts; X fine, Y coarse.");
        telemetry.update();
    }

    @Override
    public void loop() {
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
            robot.rampLeft.setPosition(rampLeftPosition);
        }
        if (gamepad2.right_bumper) {
            rampRightPosition = clamp01(rampRightPosition + gamepad2.right_stick_y * 0.005);
        }
        if (gamepad2.left_stick_button) {
            double p = -gamepad2.left_stick_y * RobotConfig.SLIDE_MANUAL_POWER;
            int pos = robot.backboardSlide.getCurrentPosition();
            if ((pos <= 0 && p < 0) || (pos >= RobotConfig.SLIDE_MAX_TICKS && p > 0)) p = 0;
            robot.backboardSlide.setPower(p);
        } else robot.backboardSlide.setPower(0.0);
        if (gamepad2.right_stick_button) {
            double turretDegrees = RobotConfig.TURRET_TICKS_PER_REVOLUTION <= 0.0 ? 0.0
                    : robot.turret.getCurrentPosition() * 360.0 / RobotConfig.TURRET_TICKS_PER_REVOLUTION;
            double power = gamepad2.right_stick_x * RobotConfig.TURRET_MANUAL_POWER;
            if ((turretDegrees <= -RobotConfig.TURRET_LIMIT_DEGREES && power < 0)
                    || (turretDegrees >= RobotConfig.TURRET_LIMIT_DEGREES && power > 0)) power = 0;
            robot.turret.setPower(power);
        } else robot.turret.setPower(0.0);
        if (gamepad1.a) {
            double rpm = launcherRpm();
            if (RobotConfig.LAUNCHER_CONTINUOUS_VELOCITY_HOLD) {
                double error = RobotConfig.LAUNCHER_TARGET_RPM - rpm;
                robot.launcher.setPower(Math.max(0.0, Math.min(1.0,
                        RobotConfig.LAUNCHER_POWER + error * RobotConfig.LAUNCHER_CONTINUOUS_KP)));
            } else {
                if (!launcherPowerOn && rpm <= RobotConfig.LAUNCHER_TARGET_RPM
                        * RobotConfig.LAUNCHER_RESTART_RPM_FRACTION) launcherPowerOn = true;
                if (launcherPowerOn && rpm >= RobotConfig.LAUNCHER_TARGET_RPM
                        * RobotConfig.LAUNCHER_CUTOFF_RPM_FRACTION) launcherPowerOn = false;
                robot.launcher.setPower(launcherPowerOn ? RobotConfig.LAUNCHER_POWER : 0.0);
            }
        } else {
            launcherPowerOn = false;
            robot.launcher.setPower(0.0);
        }
        robot.intake.setPower(0.0);
        robot.rampLeft.setPosition(clamp01(rampLeftPosition));
        robot.rampRight.setPosition(clamp01(rampRightPosition));
        double readyRpm = RobotConfig.LAUNCHER_TARGET_RPM * RobotConfig.LAUNCHER_READY_RPM_FRACTION;
        robot.launcherGate.setPosition(gamepad2.b && launcherRpm() >= readyRpm
                ? clamp01(gatePosition) : clamp01(RobotConfig.GATE_CLOSED));

        telemetry.addData("Selected", "%s (%d/%d)", names[selected], selected + 1, names.length);
        telemetry.addData("Current", "%.4f", value(selected));
        telemetry.addLine("D-pad left/right adjusts; X fine (0.1), Y coarse (10); otherwise step=1.");
        telemetry.addLine("Hold LB/RB + right stick Y to jog left/right ramp servos.");
        telemetry.addLine("Hold Gamepad 2 left/right stick button to enable slide/turret manual control.");
        telemetry.addLine("Hold Gamepad 2 B to test gate open after launcher reaches ready speed.");
        telemetry.addData("Slide encoder", robot.backboardSlide.getCurrentPosition());
        telemetry.addData("Turret encoder", robot.turret.getCurrentPosition());
        telemetry.addData("Launcher RPM", "%.0f", launcherRpm());
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
            case 0: RobotConfig.SLIDE_MAX_TICKS = Math.max(1, RobotConfig.SLIDE_MAX_TICKS + (int) delta); break;
            case 1: RobotConfig.SLIDE_FLOWER_TICKS = Math.max(0, RobotConfig.SLIDE_FLOWER_TICKS + (int) delta); break;
            case 2: RobotConfig.SLIDE_KP = Math.max(0.0, RobotConfig.SLIDE_KP + delta * 0.0001); break;
            case 3: RobotConfig.TURRET_TICKS_PER_REVOLUTION = Math.max(1.0, RobotConfig.TURRET_TICKS_PER_REVOLUTION + delta); break;
            case 4: RobotConfig.TURRET_LIMIT_DEGREES = Math.max(1.0, RobotConfig.TURRET_LIMIT_DEGREES + delta); break;
            case 5: RobotConfig.TURRET_FLOWER_DEGREES += delta; break;
            case 6: RobotConfig.TURRET_KP = Math.max(0.0, RobotConfig.TURRET_KP + delta * 0.0001); break;
            case 7: RobotConfig.RAMP_LEFT_STOW = clamp01(RobotConfig.RAMP_LEFT_STOW + delta * 0.01); rampLeftPosition = RobotConfig.RAMP_LEFT_STOW; break;
            case 8: RobotConfig.RAMP_LEFT_LEVEL = clamp01(RobotConfig.RAMP_LEFT_LEVEL + delta * 0.01); rampLeftPosition = RobotConfig.RAMP_LEFT_LEVEL; break;
            case 9: RobotConfig.RAMP_RIGHT_STOW = clamp01(RobotConfig.RAMP_RIGHT_STOW + delta * 0.01); rampRightPosition = RobotConfig.RAMP_RIGHT_STOW; break;
            case 10: RobotConfig.RAMP_RIGHT_LEVEL = clamp01(RobotConfig.RAMP_RIGHT_LEVEL + delta * 0.01); rampRightPosition = RobotConfig.RAMP_RIGHT_LEVEL; break;
            case 11: RobotConfig.GATE_CLOSED = clamp01(RobotConfig.GATE_CLOSED + delta * 0.01); gatePosition = RobotConfig.GATE_CLOSED; break;
            case 12: RobotConfig.GATE_OPEN = clamp01(RobotConfig.GATE_OPEN + delta * 0.01); gatePosition = RobotConfig.GATE_OPEN; break;
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

    private double launcherRpm() {
        return RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION <= 0.0 ? 0.0
                : Math.abs(robot.launcher.getVelocity()) * 60.0 / RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
