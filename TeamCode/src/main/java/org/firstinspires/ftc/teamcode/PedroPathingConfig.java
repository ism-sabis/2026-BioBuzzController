package org.firstinspires.ftc.teamcode;

import com.pedropathing.geometry.Pose;
import com.pedropathing.ftc.localization.constants.DriveEncoderConstants;

/** Starting point for the foam-tile drive-encoder localization check. */
public final class PedroPathingConfig {
    private PedroPathingConfig() { }

    /** Local test origin on the foam tile; Pedro heading 0 points along +X. */
    public static final Pose FOAM_TILE_START = new Pose(0.0, 0.0, 0.0);

    /**
     * Uncalibrated scales keep the check useful for collecting raw encoder deltas. One tick is
     * shown as one position unit, not one inch. Do not enable path following with these values.
     */
    public static final boolean DRIVE_SCALE_CALIBRATED = false;
    public static final double FORWARD_TICKS_TO_INCHES = 1.0;
    public static final double STRAFE_TICKS_TO_INCHES = 1.0;
    /** Pedro uses inches per turn tick, then divides by robotWidth + robotLength to get radians. */
    public static final double TURN_TICKS_TO_INCHES = 0.0;
    /** Placeholders; calibrate the effective drive wheel dimensions before using turn localization. */
    public static final double ROBOT_WIDTH_INCHES = 1.0;
    public static final double ROBOT_LENGTH_INCHES = 1.0;

    public static DriveEncoderConstants driveEncoderConstants() {
        return new DriveEncoderConstants()
                .forwardTicksToInches(FORWARD_TICKS_TO_INCHES)
                .strafeTicksToInches(STRAFE_TICKS_TO_INCHES)
                .turnTicksToInches(TURN_TICKS_TO_INCHES)
                .robotWidth(ROBOT_WIDTH_INCHES)
                .robotLength(ROBOT_LENGTH_INCHES)
                // RobotHardware normalizes each tested motor's positive power direction. Keep the
                // Pedro encoder multipliers at +1 and confirm raw encoder signs in the check mode.
                .leftFrontEncoderDirection(1.0)
                .rightFrontEncoderDirection(1.0)
                .leftRearEncoderDirection(1.0)
                .rightRearEncoderDirection(1.0)
                .leftFrontMotorName("frontLeft")
                .rightFrontMotorName("frontRight")
                .leftRearMotorName("backLeft")
                .rightRearMotorName("backRight");
    }
}
