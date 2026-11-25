package org.firstinspires.ftc.teamcode;

import com.pedropathing.ftc.localization.localizers.DriveEncoderLocalizer;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * Low-power, manual check for the PedroPathing drive-encoder baseline. It does not follow paths.
 * The pose uses tick-equivalent units until the foam-tile scale measurements are recorded.
 */
@TeleOp(name = "Pedro Localization Check", group = "BioBuzz Test")
public final class PedroLocalizationCheck extends LinearOpMode {
    private DcMotorEx frontLeft;
    private DcMotorEx frontRight;
    private DcMotorEx backLeft;
    private DcMotorEx backRight;

    @Override
    public void runOpMode() throws InterruptedException {
        frontLeft = hardwareMap.get(DcMotorEx.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotorEx.class, "frontRight");
        backLeft = hardwareMap.get(DcMotorEx.class, "backLeft");
        backRight = hardwareMap.get(DcMotorEx.class, "backRight");
        configureDriveMotor(frontLeft, DcMotor.Direction.FORWARD);
        configureDriveMotor(frontRight, DcMotor.Direction.REVERSE);
        configureDriveMotor(backLeft, DcMotor.Direction.REVERSE);
        configureDriveMotor(backRight, DcMotor.Direction.REVERSE);

        try {
            DriveEncoderLocalizer localizer = new DriveEncoderLocalizer(hardwareMap,
                    PedroPathingConfig.driveEncoderConstants(), PedroPathingConfig.FOAM_TILE_START);
            int frontLeftStartTicks = frontLeft.getCurrentPosition();
            int frontRightStartTicks = frontRight.getCurrentPosition();
            int backLeftStartTicks = backLeft.getCurrentPosition();
            int backRightStartTicks = backRight.getCurrentPosition();
            telemetry.addLine("Unscaled Pedro encoder check; no autonomous path is running.");
            telemetry.addLine("Start at the foam-tile origin facing Pedro +X; pose units are tick-equivalent until calibrated.");
            telemetry.addLine("Forward/strafe scales are provisional; turn scale is disabled until measured.");
            telemetry.update();

            waitForStart();
            while (opModeIsActive()) {
                double forward = MecanumMath.deadband(-gamepad1.left_stick_y,
                        RobotConfig.STICK_DEADBAND);
                double strafe = MecanumMath.deadband(gamepad1.left_stick_x,
                        RobotConfig.STICK_DEADBAND);
                double turn = MecanumMath.deadband(gamepad1.right_stick_x,
                        RobotConfig.STICK_DEADBAND);
                double[] powers = MecanumMath.wheelPowers(forward, strafe, turn);
                double driveScale = gamepad1.left_bumper ? 0.08 : 0.15;
                setDrivePowers(powers, driveScale);

                localizer.update();
                Pose pose = localizer.getPose();
                telemetry.addData("Scale", PedroPathingConfig.DRIVE_SCALE_CALIBRATED
                        ? "calibrated" : "UNSCALED (tick-equivalent units)");
                telemetry.addData("Pedro pose", "x=%.1f y=%.1f %s; heading=%.1f deg",
                        pose.getX(), pose.getY(),
                        PedroPathingConfig.DRIVE_SCALE_CALIBRATED ? "in" : "tick units",
                        Math.toDegrees(pose.getHeading()));
                telemetry.addData("Wheel ticks since init", "FL %d  FR %d  BL %d  BR %d",
                        frontLeft.getCurrentPosition() - frontLeftStartTicks,
                        frontRight.getCurrentPosition() - frontRightStartTicks,
                        backLeft.getCurrentPosition() - backLeftStartTicks,
                        backRight.getCurrentPosition() - backRightStartTicks);
                telemetry.addData("Turn scale", PedroPathingConfig.TURN_TICKS_TO_INCHES == 0.0
                        ? "DISABLED (not calibrated)" : "enabled");
                telemetry.addLine("Left stick: forward/strafe; right stick X: turn.");
                telemetry.addLine("Hold Gamepad 1 LB for the slower 8% setting; normal cap is 15%.");
                telemetry.update();
            }
        } finally {
            stopDrive();
        }
    }

    private static void configureDriveMotor(DcMotorEx motor, DcMotor.Direction direction) {
        motor.setPower(0.0);
        motor.setDirection(direction);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    private void setDrivePowers(double[] powers, double scale) {
        frontLeft.setPower(powers[0] * scale);
        frontRight.setPower(powers[1] * scale);
        backLeft.setPower(powers[2] * scale);
        backRight.setPower(powers[3] * scale);
    }

    private void stopDrive() {
        if (frontLeft != null) frontLeft.setPower(0.0);
        if (frontRight != null) frontRight.setPower(0.0);
        if (backLeft != null) backLeft.setPower(0.0);
        if (backRight != null) backRight.setPower(0.0);
    }
}
