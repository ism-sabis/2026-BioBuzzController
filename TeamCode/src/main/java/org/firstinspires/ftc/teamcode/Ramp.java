package org.firstinspires.ftc.teamcode;

/** The two mirrored ramp servos: stowed or level. */
public final class Ramp {
    private final RobotHardware robot;
    private boolean deployed;

    public Ramp(RobotHardware robot) {
        this.robot = robot;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void toggle() {
        set(!deployed);
    }

    public void set(boolean deploy) {
        deployed = deploy;
        apply();
    }

    /** Re-sends the servo positions, so live tuning changes show up straight away. */
    public void apply() {
        robot.rampLeft.setPosition(clamp01(deployed
                ? RobotConfig.RAMP_LEFT_LEVEL : RobotConfig.RAMP_LEFT_STOW));
        robot.rampRight.setPosition(clamp01(deployed
                ? RobotConfig.RAMP_RIGHT_LEVEL : RobotConfig.RAMP_RIGHT_STOW));
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
