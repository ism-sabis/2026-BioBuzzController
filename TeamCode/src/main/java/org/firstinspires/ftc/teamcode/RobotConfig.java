package org.firstinspires.ftc.teamcode;

/**
 * Bring-up settings for BioBuzz. Encoder counts and servo positions are starting estimates and
 * must be calibrated on the assembled robot before operating near the mechanism limits.
 * These fields are public so RobotTuning can adjust them live.
 */
public final class RobotConfig {
    private RobotConfig() { }

    public static int SLIDE_MAX_TICKS = 900;
    public static int SLIDE_FLOWER_TICKS = 550;
    public static double SLIDE_KP = 0.0030;
    public static double SLIDE_MAX_POWER = 0.65;
    public static double SLIDE_MANUAL_POWER = 0.45;

    public static double TURRET_TICKS_PER_REVOLUTION = 537.7;
    public static double TURRET_LIMIT_DEGREES = 360.0;
    public static double TURRET_FLOWER_DEGREES = 45.0;
    public static double TURRET_KP = 0.0060;
    public static double TURRET_MAX_POWER = 0.45;
    public static double TURRET_RETURN_POWER = 0.55;
    public static double TURRET_MANUAL_POWER = 0.30;
    public static double TURRET_HOME_TOLERANCE_DEGREES = 5.0;
    public static double LIMELIGHT_AIM_GAIN = 1.0;

    // The left and right servos are mirrored on the assembly, so calibrate each separately.
    public static double RAMP_LEFT_STOW = 0.14;
    public static double RAMP_LEFT_LEVEL = 0.483;
    public static double RAMP_RIGHT_STOW = 0.86;
    public static double RAMP_RIGHT_LEVEL = 0.517;
    public static double GATE_CLOSED = 0.12;
    public static double GATE_OPEN = 0.72;

    public static double LAUNCHER_TICKS_PER_REVOLUTION = 28.0;
    public static double LAUNCHER_TARGET_RPM = 3000.0;
    public static double LAUNCHER_READY_RPM_FRACTION = 0.95;
    public static double LAUNCHER_RESTART_RPM_FRACTION = 0.90;
    public static double LAUNCHER_CUTOFF_RPM_FRACTION = 1.00;
    public static double LAUNCHER_POWER = 0.85;
    public static double LAUNCHER_CONTINUOUS_KP = 0.0008;
    /** false selects encoder hysteresis/coast; true selects continuous velocity correction. */
    public static boolean LAUNCHER_CONTINUOUS_VELOCITY_HOLD = false;
    public static double FEED_OPEN_SECONDS = 0.22;
    public static double FEED_GAP_SECONDS = 0.16;
    public static double INTAKE_POWER = 0.80;

    public static int RED_LIMELIGHT_PIPELINE = 0;
    public static int BLUE_LIMELIGHT_PIPELINE = 0;
    public static boolean FLOWER_LOCKOUT_ENABLED = true;
    public static double TELEOP_DURATION_SECONDS = 120.0;
    public static double FLOWER_UNLOCK_REMAINING_SECONDS = 60.0;
}
