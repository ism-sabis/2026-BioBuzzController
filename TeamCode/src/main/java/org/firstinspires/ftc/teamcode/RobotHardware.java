package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Central hardware map and encoder reference setup for the BioBuzz robot. */
public final class RobotHardware {
    /** Motors whose encoder port is free for an odometry pod (they run without an encoder). */
    private static final List<String> POD_PORT_CHOICES = Arrays.asList(
            "frontLeft", "frontRight", "backLeft", "backRight", "intake");

    /**
     * Static, so it survives from one OpMode to the next (for example autonomous to TeleOp)
     * and only goes back to false when the Robot Controller app restarts.
     */
    private static boolean mechanismsZeroed;

    public final DcMotorEx frontLeft;
    public final DcMotorEx frontRight;
    public final DcMotorEx backLeft;
    public final DcMotorEx backRight;
    public final DcMotorEx intake;
    public final DcMotorEx turret;
    public final DcMotorEx backboardSlide;
    public final DcMotorEx launcher;
    public final Servo rampLeft;
    public final Servo rampRight;
    public final Servo launcherGate;
    public final Servo hood;
    public final DigitalChannel odometryLeft;
    public final DigitalChannel odometryRight;
    public final IMU imu;
    public final Limelight3A limelight;

    private final List<LynxModule> hubs;
    private final List<String> missing = new ArrayList<>();
    /** Null until RobotConfig names the motor port each pod is plugged into. */
    private final DcMotorEx forwardPod;
    private final DcMotorEx strafePod;
    private boolean zeroedThisInit;

    public RobotHardware(HardwareMap hardwareMap) {
        frontLeft = find(hardwareMap, DcMotorEx.class, "frontLeft");
        frontRight = find(hardwareMap, DcMotorEx.class, "frontRight");
        backLeft = find(hardwareMap, DcMotorEx.class, "backLeft");
        backRight = find(hardwareMap, DcMotorEx.class, "backRight");
        intake = find(hardwareMap, DcMotorEx.class, "intake");
        turret = find(hardwareMap, DcMotorEx.class, "turret");
        backboardSlide = find(hardwareMap, DcMotorEx.class, "backboardSlide");
        launcher = find(hardwareMap, DcMotorEx.class, "launcher");
        rampLeft = find(hardwareMap, Servo.class, "rampLeft");
        rampRight = find(hardwareMap, Servo.class, "rampRight");
        launcherGate = find(hardwareMap, Servo.class, "launcherGate");
        hood = find(hardwareMap, Servo.class, "hood");
        odometryLeft = find(hardwareMap, DigitalChannel.class, "odometryLeft");
        odometryRight = find(hardwareMap, DigitalChannel.class, "odometryRight");
        imu = find(hardwareMap, IMU.class, "imu");
        // The camera is optional during bring-up; other mechanisms remain testable without it.
        limelight = hardwareMap.tryGet(Limelight3A.class, "limelight");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Robot Configuration is missing (or has the wrong "
                    + "type for): " + missing + ". Names must match exactly.");
        }
        forwardPod = podPort(hardwareMap, RobotConfig.FORWARD_POD_MOTOR_NAME, "FORWARD");
        strafePod = podPort(hardwareMap, RobotConfig.STRAFE_POD_MOTOR_NAME, "STRAFE");
        hubs = hardwareMap.getAll(LynxModule.class);
    }

    private <T> T find(HardwareMap hardwareMap, Class<T> type, String name) {
        T device = hardwareMap.tryGet(type, name);
        if (device == null) missing.add(name);
        return device;
    }

    private static DcMotorEx podPort(HardwareMap hardwareMap, String motorName, String pod) {
        if (motorName == null || motorName.isEmpty()) return null;
        if (!POD_PORT_CHOICES.contains(motorName)) {
            throw new IllegalStateException("RobotConfig." + pod + "_POD_MOTOR_NAME is \""
                    + motorName + "\" but must be one of " + POD_PORT_CHOICES
                    + ": the turret, slide and launcher ports carry their own encoders.");
        }
        return hardwareMap.get(DcMotorEx.class, motorName);
    }

    /**
     * Sets directions, run modes and startup servo positions. The slide and turret encoders are
     * zeroed the first time this runs after the app starts, so they must be at home then. Later
     * OpModes keep that zero; use {@link #rezeroMechanisms()} to set it again.
     */
    public void initialize() {
        // One bulk read per hub per loop instead of one hub call per encoder read.
        for (LynxModule hub : hubs) hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

        // Motor Test showed BL was already correct; FL, FR and BR spun backward when
        // commanded to roll forward, so reverse those three mounting directions.
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);
        // The raised elevator moves downward under positive power with the default direction.
        // Reverse it so positive joystick input and encoder ticks mean upward extension.
        backboardSlide.setDirection(DcMotor.Direction.REVERSE);
        for (DcMotorEx motor : new DcMotorEx[] {frontLeft, frontRight, backLeft, backRight}) {
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        zeroedThisInit = !mechanismsZeroed;
        if (zeroedThisInit) {
            rezeroMechanisms();
        } else {
            turret.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            backboardSlide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        turret.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backboardSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        launcher.setDirection(DcMotor.Direction.REVERSE);
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        // The flywheel must coast when power is cut; braking it wastes the spin-up.
        launcher.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        if (RobotConfig.LAUNCHER_CUSTOM_PIDF) {
            launcher.setVelocityPIDFCoefficients(RobotConfig.LAUNCHER_P, RobotConfig.LAUNCHER_I,
                    RobotConfig.LAUNCHER_D, RobotConfig.LAUNCHER_F);
        }

        stopAllMotors();
        rampLeft.setPosition(RobotConfig.RAMP_LEFT_STOW);
        rampRight.setPosition(RobotConfig.RAMP_RIGHT_STOW);
        // Start open during INIT; Shooter keeps it open through the active start until a gate
        // command or feed request takes control.
        launcherGate.setPosition(Ramp.clamp01(RobotConfig.GATE_OPEN));
        if (RobotConfig.HOOD_POSITION >= 0.0 && RobotConfig.HOOD_POSITION <= 1.0) {
            hood.setPosition(RobotConfig.HOOD_POSITION);
        }
        odometryLeft.setMode(DigitalChannel.Mode.INPUT);
        odometryRight.setMode(DigitalChannel.Mode.INPUT);

        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
    }

    /** Makes the current slide and turret positions the new zero. They must be at home. */
    public void rezeroMechanisms() {
        resetEncoder(turret);
        resetEncoder(backboardSlide);
        mechanismsZeroed = true;
        zeroedThisInit = true;
        clearBulkCache();
    }

    /** True if the slide and turret were zeroed during this OpMode, false if the zero is older. */
    public boolean zeroedThisInit() {
        return zeroedThisInit;
    }

    /** Call once at the top of every loop so each encoder is read fresh exactly once. */
    public void clearBulkCache() {
        for (LynxModule hub : hubs) hub.clearBulkCache();
    }

    public boolean hasOdometry() {
        return forwardPod != null && strafePod != null;
    }

    public int forwardPodTicks() {
        return forwardPod == null ? 0 : forwardPod.getCurrentPosition();
    }

    public int strafePodTicks() {
        return strafePod == null ? 0 : strafePod.getCurrentPosition();
    }

    public void stopAllMotors() {
        frontLeft.setPower(0.0);
        frontRight.setPower(0.0);
        backLeft.setPower(0.0);
        backRight.setPower(0.0);
        intake.setPower(0.0);
        turret.setPower(0.0);
        backboardSlide.setPower(0.0);
        launcher.setPower(0.0);
    }

    private static void resetEncoder(DcMotorEx motor) {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}
