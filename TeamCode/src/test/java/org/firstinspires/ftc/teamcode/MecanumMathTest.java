package org.firstinspires.ftc.teamcode;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MecanumMathTest {
    private static final double EPS = 1e-9;

    @Test
    public void headingZeroLeavesTheCommandAlone() {
        assertArrayEquals(new double[] {0.7, -0.2}, MecanumMath.fieldToRobot(0.7, -0.2, 0.0), EPS);
    }

    @Test
    public void robotTurnedLeftMustStrafeRightToGoFieldForward() {
        // Robot faces the driver's left (yaw +90). Field-forward is now on the robot's right.
        double[] robot = MecanumMath.fieldToRobot(1.0, 0.0, Math.toRadians(90));
        assertEquals(0.0, robot[0], EPS);
        assertEquals(1.0, robot[1], EPS);
    }

    @Test
    public void robotTurnedRightMustStrafeLeftToGoFieldForward() {
        double[] robot = MecanumMath.fieldToRobot(1.0, 0.0, Math.toRadians(-90));
        assertEquals(0.0, robot[0], EPS);
        assertEquals(-1.0, robot[1], EPS);
    }

    @Test
    public void robotFacingTheDriverMustReverseToGoFieldForward() {
        double[] robot = MecanumMath.fieldToRobot(1.0, 0.0, Math.toRadians(180));
        assertEquals(-1.0, robot[0], EPS);
        assertEquals(0.0, robot[1], EPS);
    }

    @Test
    public void robotTurnedLeftMustReverseToGoFieldRight() {
        double[] robot = MecanumMath.fieldToRobot(0.0, 1.0, Math.toRadians(90));
        assertEquals(-1.0, robot[0], EPS);
        assertEquals(0.0, robot[1], EPS);
    }

    @Test
    public void wheelPowersForPureMoves() {
        assertArrayEquals(new double[] {1, 1, 1, 1}, MecanumMath.wheelPowers(1, 0, 0), EPS);
        assertArrayEquals(new double[] {1, -1, -1, 1}, MecanumMath.wheelPowers(0, 1, 0), EPS);
        assertArrayEquals(new double[] {1, -1, 1, -1}, MecanumMath.wheelPowers(0, 0, 1), EPS);
    }

    @Test
    public void wheelPowersNeverExceedFullPower() {
        double[] powers = MecanumMath.wheelPowers(1, 1, 1);
        assertArrayEquals(new double[] {1.0, -1.0 / 3, 1.0 / 3, 1.0 / 3}, powers, EPS);
    }

    @Test
    public void deadbandZeroesSmallInputsAndKeepsFullStick() {
        assertEquals(0.0, MecanumMath.deadband(0.04, 0.05), EPS);
        assertEquals(0.0, MecanumMath.deadband(-0.05, 0.05), EPS);
        assertEquals(1.0, MecanumMath.deadband(1.0, 0.05), EPS);
        assertEquals(-1.0, MecanumMath.deadband(-1.0, 0.05), EPS);
        assertEquals(0.5, MecanumMath.deadband(0.525, 0.05), EPS);
    }
}
