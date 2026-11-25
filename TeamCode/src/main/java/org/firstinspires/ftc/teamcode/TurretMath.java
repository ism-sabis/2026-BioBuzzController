package org.firstinspires.ftc.teamcode;

/** Turret math with no hardware in it, so it can be tested on a laptop. */
public final class TurretMath {
    private TurretMath() { }

    public static double ticksToDegrees(int ticks, double ticksPerRevolution) {
        if (ticksPerRevolution <= 0.0) return 0.0;
        return ticks * 360.0 / ticksPerRevolution;
    }

    /**
     * Keeps a target inside the soft limits. A target just past a limit is swapped for the same
     * direction one full turn back (for example 365 becomes 5) when that fits inside the limits,
     * so the turret unwinds straight onto the target. Otherwise it is held at the limit.
     */
    public static double boundTarget(double targetDegrees, double limitDegrees) {
        double limit = Math.abs(limitDegrees);
        if (targetDegrees > limit) {
            double unwound = targetDegrees - 360.0;
            return unwound >= -limit ? unwound : limit;
        }
        if (targetDegrees < -limit) {
            double unwound = targetDegrees + 360.0;
            return unwound <= limit ? unwound : -limit;
        }
        return targetDegrees;
    }

    /** Where the turret should point, given where it is and how far off-center the tag is. */
    public static double aimTarget(double currentDegrees, double txDegrees, double gain,
                                   double limitDegrees) {
        return boundTarget(currentDegrees + txDegrees * gain, limitDegrees);
    }

    /** Blocks power that would push the turret further past a soft limit. */
    public static double limitPower(double power, double currentDegrees, double limitDegrees) {
        double limit = Math.abs(limitDegrees);
        if (currentDegrees >= limit && power > 0.0) return 0.0;
        if (currentDegrees <= -limit && power < 0.0) return 0.0;
        return power;
    }

    /** Proportional power toward a target: zero inside the tolerance, capped at maxPower. */
    public static double positionPower(double error, double kp, double maxPower,
                                       double tolerance) {
        return positionPower(error, kp, maxPower, tolerance, 0.0);
    }

    /** Apply a minimum output outside tolerance to overcome static friction. */
    public static double positionPower(double error, double kp, double maxPower,
                                       double tolerance, double minPower) {
        if (Math.abs(error) <= Math.abs(tolerance)) return 0.0;
        double cap = Math.abs(maxPower);
        if (cap == 0.0) return 0.0;
        double power = Math.max(-cap, Math.min(cap, error * kp));
        double floor = Math.min(Math.abs(minPower), cap);
        if (Math.abs(power) < floor) {
            power = Math.copySign(floor, power == 0.0 ? error : power);
        }
        return power;
    }
}
