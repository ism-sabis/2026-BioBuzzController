package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/**
 * Disabled PedroPathing autonomous entry point. Add the robot-specific follower and paths here
 * after the localization constants and drive geometry have been measured; this shell does not move.
 */
@Autonomous(name = "BioBuzz Pedro Auto (shell)", group = "BioBuzz")
@Disabled
public class PedroAutoShell extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        telemetry.addLine("PedroPathing autonomous shell: no paths are configured.");
        telemetry.addLine("Configure localization and paths before enabling this OpMode.");
        telemetry.update();
        waitForStart();
        while (opModeIsActive()) {
            telemetry.addLine("No movement commands configured.");
            telemetry.update();
            idle();
        }
    }
}
