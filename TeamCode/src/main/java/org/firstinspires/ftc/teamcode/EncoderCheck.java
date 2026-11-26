package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.hardware.lynx.LynxModule;

import java.util.List;

/** Reads every configured motor encoder and applies one short, bounded pulse at a time. */
@TeleOp(name = "Encoder Check", group = "BioBuzz")
public class EncoderCheck extends OpMode {
    private static final double PULSE_POWER = 0.18;
    private static final long PULSE_DURATION_NANOS = 200_000_000L;

    private static final String[] NAMES = {
            "frontLeft (Expansion 0)", "backLeft (Expansion 1)",
            "intake (Expansion 2)", "launcher (Expansion 3)",
            "backboardSlide (Control 0)", "turret (Control 1)",
            "backRight (Control 2)", "frontRight (Control 3)"
    };

    private DcMotorEx[] motors;
    private List<LynxModule> hubs;
    private int selected;
    private int pulseMotor = -1;
    private int pulseStartTicks;
    private int lastDelta;
    private String lastTested = "none";
    private String lastResult = "No pulse yet";
    private long pulseEndsAt;
    private boolean previousUp;
    private boolean previousDown;
    private boolean previousA;

    @Override
    public void init() {
        motors = new DcMotorEx[] {
                hardwareMap.get(DcMotorEx.class, "frontLeft"),
                hardwareMap.get(DcMotorEx.class, "backLeft"),
                hardwareMap.get(DcMotorEx.class, "intake"),
                hardwareMap.get(DcMotorEx.class, "launcher"),
                hardwareMap.get(DcMotorEx.class, "backboardSlide"),
                hardwareMap.get(DcMotorEx.class, "turret"),
                hardwareMap.get(DcMotorEx.class, "backRight"),
                hardwareMap.get(DcMotorEx.class, "frontRight")
        };
        hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : hubs) hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);

        motors[0].setDirection(DcMotor.Direction.FORWARD);
        motors[1].setDirection(DcMotor.Direction.REVERSE);
        motors[2].setDirection(DcMotor.Direction.FORWARD);
        motors[3].setDirection(DcMotor.Direction.REVERSE);
        motors[4].setDirection(DcMotor.Direction.REVERSE);
        motors[5].setDirection(DcMotor.Direction.FORWARD);
        motors[6].setDirection(DcMotor.Direction.REVERSE);
        motors[7].setDirection(DcMotor.Direction.REVERSE);

        for (DcMotorEx motor : motors) {
            motor.setPower(0.0);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        // Preserve the launcher's normal coasting behavior when its short pulse ends.
        motors[3].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    @Override
    public void loop() {
        clearBulkCache();
        long now = System.nanoTime();

        if (pulseMotor >= 0) {
            if (now >= pulseEndsAt) {
                int completedMotor = pulseMotor;
                motors[completedMotor].setPower(0.0);
                clearBulkCache();
                lastDelta = motors[completedMotor].getCurrentPosition() - pulseStartTicks;
                lastTested = NAMES[completedMotor];
                lastResult = lastDelta == 0
                        ? "No count change; verify the motor moved during the pulse"
                        : "Encoder count changed";
                pulseMotor = -1;
            } else {
                motors[pulseMotor].setPower(PULSE_POWER);
            }
        }

        if (pulseMotor < 0) {
            if (gamepad1.dpad_up && !previousUp) selected = (selected + NAMES.length - 1) % NAMES.length;
            if (gamepad1.dpad_down && !previousDown) selected = (selected + 1) % NAMES.length;
            if (gamepad1.a && !previousA) beginPulse();
        }
        previousUp = gamepad1.dpad_up;
        previousDown = gamepad1.dpad_down;
        previousA = gamepad1.a;

        telemetry.addLine("Encoder Check: one motor at a time; pulse is 0.18 power for 0.20 s.");
        telemetry.addLine("D-pad up/down selects. Tap Cross/A for one pulse. STOP ends the OpMode.");
        telemetry.addData("Selected", NAMES[selected]);
        telemetry.addData("Last pulse", "%s | delta %d | %s", lastTested, lastDelta, lastResult);
        telemetry.addData("FL / FR", "%d / %d", motors[0].getCurrentPosition(),
                motors[7].getCurrentPosition());
        telemetry.addData("BL / BR", "%d / %d", motors[1].getCurrentPosition(),
                motors[6].getCurrentPosition());
        telemetry.addData("Intake / launcher", "%d / %d", motors[2].getCurrentPosition(),
                motors[3].getCurrentPosition());
        telemetry.addData("Slide / turret", "%d / %d", motors[4].getCurrentPosition(),
                motors[5].getCurrentPosition());
        telemetry.update();
    }

    @Override
    public void stop() {
        if (motors != null) {
            for (DcMotorEx motor : motors) motor.setPower(0.0);
        }
    }

    private void beginPulse() {
        pulseMotor = selected;
        pulseStartTicks = motors[pulseMotor].getCurrentPosition();
        pulseEndsAt = System.nanoTime() + PULSE_DURATION_NANOS;
        lastTested = NAMES[pulseMotor];
        lastResult = "Pulse running";
        motors[pulseMotor].setPower(PULSE_POWER);
    }

    private void clearBulkCache() {
        for (LynxModule hub : hubs) hub.clearBulkCache();
    }
}
