package org.firstinspires.ftc.teamcode;

/**
 * Bring-up settings for BioBuzz. Encoder counts and servo positions are starting estimates and
 * must be calibrated on the assembled robot before operating near the mechanism limits.
 * These fields are public so RobotTuning can adjust them live.
 */
public final class RobotConfig {
    private RobotConfig() { }

    // ---- Drive ----
    /** Stick movement smaller than this is ignored so a worn stick does not creep the robot. */
    public static double STICK_DEADBAND = 0.05;
    /** Drive speed multiplier while Gamepad 1 right bumper is held. */
    public static double DRIVE_SLOW_SCALE = 0.40;

    // ---- Odometry pods (one forward pod, one strafe pod, mounted 90 degrees apart) ----
    // A pod has no motor of its own: its cable plugs into the encoder port of an existing motor.
    // Put that motor's configuration name here (for example "frontLeft"). Leave "" until the
    // ports are decided; the robot runs without odometry while these are empty.
    public static String FORWARD_POD_MOTOR_NAME = "";
    public static String STRAFE_POD_MOTOR_NAME = "";

    // ---- Backboard slide ----
    public static int SLIDE_MAX_TICKS = 900;
    public static int SLIDE_FLOWER_TICKS = 550;
    public static double SLIDE_KP = 0.0030;
    public static double SLIDE_MAX_POWER = 0.65;
    public static double SLIDE_MANUAL_POWER = 0.45;
    public static int SLIDE_TOLERANCE_TICKS = 8;

    // ---- Turret ----
    public static double TURRET_TICKS_PER_REVOLUTION = 537.7;
    public static double TURRET_LIMIT_DEGREES = 360.0;
    public static double TURRET_FLOWER_DEGREES = 45.0;
    public static double TURRET_KP = 0.0060;
    public static double TURRET_MAX_POWER = 0.45;
    public static double TURRET_MANUAL_POWER = 0.30;
    public static double TURRET_TOLERANCE_DEGREES = 0.5;

    // ---- Limelight aiming (camera is mounted on the turret and turns with it) ----
    /** Turret degrees per degree of Limelight tx. Make it negative if the turret turns away. */
    public static double LIMELIGHT_AIM_GAIN = 1.0;
    /** Camera frames older than this are ignored. */
    public static double LIMELIGHT_MAX_STALENESS_MS = 250.0;
    /**
     * AprilTag IDs the turret may aim at for each alliance (inclusive range). The turret aims at
     * the largest tag in view inside the range and ignores every other tag. BIOBUZZ stickers:
     *   RED SCORING 30-33, RED AUDIENCE 34-37, BLUE AUDIENCE 38-41, BLUE SCORING 42-45.
     * The defaults allow both of an alliance's CELLS. To aim at one CELL only, narrow the range
     * (for example 30 and 33 for RED SCORING).
     */
    public static int RED_TAG_MIN_ID = 30;
    public static int RED_TAG_MAX_ID = 37;
    public static int BLUE_TAG_MIN_ID = 38;
    public static int BLUE_TAG_MAX_ID = 45;
    public static int RED_LIMELIGHT_PIPELINE = 0;
    public static int BLUE_LIMELIGHT_PIPELINE = 0;

    // ---- Ramp and gate servos ----
    // The left and right servos are mirrored on the assembly, so calibrate each separately.
    public static double RAMP_LEFT_STOW = 0.14;
    public static double RAMP_LEFT_LEVEL = 0.483;
    public static double RAMP_RIGHT_STOW = 0.86;
    public static double RAMP_RIGHT_LEVEL = 0.517;
    public static double GATE_CLOSED = 0.12;
    public static double GATE_OPEN = 0.72;

    // ---- Launcher ----
    /** Encoder ticks per revolution of the bare motor shaft (28 for goBILDA Yellow Jacket). */
    public static double LAUNCHER_TICKS_PER_REVOLUTION = 28.0;
    /**
     * Gearbox ratio between the motor shaft and the launcher wheel. 1.0 means every RPM number
     * below is motor-shaft RPM. Set it to the real ratio (for example 3.7) to work in wheel RPM.
     */
    public static double LAUNCHER_GEAR_RATIO = 1.0;
    public static double LAUNCHER_TARGET_RPM = 3000.0;
    /** Fraction of target RPM the launcher must reach before a ball is fed. */
    public static double LAUNCHER_READY_RPM_FRACTION = 0.95;
    /** Set true to replace the hub's default velocity PIDF with the four values below. */
    public static boolean LAUNCHER_CUSTOM_PIDF = false;
    public static double LAUNCHER_P = 10.0;
    public static double LAUNCHER_I = 3.0;
    public static double LAUNCHER_D = 0.0;
    public static double LAUNCHER_F = 0.0;

    // ---- Feeding ----
    public static double FEED_OPEN_SECONDS = 0.22;
    public static double FEED_GAP_SECONDS = 0.16;
    public static double INTAKE_POWER = 0.80;

    // ---- Flower lockout ----
    public static boolean FLOWER_LOCKOUT_ENABLED = true;
    public static double TELEOP_DURATION_SECONDS = 120.0;
    public static double FLOWER_UNLOCK_REMAINING_SECONDS = 60.0;
}
