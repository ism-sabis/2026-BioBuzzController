package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import java.util.List;

/** Non-blocking driver control and mechanism state machines for BioBuzz. */
@TeleOp(name = "BioBuzz TeleOp", group = "BioBuzz")
public class BioBuzzTeleOp extends LinearOpMode {
    private enum Alliance { RED, BLUE }
    private enum FeedState { IDLE, WAIT_READY, OPEN, GAP }

    private RobotHardware robot;
    private Alliance alliance = Alliance.BLUE;
    private boolean fieldCentric = true;
    private boolean flowerMode;
    private boolean rampDeployed;
    private boolean turretReturningHome;
    private boolean launcherHoldEnabled;
    private boolean launcherPowerOn;
    private boolean manualGateOpen;
    private boolean previousDriveY;
    private boolean previousFlowerButton;
    private boolean previousRampButton;
    private boolean previousA;
    private boolean previousB;
    private boolean previousX;
    private boolean previousY;
    private FeedState feedState = FeedState.IDLE;
    private int ballsRemaining;
    private long feedStateStartedNanos;
    private double turretTargetDegrees;
    private double lastSeenTargetX;
    private boolean hasSeenHivTag;
    private String selectedHivTag = "none";

    @Override
    public void runOpMode() {
        robot = new RobotHardware(hardwareMap);
        robot.initialize();
        robot.limelight.pipelineSwitch(RobotConfig.BLUE_LIMELIGHT_PIPELINE);
        robot.limelight.start();

        while (!isStarted() && !isStopRequested()) {
            if (gamepad1.dpad_left) {
                setAlliance(Alliance.RED);
            } else if (gamepad1.dpad_right) {
                setAlliance(Alliance.BLUE);
            }
            telemetry.addLine("Select alliance: Gamepad 1 D-pad left = Red, right = Blue");
            telemetry.addData("Alliance", alliance);
            telemetry.addData("Startup reference", "Slide and turret are assumed to be at home");
            telemetry.update();
            idle();
        }
        if (isStopRequested()) {
            robot.limelight.stop();
            return;
        }

        robot.imu.resetYaw();
        resetRuntime();
        while (opModeIsActive()) {
            updateDrive();
            updateModeButtons();
            updateRampControl();
            updateTurretTarget();
            updateManualControls();
            updateLauncherAndFeed();
            updateMechanismPositioning();
            updateTelemetry();
            idle();
        }
        robot.stopAllMotors();
        robot.launcherGate.setPosition(RobotConfig.GATE_CLOSED);
        robot.limelight.stop();
    }

    private void setAlliance(Alliance nextAlliance) {
        if (alliance == nextAlliance) return;
        alliance = nextAlliance;
        robot.limelight.pipelineSwitch(nextAlliance == Alliance.RED
                ? RobotConfig.RED_LIMELIGHT_PIPELINE : RobotConfig.BLUE_LIMELIGHT_PIPELINE);
    }

    private void updateDrive() {
        if (gamepad1.y && !previousDriveY) fieldCentric = !fieldCentric;
        previousDriveY = gamepad1.y;

        double forward = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_stick_x;
        double turn = gamepad1.right_stick_x;
        if (fieldCentric) {
            double heading = robot.imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            double fieldForward = forward * Math.cos(heading) + strafe * Math.sin(heading);
            double fieldStrafe = -forward * Math.sin(heading) + strafe * Math.cos(heading);
            forward = fieldForward;
            strafe = fieldStrafe;
        }

        double fl = forward + strafe + turn;
        double fr = forward - strafe - turn;
        double bl = forward - strafe + turn;
        double br = forward + strafe - turn;
        double scale = Math.max(1.0, Math.max(Math.max(Math.abs(fl), Math.abs(fr)),
                Math.max(Math.abs(bl), Math.abs(br))));
        robot.frontLeft.setPower(fl / scale);
        robot.frontRight.setPower(fr / scale);
        robot.backLeft.setPower(bl / scale);
        robot.backRight.setPower(br / scale);
    }

    private void updateModeButtons() {
        boolean flowerPressed = gamepad2.left_bumper;
        if (flowerPressed && !previousFlowerButton) {
            if (flowerMode) {
                flowerMode = false;
                turretTargetDegrees = 0.0;
            } else if (flowerUnlocked()) {
                flowerMode = true;
            }
        }
        previousFlowerButton = flowerPressed;

        boolean rampPressed = gamepad2.right_bumper;
        if (rampPressed && !previousRampButton) rampDeployed = !rampDeployed;
        previousRampButton = rampPressed;
        applyRampPosition();
    }

    private boolean flowerUnlocked() {
        return !RobotConfig.FLOWER_LOCKOUT_ENABLED || gamepad1.start
                || RobotConfig.TELEOP_DURATION_SECONDS - getRuntime()
                <= RobotConfig.FLOWER_UNLOCK_REMAINING_SECONDS;
    }

    private void updateRampControl() {
        if (!gamepad2.left_stick_button) return;
        if (gamepad2.dpad_up) rampDeployed = true;
        if (gamepad2.dpad_down) rampDeployed = false;
        applyRampPosition();
    }

    private void applyRampPosition() {
        robot.rampLeft.setPosition(clamp01(rampDeployed
                ? RobotConfig.RAMP_LEFT_LEVEL : RobotConfig.RAMP_LEFT_STOW));
        robot.rampRight.setPosition(clamp01(rampDeployed
                ? RobotConfig.RAMP_RIGHT_LEVEL : RobotConfig.RAMP_RIGHT_STOW));
    }

    private void updateTurretTarget() {
        if (flowerMode) {
            turretTargetDegrees = RobotConfig.TURRET_FLOWER_DEGREES;
            return;
        }
        if (gamepad2.left_stick_button && Math.abs(gamepad2.right_stick_x) > 0.05) return;

        LLResult result = robot.limelight.getLatestResult();
        if (result == null || !result.isValid()) return;
        List<LLResultTypes.FiducialResult> tags = result.getFiducialResults();
        LLResultTypes.FiducialResult nearest = null;
        for (LLResultTypes.FiducialResult tag : tags) {
            // The selected Limelight pipeline should filter detections to HIVE tags.
            if (nearest == null || tag.getTargetArea() > nearest.getTargetArea()) nearest = tag;
        }
        if (nearest == null) return;
        lastSeenTargetX = nearest.getTargetXDegrees();
        selectedHivTag = Integer.toString(nearest.getFiducialId());
        hasSeenHivTag = true;
        double currentDegrees = turretDegrees();
        turretTargetDegrees = currentDegrees + lastSeenTargetX * RobotConfig.LIMELIGHT_AIM_GAIN;
    }

    private void updateManualControls() {
        boolean manualEnabled = gamepad2.left_stick_button;
        boolean slideInput = manualEnabled && Math.abs(gamepad2.left_stick_y) > 0.05;
        boolean turretInput = manualEnabled && Math.abs(gamepad2.right_stick_x) > 0.05;
        boolean gateInput = manualEnabled && (gamepad2.dpad_left || gamepad2.dpad_right);
        boolean rampInput = manualEnabled && (gamepad2.dpad_up || gamepad2.dpad_down);
        boolean manualOverride = slideInput || turretInput || gateInput || rampInput;

        if (manualOverride) cancelFeed();
        if (slideInput) {
            double power = -gamepad2.left_stick_y * RobotConfig.SLIDE_MANUAL_POWER;
            int ticks = robot.backboardSlide.getCurrentPosition();
            if ((ticks <= 0 && power < 0) || (ticks >= RobotConfig.SLIDE_MAX_TICKS && power > 0)) power = 0;
            robot.backboardSlide.setPower(power);
        }
        if (turretInput) {
            double degrees = turretDegrees();
            double power = gamepad2.right_stick_x * RobotConfig.TURRET_MANUAL_POWER;
            if ((degrees <= -RobotConfig.TURRET_LIMIT_DEGREES && power < 0)
                    || (degrees >= RobotConfig.TURRET_LIMIT_DEGREES && power > 0)) power = 0;
            robot.turret.setPower(power);
        }
        if (gateInput) {
            if (gamepad2.dpad_left) manualGateOpen = false;
            if (gamepad2.dpad_right && launcherReady()) manualGateOpen = true;
        }
        if (rampInput) {
            if (gamepad2.dpad_up) rampDeployed = true;
            if (gamepad2.dpad_down) rampDeployed = false;
            applyRampPosition();
        }
        if (manualGateOpen && launcherReady() && feedState == FeedState.IDLE) {
            robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_OPEN));
        } else if (feedState == FeedState.IDLE) {
            robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
        }
        if (!slideInput) robot.backboardSlide.setPower(0.0);
        if (!turretInput) robot.turret.setPower(0.0);
    }

    private void updateLauncherAndFeed() {
        if (gamepad2.back) {
            cancelFeed();
            manualGateOpen = false;
            launcherHoldEnabled = false;
            launcherPowerOn = false;
        }

        if (pressed(gamepad2.a, previousA)) startFeed(1);
        if (pressed(gamepad2.b, previousB)) startFeed(2);
        if (pressed(gamepad2.x, previousX)) startFeed(3);
        if (pressed(gamepad2.y, previousY)) startFeed(4);
        previousA = gamepad2.a;
        previousB = gamepad2.b;
        previousX = gamepad2.x;
        previousY = gamepad2.y;

        double rpm = launcherRpm();
        if (launcherHoldEnabled) {
            if (RobotConfig.LAUNCHER_CONTINUOUS_VELOCITY_HOLD) {
                double error = RobotConfig.LAUNCHER_TARGET_RPM - rpm;
                double power = RobotConfig.LAUNCHER_POWER + error * RobotConfig.LAUNCHER_CONTINUOUS_KP;
                robot.launcher.setPower(Math.max(0.0, Math.min(1.0, power)));
                launcherPowerOn = power > 0.02;
            } else {
                if (!launcherPowerOn && rpm <= RobotConfig.LAUNCHER_TARGET_RPM
                        * RobotConfig.LAUNCHER_RESTART_RPM_FRACTION) launcherPowerOn = true;
                if (launcherPowerOn && rpm >= RobotConfig.LAUNCHER_TARGET_RPM
                        * RobotConfig.LAUNCHER_CUTOFF_RPM_FRACTION) launcherPowerOn = false;
                robot.launcher.setPower(launcherPowerOn ? RobotConfig.LAUNCHER_POWER : 0.0);
            }
        } else {
            robot.launcher.setPower(0.0);
            launcherPowerOn = false;
        }

        boolean ready = launcherReady();
        long now = System.nanoTime();
        if (feedState == FeedState.IDLE && ballsRemaining > 0) feedState = FeedState.WAIT_READY;
        if (feedState == FeedState.WAIT_READY && ready) beginFeedPulse(now);
        if (feedState == FeedState.OPEN && !ready) {
            closeGateAndIntake();
            feedState = FeedState.WAIT_READY;
        } else if (feedState == FeedState.OPEN
                && elapsedSeconds(now, feedStateStartedNanos) >= RobotConfig.FEED_OPEN_SECONDS) {
            closeGateAndIntake();
            ballsRemaining--;
            feedState = ballsRemaining > 0 ? FeedState.GAP : FeedState.IDLE;
            feedStateStartedNanos = now;
        } else if (feedState == FeedState.GAP
                && elapsedSeconds(now, feedStateStartedNanos) >= RobotConfig.FEED_GAP_SECONDS) {
            feedState = FeedState.WAIT_READY;
        }

        if (feedState == FeedState.OPEN && ready) {
            robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_OPEN));
            robot.intake.setPower(RobotConfig.INTAKE_POWER);
        } else if (feedState != FeedState.IDLE) {
            robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
            robot.intake.setPower(0.0);
        }
    }

    private void startFeed(int count) {
        if (gamepad2.back) return;
        ballsRemaining = count;
        feedState = FeedState.WAIT_READY;
        launcherHoldEnabled = true;
        manualGateOpen = false;
        robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
        robot.intake.setPower(0.0);
    }

    private void beginFeedPulse(long now) {
        feedState = FeedState.OPEN;
        feedStateStartedNanos = now;
        robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_OPEN));
        robot.intake.setPower(RobotConfig.INTAKE_POWER);
    }

    private void cancelFeed() {
        ballsRemaining = 0;
        feedState = FeedState.IDLE;
        robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
        robot.intake.setPower(0.0);
    }

    private void closeGateAndIntake() {
        robot.launcherGate.setPosition(clamp01(RobotConfig.GATE_CLOSED));
        robot.intake.setPower(0.0);
    }

    private void updateMechanismPositioning() {
        if (gamepad2.left_stick_button
                && (Math.abs(gamepad2.left_stick_y) > 0.05 || Math.abs(gamepad2.right_stick_x) > 0.05)) return;

        int slidePosition = robot.backboardSlide.getCurrentPosition();
        int slideTarget = flowerMode ? RobotConfig.SLIDE_FLOWER_TICKS : 0;
        slideTarget = Math.max(0, Math.min(RobotConfig.SLIDE_MAX_TICKS, slideTarget));
        double slideError = slideTarget - slidePosition;
        if (Math.abs(slideError) <= 8.0) {
            robot.backboardSlide.setPower(0.0);
        } else {
            robot.backboardSlide.setPower(Math.max(-RobotConfig.SLIDE_MAX_POWER,
                    Math.min(RobotConfig.SLIDE_MAX_POWER, slideError * RobotConfig.SLIDE_KP)));
        }

        double currentTurret = turretDegrees();
        if (!turretReturningHome && Math.abs(currentTurret) >= RobotConfig.TURRET_LIMIT_DEGREES) {
            turretReturningHome = true;
        }
        if (turretReturningHome) {
            turretTargetDegrees = 0.0;
            if (Math.abs(currentTurret) <= RobotConfig.TURRET_HOME_TOLERANCE_DEGREES) {
                turretReturningHome = false;
            } else {
                robot.turret.setPower(currentTurret > 0
                        ? -RobotConfig.TURRET_RETURN_POWER : RobotConfig.TURRET_RETURN_POWER);
                return;
            }
        }
        double boundedTarget = Math.max(-RobotConfig.TURRET_LIMIT_DEGREES,
                Math.min(RobotConfig.TURRET_LIMIT_DEGREES, turretTargetDegrees));
        double turretError = boundedTarget - currentTurret;
        robot.turret.setPower(Math.max(-RobotConfig.TURRET_MAX_POWER,
                Math.min(RobotConfig.TURRET_MAX_POWER, turretError * RobotConfig.TURRET_KP)));
    }

    private boolean launcherReady() {
        return launcherRpm() >= RobotConfig.LAUNCHER_TARGET_RPM
                * RobotConfig.LAUNCHER_READY_RPM_FRACTION;
    }

    private double launcherRpm() {
        if (RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION <= 0.0) return 0.0;
        return Math.abs(robot.launcher.getVelocity()) * 60.0
                / RobotConfig.LAUNCHER_TICKS_PER_REVOLUTION;
    }

    private double turretDegrees() {
        if (RobotConfig.TURRET_TICKS_PER_REVOLUTION <= 0.0) return 0.0;
        return robot.turret.getCurrentPosition() * 360.0
                / RobotConfig.TURRET_TICKS_PER_REVOLUTION;
    }

    private void updateTelemetry() {
        double remaining = Math.max(0.0, RobotConfig.TELEOP_DURATION_SECONDS - getRuntime());
        telemetry.addData("Alliance / drive", "%s / %s-centric", alliance,
                fieldCentric ? "field" : "robot");
        telemetry.addData("Flower", "%s (%s)", flowerMode ? "ON" : "OFF",
                flowerUnlocked() ? "unlocked" : "locked; hold Gamepad 1 Start to override");
        telemetry.addData("Match time remaining", "%.1f s", remaining);
        telemetry.addData("Slide", "%d / %d ticks", robot.backboardSlide.getCurrentPosition(),
                RobotConfig.SLIDE_MAX_TICKS);
        telemetry.addData("Turret", "%.1f / %.1f deg target=%.1f%s", turretDegrees(),
                RobotConfig.TURRET_LIMIT_DEGREES, turretTargetDegrees,
                turretReturningHome ? " RETURNING HOME" : "");
        telemetry.addData("HIVE tag", hasSeenHivTag ? selectedHivTag + " tx="
                + String.format("%.1f", lastSeenTargetX) : "not seen; holding last target");
        telemetry.addData("Ramp / gate", "%s / %s", rampDeployed ? "level" : "stowed",
                launcherReady() ? (feedState == FeedState.OPEN || manualGateOpen ? "open" : "closed")
                        : "closed (launcher not ready)");
        telemetry.addData("Launcher", "%.0f / %.0f RPM, ready=%s, hold=%s", launcherRpm(),
                RobotConfig.LAUNCHER_TARGET_RPM, launcherReady(), launcherHoldEnabled);
        telemetry.addData("Feed", "%s, %d balls queued", feedState, ballsRemaining);
        telemetry.update();
    }

    private boolean pressed(boolean current, boolean previous) {
        return current && !previous;
    }

    private static double elapsedSeconds(long now, long start) {
        return (now - start) / 1_000_000_000.0;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
