package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;

/** Central hardware map and encoder reference setup for the BioBuzz robot. */
public final class RobotHardware {
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
    public final DcMotorEx odoLeft;
    public final DcMotorEx odoRight;
    public final IMU imu;
    public final Limelight3A limelight;

    public RobotHardware(HardwareMap hardwareMap) {
        frontLeft = hardwareMap.get(DcMotorEx.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backLeft = hardwareMap.get(DcMotorEx.class, "backLeft");
        backRight = hardwareMap.get(DcMotorEx.class, "backRight");
        intake = hardwareMap.get(DcMotorEx.class, "intake");
        turret = hardwareMap.get(DcMotorEx.class, "turret");
        backboardSlide = hardwareMap.get(DcMotorEx.class, "backboardSlide");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");
        rampLeft = hardwareMap.get(Servo.class, "rampLeft");
        rampRight = hardwareMap.get(Servo.class, "rampRight");
        launcherGate = hardwareMap.get(Servo.class, "launcherGate");
        odoLeft = hardwareMap.get(DcMotorEx.class, "odoLeft");
        odoRight = hardwareMap.get(DcMotorEx.class, "odoRight");
        imu = hardwareMap.get(IMU.class, "imu");
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
    }

    /**
     * The slide and turret are assumed to be physically at their known home positions at init.
     * Their encoders are zeroed there. Set the IMU mounting orientation to match the robot.
     */
    public void initialize() {
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);
        setDriveMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        resetEncoder(turret);
        resetEncoder(backboardSlide);
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        odoLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        odoRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        stopAllMotors();
        rampLeft.setPosition(RobotConfig.RAMP_LEFT_STOW);
        rampRight.setPosition(RobotConfig.RAMP_RIGHT_STOW);
        launcherGate.setPosition(RobotConfig.GATE_CLOSED);

        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)));
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
        odoLeft.setPower(0.0);
        odoRight.setPower(0.0);
    }

    private void setDriveMode(DcMotor.RunMode mode) {
        frontLeft.setMode(mode);
        frontRight.setMode(mode);
        backLeft.setMode(mode);
        backRight.setMode(mode);
    }

    private static void resetEncoder(DcMotorEx motor) {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
}
