package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Driver control for BioBuzz. This class only maps gamepad inputs onto the mechanism classes
 * (Drive, Slide, Turret, Shooter, Ramp); nothing here blocks or waits.
 */
@TeleOp(name = "BioBuzz TeleOp", group = "BioBuzz")
public class BioBuzzTeleOp extends LinearOpMode {
    private enum Alliance { RED, BLUE }

    private RobotHardware robot;
    private Drive drive;
    private Slide slide;
    private Turret turret;
    private Shooter shooter;
    private Ramp ramp;

    private Alliance alliance = Alliance.BLUE;
    private boolean flowerMode;
    private boolean previousDriveY;
    private boolean previousDriveBack;
    private boolean previousInitX;
    private boolean previousFlowerButton;
    private boolean previousRampButton;
    private boolean previousA;
    private boolean previousB;
    private boolean previousX;
    private boolean previousY;
    private long previousLoopNanos;
    private double loopMillis;

    @Override
    public void runOpMode() {
        robot = new RobotHardware(hardwareMap);
        robot.initialize();
        drive = new Drive(robot);
        slide = new Slide(robot);
        turret = new Turret(robot);
        shooter = new Shooter(robot);
        ramp = new Ramp(robot);
        if (robot.limelight != null) {
            robot.limelight.pipelineSwitch(RobotConfig.BLUE_LIMELIGHT_PIPELINE);
            robot.limelight.start();
        }

        while (!isStarted() && !isStopRequested()) {
            robot.clearBulkCache();
            if (gamepad1.dpad_left) {
                setAlliance(Alliance.RED);
            } else if (gamepad1.dpad_right) {
                setAlliance(Alliance.BLUE);
            }
            if (gamepad1.x && !previousInitX) {
                robot.rezeroMechanisms();
                slide.setTarget(0);
                turret.setTarget(0.0);
            }
            previousInitX = gamepad1.x;

            telemetry.addLine("Gamepad 1 D-pad left = Red, right = Blue");
            telemetry.addData("Alliance", alliance);
            telemetry.addData("Goal tags", "%d-%d", minTagId(), maxTagId());
            telemetry.addData("Slide / turret zero", robot.zeroedThisInit()
                    ? "set during this init" : "kept from an earlier OpMode");
            telemetry.addData("Slide / turret now", "%d ticks / %.1f deg", slide.position(),
                    turret.degrees());
            telemetry.addLine("If they are at home but do not read 0, press Gamepad 1 X.");
            telemetry.addData("Odometry pods", robot.hasOdometry()
                    ? "configured" : "not configured (ports not set)");
            telemetry.update();
            idle();
        }
        if (isStopRequested()) {
            if (robot.limelight != null) robot.limelight.stop();
            return;
        }

        drive.resetHeading();
        resetRuntime();
        previousLoopNanos = System.nanoTime();
        while (opModeIsActive()) {
            robot.clearBulkCache();
            long now = System.nanoTime();
            loopMillis = (now - previousLoopNanos) / 1.0e6;
            previousLoopNanos = now;

            updateDrive();
            updateModeButtons();
            updateMechanisms();
            updateShooter(now / 1.0e9);
            updateTelemetry();
            idle();
        }
        robot.stopAllMotors();
        robot.launcherGate.setPosition(Ramp.clamp01(RobotConfig.GATE_CLOSED));
        if (robot.limelight != null) robot.limelight.stop();
    }

    private void setAlliance(Alliance nextAlliance) {
        if (alliance == nextAlliance) return;
        alliance = nextAlliance;
        if (robot.limelight != null) {
            robot.limelight.pipelineSwitch(nextAlliance == Alliance.RED
                    ? RobotConfig.RED_LIMELIGHT_PIPELINE : RobotConfig.BLUE_LIMELIGHT_PIPELINE);
        }
    }

    private int minTagId() {
        return alliance == Alliance.RED ? RobotConfig.RED_TAG_MIN_ID : RobotConfig.BLUE_TAG_MIN_ID;
    }

    private int maxTagId() {
        return alliance == Alliance.RED ? RobotConfig.RED_TAG_MAX_ID : RobotConfig.BLUE_TAG_MAX_ID;
    }

    private void updateDrive() {
        if (gamepad1.y && !previousDriveY) drive.toggleFieldCentric();
        previousDriveY = gamepad1.y;
        if (gamepad1.back && !previousDriveBack) drive.resetHeading();
        previousDriveBack = gamepad1.back;

        double scale = gamepad1.right_bumper ? RobotConfig.DRIVE_SLOW_SCALE : 1.0;
        drive.drive(-gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x, scale);
    }

    private void updateModeButtons() {
        boolean flowerPressed = gamepad2.left_bumper;
        if (flowerPressed && !previousFlowerButton) {
            if (flowerMode) {
                flowerMode = false;
                slide.setTarget(0);
                turret.setTarget(0.0);
            } else if (flowerUnlocked()) {
                flowerMode = true;
                slide.setTarget(RobotConfig.SLIDE_FLOWER_TICKS);
                turret.setTarget(RobotConfig.TURRET_FLOWER_DEGREES);
            }
        }
        previousFlowerButton = flowerPressed;

        boolean rampPressed = gamepad2.right_bumper;
        if (rampPressed && !previousRampButton) ramp.toggle();
        previousRampButton = rampPressed;
    }

    private boolean flowerUnlocked() {
        return !RobotConfig.FLOWER_LOCKOUT_ENABLED || gamepad1.start
                || RobotConfig.TELEOP_DURATION_SECONDS - getRuntime()
                <= RobotConfig.FLOWER_UNLOCK_REMAINING_SECONDS;
    }

    /** Manual overrides (hold Gamepad 2 left stick button), then slide and turret positioning. */
    private void updateMechanisms() {
        boolean manualEnabled = gamepad2.left_stick_button;
        double slideStick = manualEnabled
                ? MecanumMath.deadband(-gamepad2.left_stick_y, RobotConfig.STICK_DEADBAND) : 0.0;
        double turretStick = manualEnabled
                ? MecanumMath.deadband(gamepad2.right_stick_x, RobotConfig.STICK_DEADBAND) : 0.0;
        boolean gateInput = manualEnabled && (gamepad2.dpad_left || gamepad2.dpad_right);
        boolean rampInput = manualEnabled && (gamepad2.dpad_up || gamepad2.dpad_down);

        if (slideStick != 0.0 || turretStick != 0.0 || gateInput || rampInput) {
            shooter.cancelFeed();
        }
        if (rampInput) ramp.set(gamepad2.dpad_up);
        if (gateInput) {
            if (gamepad2.dpad_left) {
                shooter.setManualGate(false);
            } else if (shooter.ready()) {
                shooter.setManualGate(true);
            }
        }
        ramp.apply();

        if (slideStick != 0.0) {
            slide.manual(slideStick);
        } else {
            slide.update();
        }

        if (turretStick != 0.0) {
            turret.manual(turretStick);
        } else {
            // Flower mode owns the turret target; otherwise the Limelight does.
            if (!flowerMode) turret.trackTag(minTagId(), maxTagId());
            turret.update();
        }
    }

    private void updateShooter(double nowSeconds) {
        if (gamepad2.back) {
            shooter.shutDown();
        } else if (!gamepad2.start) {
            // Start+A and Start+B pair a gamepad, so they must not also start a feed.
            if (gamepad2.a && !previousA) shooter.startFeed(1);
            if (gamepad2.b && !previousB) shooter.startFeed(2);
            if (gamepad2.x && !previousX) shooter.startFeed(3);
            if (gamepad2.y && !previousY) shooter.startFeed(4);
        }
        previousA = gamepad2.a;
        previousB = gamepad2.b;
        previousX = gamepad2.x;
        previousY = gamepad2.y;
        shooter.update(nowSeconds);
    }

    private void updateTelemetry() {
        double remaining = Math.max(0.0, RobotConfig.TELEOP_DURATION_SECONDS - getRuntime());
        telemetry.addData("Alliance / drive", "%s / %s-centric", alliance,
                drive.isFieldCentric() ? "field" : "robot");
        telemetry.addData("Flower", "%s (%s)", flowerMode ? "ON" : "OFF",
                flowerUnlocked() ? "unlocked" : "locked; hold Gamepad 1 Start to override");
        telemetry.addData("Match time remaining", "%.1f s", remaining);
        telemetry.addData("Slide", "%d ticks, target %d, max %d", slide.position(),
                slide.target(), RobotConfig.SLIDE_MAX_TICKS);
        telemetry.addData("Turret", "%.1f deg, target %.1f, limit %.1f", turret.degrees(),
                turret.target(), RobotConfig.TURRET_LIMIT_DEGREES);
        telemetry.addData("Goal tag", "IDs %d-%d, %s", minTagId(), maxTagId(),
                robot.limelight == null ? "Limelight not configured"
                        : turret.tagVisible()
                        ? String.format("tracking %d tx=%.1f", turret.lastTagId(), turret.lastTx())
                        : "not in view; holding last target");
        telemetry.addData("Ramp / gate", "%s / %s", ramp.isDeployed() ? "level" : "stowed",
                shooter.gateOpen() ? "open" : "closed");
        telemetry.addData("Launcher", "%.0f / %.0f RPM, ready=%s, spinning=%s", shooter.rpm(),
                RobotConfig.LAUNCHER_TARGET_RPM, shooter.ready(), shooter.isSpinning());
        telemetry.addData("Feed", "%s, %d balls queued", shooter.feedState(),
                shooter.ballsRemaining());
        telemetry.addData("Loop time", "%.0f ms", loopMillis);
        telemetry.update();
    }
}
