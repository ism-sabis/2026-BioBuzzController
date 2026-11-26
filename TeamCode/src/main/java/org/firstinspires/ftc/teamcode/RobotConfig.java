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
    public static int SLIDE_MAX_TICKS = 1850;
    public static int SLIDE_FLOWER_TICKS = 550;
    public static double SLIDE_KP = 0.0030;
    public static double SLIDE_MAX_POWER = 0.65;
    public static double SLIDE_MANUAL_POWER = 0.45;
    public static int SLIDE_TOLERANCE_TICKS = 8;

    // ---- Turret ----
    /** Calibrated: the prior scale showed 64 degrees over a measured 90-degree move. */
    public static double TURRET_TICKS_PER_REVOLUTION = 1274.5;
    /** User-reported mechanical travel measured from home: CCW is negative, CW is positive. */
    public static final double TURRET_PHYSICAL_MIN_DEGREES = -210.0;
    public static final double TURRET_PHYSICAL_MAX_DEGREES = 180.0;
    /** Software limits match the user-reported mechanical endpoints. */
    public static final double TURRET_LIMIT_MARGIN_DEGREES = 0.0;
    public static double TURRET_MIN_DEGREES =
            TURRET_PHYSICAL_MIN_DEGREES + TURRET_LIMIT_MARGIN_DEGREES;
    public static double TURRET_MAX_DEGREES =
            TURRET_PHYSICAL_MAX_DEGREES - TURRET_LIMIT_MARGIN_DEGREES;
    public static double TURRET_FLOWER_DEGREES = 45.0;
    public static double TURRET_KP = 0.0058;
    /** Measured initial breakaway value; verify it remains reliable in both directions. */
    public static double TURRET_MIN_POWER = 0.08;
    public static double TURRET_MAX_POWER = 1.0;
    public static double TURRET_TOLERANCE_DEGREES = 0.5;
    /** Small relative target step used by BioBuzz Tuning for proportional-response checks. */
    public static double TURRET_TEST_STEP_DEGREES = 1.0;
    /** R2+Circle test amplitude; 210 reaches the inset targets near both asymmetric limits. */
    public static double TURRET_TEST_FLIP_DEGREES = 210.0;
    /** Keep the wide flip-test targets clear of hard stops while diagnosing overrun. */
    public static final double TURRET_TEST_LIMIT_MARGIN_DEGREES = 60.0;
    /** Maximum reference speed for every automatic and joystick-targeted turret move. */
    public static double TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC = 360.0;
    /** Acceleration limit for the turret's speed-up phase, in degrees per second squared. */
    public static double TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ = 720.0;
    /** Lower braking acceleration gives the turret more distance to shed momentum before a target. */
    public static double TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ = 190.0;
    /** Full-stick speed for jogging the target angle; motor output remains closed-loop/profiled. */
    public static double TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC = 240.0;

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
    // Both ramp servos start at zero. RobotTuning adjusts the mechanically linked pair together.
    public static double RAMP_LEFT_STOW = 0.0;
    public static double RAMP_LEFT_LEVEL = 0.483;
    public static double RAMP_RIGHT_STOW = 0.0;
    public static double RAMP_RIGHT_LEVEL = 0.517;
    public static double GATE_CLOSED = 0.11;
    public static double GATE_OPEN = 0.38;
    /** Servo target for the hood: 0 is fully down and 1 is fully up. */
    public static double HOOD_POSITION = 1.0;

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
