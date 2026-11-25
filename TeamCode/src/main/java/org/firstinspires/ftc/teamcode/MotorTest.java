package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Motor Test: run this FIRST, with the robot up on a box so the wheels spin freely.
 *
 * Hold one button at a time; that wheel should spin FORWARD (the way the robot would roll
 * forward). If it spins backward, flip its direction in RobotHardware. If the wrong wheel
 * spins, the config names or plugs are swapped.
 *
 *   X = front left     Y = front right
 *   A = back left      B = back right
 */
@TeleOp(name = "Motor Test", group = "BioBuzz")
public class MotorTest extends LinearOpMode {
    @Override
    public void runOpMode() {
        RobotHardware robot = new RobotHardware(hardwareMap);
        robot.initialize();
        waitForStart();

        while (opModeIsActive()) {
            robot.clearBulkCache();
            // Start+A and Start+B pair a gamepad, so ignore the buttons while Start is held.
            boolean armed = !gamepad1.start;
            robot.frontLeft.setPower(armed && gamepad1.x ? 0.3 : 0.0);
            robot.frontRight.setPower(armed && gamepad1.y ? 0.3 : 0.0);
            robot.backLeft.setPower(armed && gamepad1.a ? 0.3 : 0.0);
            robot.backRight.setPower(armed && gamepad1.b ? 0.3 : 0.0);

            telemetry.addLine("X=FL  Y=FR  A=BL  B=BR  (each should spin FORWARD)");
            telemetry.addData("FL / FR encoder", "%d / %d",
                    robot.frontLeft.getCurrentPosition(), robot.frontRight.getCurrentPosition());
            telemetry.addData("BL / BR encoder", "%d / %d",
                    robot.backLeft.getCurrentPosition(), robot.backRight.getCurrentPosition());
            telemetry.addData("Odometry digital", "left=%s right=%s",
                    robot.odometryLeft.getState() ? "HIGH" : "LOW",
                    robot.odometryRight.getState() ? "HIGH" : "LOW");
            if (robot.hasOdometry()) {
                telemetry.addData("Pods forward / strafe", "%d / %d",
                        robot.forwardPodTicks(), robot.strafePodTicks());
            }
            telemetry.update();
        }
        robot.stopAllMotors();
    }
}
