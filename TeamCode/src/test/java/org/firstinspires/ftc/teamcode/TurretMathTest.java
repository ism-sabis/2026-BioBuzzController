package org.firstinspires.ftc.teamcode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TurretMathTest {
    private static final double EPS = 1e-9;

    @Test
    public void ticksConvertToDegrees() {
        assertEquals(180.0, TurretMath.ticksToDegrees(500, 1000.0), EPS);
        assertEquals(0.0, TurretMath.ticksToDegrees(500, 0.0), EPS);
    }

    @Test
    public void targetInsideTheLimitsIsUnchanged() {
        assertEquals(120.0, TurretMath.boundTarget(120.0, 360.0), EPS);
        assertEquals(-360.0, TurretMath.boundTarget(-360.0, 360.0), EPS);
    }

    @Test
    public void targetPastTheLimitUnwindsAFullTurn() {
        assertEquals(5.0, TurretMath.boundTarget(365.0, 360.0), EPS);
        assertEquals(-5.0, TurretMath.boundTarget(-365.0, 360.0), EPS);
        assertEquals(-155.0, TurretMath.boundTarget(205.0, 200.0), EPS);
    }

    @Test
    public void targetStopsAtTheLimitWhenUnwindingDoesNotFit() {
        assertEquals(90.0, TurretMath.boundTarget(95.0, 90.0), EPS);
        assertEquals(-90.0, TurretMath.boundTarget(-95.0, 90.0), EPS);
    }

    @Test
    public void aimTargetAddsTheTagOffsetToTheCurrentAngle() {
        assertEquals(40.0, TurretMath.aimTarget(30.0, 10.0, 1.0, 360.0), EPS);
        assertEquals(20.0, TurretMath.aimTarget(30.0, 10.0, -1.0, 360.0), EPS);
        assertEquals(10.0, TurretMath.aimTarget(355.0, 15.0, 1.0, 360.0), EPS);
    }

    @Test
    public void powerIsBlockedOnlyWhenPushingPastALimit() {
        assertEquals(0.0, TurretMath.limitPower(0.3, 360.0, 360.0), EPS);
        assertEquals(-0.3, TurretMath.limitPower(-0.3, 360.0, 360.0), EPS);
        assertEquals(0.0, TurretMath.limitPower(-0.3, -361.0, 360.0), EPS);
        assertEquals(0.3, TurretMath.limitPower(0.3, 100.0, 360.0), EPS);
    }

    @Test
    public void positionPowerIsProportionalCappedAndQuietNearTheTarget() {
        assertEquals(0.06, TurretMath.positionPower(10.0, 0.006, 0.45, 0.5), EPS);
        assertEquals(0.45, TurretMath.positionPower(300.0, 0.006, 0.45, 0.5), EPS);
        assertEquals(-0.45, TurretMath.positionPower(-300.0, 0.006, 0.45, 0.5), EPS);
        assertEquals(0.0, TurretMath.positionPower(0.4, 0.006, 0.45, 0.5), EPS);
    }

    @Test
    public void positionPowerUsesMinimumOnlyOutsideToleranceAndWithinMaximum() {
        assertEquals(0.08, TurretMath.positionPower(1.0, 0.006, 0.45, 0.5, 0.08), EPS);
        assertEquals(-0.08, TurretMath.positionPower(-1.0, 0.006, 0.45, 0.5, 0.08), EPS);
        assertEquals(0.0, TurretMath.positionPower(0.4, 0.006, 0.45, 0.5, 0.08), EPS);
        assertEquals(0.10, TurretMath.positionPower(300.0, 0.006, 0.10, 0.5, 0.25), EPS);
    }
}
