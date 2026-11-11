package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/** Mecanum drive with field centric and robot centric modes. */
public final class Drive {
    private final RobotHardware robot;
    private boolean fieldCentric = true;

    public Drive(RobotHardware robot) {
        this.robot = robot;
    }

    public void toggleFieldCentric() {
        fieldCentric = !fieldCentric;
    }

    public boolean isFieldCentric() {
        return fieldCentric;
    }

    /** Makes the direction the robot faces right now the field-forward direction. */
    public void resetHeading() {
        robot.imu.resetYaw();
    }

    /**
     * @param forward + drives away from the driver (pass -left_stick_y)
     * @param strafe  + drives right
     * @param turn    + turns clockwise
     * @param scale   speed multiplier, 1.0 for full speed
     */
    public void drive(double forward, double strafe, double turn, double scale) {
        forward = MecanumMath.deadband(forward, RobotConfig.STICK_DEADBAND) * scale;
        strafe = MecanumMath.deadband(strafe, RobotConfig.STICK_DEADBAND) * scale;
        turn = MecanumMath.deadband(turn, RobotConfig.STICK_DEADBAND) * scale;
        if (fieldCentric) {
            double heading = robot.imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            double[] rotated = MecanumMath.fieldToRobot(forward, strafe, heading);
            forward = rotated[0];
            strafe = rotated[1];
        }
        double[] powers = MecanumMath.wheelPowers(forward, strafe, turn);
        robot.frontLeft.setPower(powers[0]);
        robot.frontRight.setPower(powers[1]);
        robot.backLeft.setPower(powers[2]);
        robot.backRight.setPower(powers[3]);
    }
}
