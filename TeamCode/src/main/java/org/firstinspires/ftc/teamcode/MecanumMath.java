package org.firstinspires.ftc.teamcode;

/** Drive math with no hardware in it, so it can be tested on a laptop. */
public final class MecanumMath {
    private MecanumMath() { }

    /** Returns 0 inside the deadband and rescales the rest so full stick is still 1. */
    public static double deadband(double value, double band) {
        if (band <= 0.0) return value;
        if (band >= 1.0 || Math.abs(value) <= band) return 0.0;
        return Math.signum(value) * (Math.abs(value) - band) / (1.0 - band);
    }

    /**
     * Converts a field-relative stick command into a robot-relative one.
     *
     * @param forward    + is away from the driver
     * @param strafe     + is to the driver's right
     * @param headingRad robot yaw from the IMU, + is counter-clockwise seen from above
     * @return {robotForward, robotStrafe}
     */
    public static double[] fieldToRobot(double forward, double strafe, double headingRad) {
        double cos = Math.cos(headingRad);
        double sin = Math.sin(headingRad);
        return new double[] {
                forward * cos - strafe * sin,
                forward * sin + strafe * cos
        };
    }

    /**
     * Mixes forward, strafe (+ right) and turn (+ clockwise) into wheel powers, scaled down
     * together if any wheel would exceed full power.
     *
     * @return {frontLeft, frontRight, backLeft, backRight}
     */
    public static double[] wheelPowers(double forward, double strafe, double turn) {
        double fl = forward + strafe + turn;
        double fr = forward - strafe - turn;
        double bl = forward - strafe + turn;
        double br = forward + strafe - turn;
        double scale = Math.max(1.0, Math.max(Math.max(Math.abs(fl), Math.abs(fr)),
                Math.max(Math.abs(bl), Math.abs(br))));
        return new double[] {fl / scale, fr / scale, bl / scale, br / scale};
    }
}
