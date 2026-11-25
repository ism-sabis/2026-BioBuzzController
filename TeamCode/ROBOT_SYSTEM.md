# BioBuzz Robot System Reference

This document records the robot's current hardware contract, control behavior, calibration model,
and known gaps. It is intended to let future work start from the actual implementation rather than
reconstructing the assumptions from OpMode code.

The project targets FTC SDK 12.0.0. The control implementation lives in `TeamCode`; the four-wheel
drive is mecanum. Hardware measurements, motor models, most encoder ratios, servo PWM endpoints,
and the Limelight's field configuration have not yet been confirmed on the assembled robot. Values
called *starting estimates* below must be measured before relying on them near a mechanism limit.

## Robot overview

| Mechanism | Current role in the code | Main control concept |
| --- | --- | --- |
| Mecanum drive | Four independently driven wheels | Field centric or robot centric open-loop mixing, slow mode, stick deadband |
| Backboard slide | A linear mechanism named `backboardSlide` | Encoder-referenced extension with a software travel cap |
| Turret | A rotating aiming mechanism | Encoder angle, soft limits, tag tracking, Flower target, manual override |
| Launcher | Ball launching motor | Hub velocity control to a target RPM; coasts when switched off |
| Intake and gate | Ball transfer into the launcher | Timed feeding keeps the launcher-ready check; tuning has a manual gate test |
| Ramp | Two mechanically linked, mirrored servos | Tuning moves both together with complementary positions |
| Hood | One servo | Tuning jogs the position through its 0-to-1 command range |
| Odometry | Left and right pods configured as digital devices | Inputs are mapped; pod encoder counts are not decoded yet |
| IMU and optional Limelight | Heading and fiducial sensing | Yaw for field centric drive; goal tag horizontal angle for turret aim when the camera is configured |

The Flower mechanism is a coordinated slide and turret target. Flower mode does not own the ramp;
the ramp remains an independent mechanism.

## Source map

| File | Responsibility |
| --- | --- |
| [`RobotHardware.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotHardware.java) | Device lookup, motor modes and directions, bulk reads, encoder zeroing, startup servo positions, and IMU orientation |
| [`RobotConfig.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotConfig.java) | Shared calibration values and behavior switches |
| [`BioBuzzTeleOp.java`](src/main/java/org/firstinspires/ftc/teamcode/BioBuzzTeleOp.java) | Driver inputs only: maps the gamepads onto the mechanism classes below |
| [`Drive.java`](src/main/java/org/firstinspires/ftc/teamcode/Drive.java), [`Slide.java`](src/main/java/org/firstinspires/ftc/teamcode/Slide.java), [`Turret.java`](src/main/java/org/firstinspires/ftc/teamcode/Turret.java), [`Shooter.java`](src/main/java/org/firstinspires/ftc/teamcode/Shooter.java), [`Ramp.java`](src/main/java/org/firstinspires/ftc/teamcode/Ramp.java) | One class per mechanism, shared by TeleOp, the tuner, and a future autonomous |
| [`MecanumMath.java`](src/main/java/org/firstinspires/ftc/teamcode/MecanumMath.java), [`TurretMath.java`](src/main/java/org/firstinspires/ftc/teamcode/TurretMath.java), [`FeedSequencer.java`](src/main/java/org/firstinspires/ftc/teamcode/FeedSequencer.java) | Pure logic with no hardware in it, covered by the unit tests in `src/test` |
| [`RobotTuning.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotTuning.java) | Bring-up OpMode for live numeric changes and guarded mechanism movement |
| [`MotorTest.java`](src/main/java/org/firstinspires/ftc/teamcode/MotorTest.java) | Spins one drive wheel per button to check wiring and directions |
| [`PedroLocalizationCheck.java`](src/main/java/org/firstinspires/ftc/teamcode/PedroLocalizationCheck.java) | Low-power manual drive-encoder calibration using PedroPathing |
| [`PedroPathingConfig.java`](src/main/java/org/firstinspires/ftc/teamcode/PedroPathingConfig.java) | Foam-tile origin and provisional encoder scales |
| [`PedroAutoShell.java`](src/main/java/org/firstinspires/ftc/teamcode/PedroAutoShell.java) | Disabled autonomous placeholder with no movement path |
| [`TeamCode/build.gradle`](build.gradle) | TeamCode module dependencies |

`RobotConfig` is the source of truth used by both TeleOp and the tuner. The tuner changes static
fields while the app process is running; changes are lost when the Robot Controller app restarts.
Copy useful values from the tuner's telemetry back into `RobotConfig.java` to keep them.

## Bring-up record

**2026-11-24 — Motor Test:** The observed result was clarified: with the original direction setup
and each wheel commanded forward, back left spun forward while front left, front right, and back
right spun backward. The workspace direction settings have been changed for those three motors
and the updated OnBotJava source built successfully. In the raised-wheel retest, the user reported
all four wheels spinning forward with their assigned controls. The drive direction check passes;
first floor driving should still begin at low power.

**2026-11-25 — Software build:** Uploaded the PedroPathing 2.1.2 libraries and updated
`RobotConfig`, `RobotTuning`, `Turret`, and `TurretMath`, plus the new Pedro localization check, to
OnBotJava. The Control Hub's **Build Everything** succeeded. Local `:TeamCode:testDebugUnitTest`
and `:TeamCode:assembleDebug` also passed. These checks verify compilation and unit tests; no new
mechanism or localization calibration values had been measured at this point.

**2026-11-25 — Turret first motion check:** With the tuner settings unchanged at their defaults,
the user reported that a small target moved the turret, but response was delayed, slightly jerky,
and appeared to speed up. The battery was reported at about 12 V; whether that was at rest or under
load is unknown, so voltage may have affected the response. The reported `Turret target / error`
row was `1.0 / 1.0`, so the encoder angle had not registered movement at that reading. At the current
537.7 ticks/revolution estimate, one degree is only about 1.5 ticks; the separate encoder tick count
still needs to be checked. No minimum-power threshold was measured. Repeat on a charged battery,
then validate settings at a realistic low voltage. Keep `TURRET_MIN_POWER` at `0.0` until encoder
feedback is confirmed to move toward the target.

The launcher gate was measured at `0.11` closed (blocks balls) and `0.38` open. The hood's command
range is `0.0` down to `1.0` up, but its preferred operating target is not yet calibrated. Ramp
endpoints still need final mechanical verification; other configuration values remain estimates.

**2026-11-25 — Tuning diagnostics and one-controller input (local build only):** The telemetry
separates turret target, measured angle, error, encoder ticks, and normalized power request. The
tuner now runs from Gamepad 1 alone; Gamepad 2 is not needed. `:TeamCode:testDebugUnitTest` passed
all 24 tests (8 each for feed sequencing, mecanum math, and turret math), and
`:TeamCode:assembleDebug` succeeded. These changes have not yet been uploaded to OnBotJava; deploy
and use **Build Everything** after the robot is stopped and its battery is charged. Do not install
the debug APK over the FTC Robot Controller app because of its signature mismatch.

**2026-11-25 — Hub connection check:** The existing OnBotJava page at `192.168.43.1:8080` reported
**Robot Controller disconnected** and files read-only. The Hub project lists the tuning and Pedro
source files, but the current workspace updates are not deployed. Resume upload and physical tests
after the Hub is back online and the battery is charged.

**2026-11-25 — Control Hub startup failure:** The user clarified that the Robot Controller app
opened and displayed its network status, then failed during its own **Initializing Robot** startup
phase, before any OpMode was selected or run. This began after a power cycle; a full Android factory
reset stopped the failure. The pre-reset crash log was not preserved, so the original cause is
unknown. The `backboardSlides` name observed while restoring the hardware configuration was entered
after that reset and is unrelated to the earlier failure. Before the reset, the OnBotJava project
also contained a legacy `MarkII.java` file absent from this workspace; the exact set of compiled
OnBotJava classes at the time of failure is unknown. No OpMode has been run since the reset. If the
startup failure recurs, capture the Control Hub log before resetting it.

The unit tests run on a laptop without a robot: in Android Studio, right-click
`TeamCode/src/test` and choose Run Tests, or run `./gradlew :TeamCode:testDebugUnitTest`.

## Recorded hub port layout

This is the robot configuration supplied for the assembled robot. Port numbers are hub port
numbers; configuration names preserve the reported camel case.

| Hub | Device port | Configuration name |
| --- | --- | --- |
| Expansion Hub | Motor 0 | `frontLeft` |
| Expansion Hub | Motor 1 | `backLeft` |
| Expansion Hub | Motor 2 | `intake` |
| Expansion Hub | Motor 3 | `launcher` |
| Expansion Hub | Servo 1 | `rampLeft` |
| Expansion Hub | Servo 5 | `launcherGate` |
| Expansion Hub | Digital 0 | `odometryLeft` |
| Control Hub | Motor 0 | `backboardSlide` |
| Control Hub | Motor 1 | `turret` |
| Control Hub | Motor 2 | `backRight` |
| Control Hub | Motor 3 | `frontRight` |
| Control Hub | Servo 4 | `hood` |
| Control Hub | Servo 5 | `rampRight` |
| Control Hub | Digital 0 | `odometryRight` |
| Control Hub USB, top level | Ethernet Device | `limelight` (saved in `Mark III.xml`) |
| Control Hub | Built-in | `imu` |

The hub port layout is recorded in this repository; the Robot Controller app's active configuration
is stored on the robot and must use the same names and ports. `Mark III.xml` now has the top-level
USB-connected `EthernetDevice` saved as `limelight`, which lets `hardwareMap` find the Limelight 3A.
The camera remains optional while testing the other hardware. `hood` is mapped as a positional
`Servo`; its starting position is uncalibrated. The odometry devices are mapped as digital inputs,
but pod encoder counts are not decoded from them yet.

## Hardware contract

Every device below except `limelight` is required by `RobotHardware` and must have the exact Robot
Configuration name. If a required device is absent or has another device class, the OpMode stops at
init with one error listing the missing names. The Limelight 3A is optional until it is added to the
Robot Configuration as `limelight`.

| Configuration name | SDK type | Startup setup | Current use |
| --- | --- | --- | --- |
| `frontLeft` | `DcMotorEx` | `RUN_WITHOUT_ENCODER` (forward direction) | Mecanum drive |
| `frontRight` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backLeft` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backRight` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `intake` | `DcMotorEx` | `RUN_WITHOUT_ENCODER` | Runs during each feed pulse |
| `turret` | `DcMotorEx` | Encoder reset on first init, then `RUN_USING_ENCODER`; brakes at zero power | Aiming and Flower rotation |
| `backboardSlide` | `DcMotorEx` | Encoder reset on first init, then `RUN_USING_ENCODER`; brakes at zero power | Slide position and manual motion |
| `launcher` | `DcMotorEx` | `RUN_USING_ENCODER`; zero-power behavior `FLOAT` (coasts) | Launcher velocity control |
| `rampLeft` | `Servo` | Commanded to `RAMP_LEFT_STOW`; paired with the mirrored right target | Left side of ramp |
| `rampRight` | `Servo` | Commanded to `RAMP_RIGHT_STOW`; paired with the mirrored left target | Right side of ramp |
| `launcherGate` | `Servo` | Starts at `GATE_OPEN` during INIT; closes to `GATE_CLOSED` after start unless feeding | Ball gate |
| `hood` | `Servo` | Uses `HOOD_POSITION` only after calibration; target range is 0 through 1 | Hood mechanism |
| `odometryLeft` | `DigitalChannel` | Configured as `INPUT`; not decoded | Left odometry pod signal |
| `odometryRight` | `DigitalChannel` | Configured as `INPUT`; not decoded | Right odometry pod signal |
| `imu` | `IMU` | Logo up, USB forward; yaw reset at TeleOp start | Field centric drive heading |
| `limelight` | `Limelight3A` | Optional; pipeline selected when mapped | Fiducial target angle for turret |

Drive motor directions are set from the raised-wheel Motor Test observation recorded above, and
the user confirmed all four wheels spun forward with their assigned controls after the correction.
The mechanism motors use the SDK's default `FORWARD` direction; confirm their signs before tuning
motion targets. The IMU mounting orientation must be changed in `RobotHardware.initialize()` if
the Hub is installed differently.

### Odometry pods

The reported Robot Configuration names the digital devices `odometryLeft` (Expansion Hub Digital
0) and `odometryRight` (Control Hub Digital 0). The code maps them as `DigitalChannel` inputs but
does not decode pod encoder counts from them. Its older optional odometry reader reads counts from
`DcMotorEx` encoder ports, using `FORWARD_POD_MOTOR_NAME` and `STRAFE_POD_MOTOR_NAME` in
`RobotConfig`; both settings are currently empty. The eight motors already fill every motor port on
the two hubs, so the digital-device wiring needs an encoder-reading implementation before `Motor
Test` or a localizer can consume pod counts.

### Bulk reads

`RobotHardware.initialize()` puts every hub in manual bulk-caching mode. Each OpMode calls
`robot.clearBulkCache()` once at the top of its loop, and every encoder position and velocity in
that loop then comes from a single hub read. Any new loop must make the same call or its encoder
values will never update.

### Startup reference assumption

The first time an OpMode initializes after the Robot Controller app starts, the code resets the
slide and turret encoders and treats the current physical locations as zero: slide fully retracted
and turret at its known home angle. Later OpModes in the same app session keep that zero, so an
autonomous can hand over to TeleOp without the mechanisms being back at home. No home switch,
absolute encoder, or automatic homing sequence is mapped.

If either mechanism was not at home for that first init, its software position and all
encoder-based bounds are wrong. To fix it, move both to home and press Gamepad 1 `X` during
TeleOp init, which zeroes them again. The init screen shows the current readings and whether the
zero was set in this init or kept from earlier. `BioBuzz Tuning` always re-zeroes when it starts.

The current slide and turret limits are software checks. They do not detect a jam or protect against
encoder failure, incorrect motor polarity, loose couplings, or inertia past the boundary. The slide
has no programmed extension switch. The turret's ±360° limit assumes the encoder count maps correctly
to turret output angle.

## Driver controls

### Gamepad 1

| Input | Behavior |
| --- | --- |
| Left stick | Forward/back and strafe |
| Right stick X | Turn |
| Hold `RB` | Slow mode (`DRIVE_SLOW_SCALE`, default 40% speed) |
| `Y` | Toggle field centric / robot centric; starts field centric |
| `Back` | Reset field centric heading: the direction the robot faces now becomes field-forward |
| During init, D-pad left/right | Select Red / Blue and switch to that alliance's Limelight pipeline |
| During init, `X` | Re-zero the slide and turret encoders (both must be at home) |
| `Start` | Temporarily bypass Flower's time lockout while held |

The field centric mode resets IMU yaw when TeleOp starts, making the direction the robot faces at
start the field-forward reference. Alliance selection affects the Limelight pipeline and which
goal tag ID the turret tracks; it does not rotate the drive axes or change another robot
coordinate frame.

### Gamepad 2

| Input | Behavior |
| --- | --- |
| `A` / `B` / `X` / `Y` | Start 1 / 2 / 3 / 4-ball feed sequence (ignored while `Start` is held, so pairing with Start+A/B does not fire) |
| `LB` | Toggle Flower mode, subject to the time lockout when turning it on |
| `RB` | Toggle ramp between CAD stow and level/deployed targets |
| Hold left stick button + left stick Y | Manually extend/retract the slide |
| Hold left stick button + right stick X | Manually turn the turret |
| Hold left stick button + D-pad up/down | Command ramp level/stow |
| Hold left stick button + D-pad right/left | Open/close the gate; opening requires launcher-ready speed |
| `Back` | Cancel remaining feeds, close the gate, stop intake, and let the launcher coast down |

An analog stick movement or D-pad command while manual control is enabled cancels the current feed
sequence. Cancelling a sequence does not turn the launcher off; `Back` does. A newly selected ball
sequence replaces the current remaining count rather than appending to it.

After a manual slide or turret move, the mechanism holds where it was released. The turret then
moves again when the Limelight sees the goal tag (outside Flower mode); the slide stays put until
Flower is toggled.

## Runtime behavior

### Per-loop structure

`BioBuzzTeleOp` does not use blocking waits for mechanisms. After init it repeatedly:

1. Clears the bulk cache so every encoder is read fresh once.
2. Reads sticks and updates the mecanum drive powers.
3. Handles Flower and ramp toggle edges.
4. Applies manual ramp, gate, slide, and turret inputs; cancels feeding on manual mechanism input.
5. Moves the slide and turret toward their targets when no manual stick owns them, updating the
   turret target from the Limelight outside Flower mode.
6. Updates the launcher and advances the feed sequence.
7. Sends telemetry, including the loop time in milliseconds.

Keep new work per-loop and bounded so drive, feed timing, and safety checks continue to update.

### Drive

The three stick inputs pass through a deadband (`STICK_DEADBAND`), are scaled by the slow-mode
factor, and are mixed as mecanum forward, strafe, and turn terms, then normalized if any wheel
would exceed unit power. Field centric mode rotates forward/strafe by IMU yaw; turn remains a
robot-relative command. Motors run open loop without encoder velocity regulation and brake at zero
power.

### Slide and Flower

The slide is measured in encoder ticks relative to its zero. `SLIDE_MAX_TICKS` limits every target
and manual outward motion. Turning Flower on sets the slide target to `SLIDE_FLOWER_TICKS` and the
turret target to `TURRET_FLOWER_DEGREES`; turning it off sets both targets to zero. A proportional
power command drives toward the target and stops within `SLIDE_TOLERANCE_TICKS`. There is no
position hold against gravity within that tolerance and no motion profile.

Flower sets its targets once, when it is toggled. Manual adjustments made afterwards stay in place
rather than being pulled back to the Flower positions.

Flower activation is enabled when either the code switch `FLOWER_LOCKOUT_ENABLED` is false, Gamepad 1
`Start` is held, or the remaining time in the configured `TELEOP_DURATION_SECONDS` is at most
`FLOWER_UNLOCK_REMAINING_SECONDS`. Defaults are a 120-second TeleOp and a 60-second lockout. Turning
Flower off is always allowed. Ramp state is independent.

### Turret and Limelight

Turret angle is computed from encoder ticks using `TURRET_TICKS_PER_REVOLUTION`, interpreted as
encoder ticks per full turret output revolution. Every target is kept within ±
`TURRET_LIMIT_DEGREES`. A target just past a limit is replaced by the same direction one full turn
back (365° becomes 5°) when that fits inside the limits, so the turret unwinds directly onto the
target; otherwise the target stops at the limit. Power that would push further past a limit is
blocked, for manual and automatic motion alike.

The Limelight is mounted on the turret and turns with it. While not in Flower mode or manual turret
control, the OpMode inspects fiducials in the latest valid Limelight result. Only tags whose ID is
inside the alliance's range are considered (`RED_TAG_MIN_ID`-`RED_TAG_MAX_ID`, default 30-37;
`BLUE_TAG_MIN_ID`-`BLUE_TAG_MAX_ID`, default 38-45), and the one with the largest target area is
used. The tag's horizontal angle (`tx`) is multiplied by `LIMELIGHT_AIM_GAIN` and added to the
current turret angle.

BIOBUZZ has no single goal tag. Each HIVE CELL carries a sticker of four 3.25 in 36h11 tags:
RED SCORING 30-33, RED AUDIENCE 34-37, BLUE AUDIENCE 38-41, BLUE SCORING 42-45. The tags move when
the HIVE tips, so use them for aiming, not for robot position. To aim at one CELL only, narrow the
alliance's range to that sticker's four IDs. Set the tag size to 3.25 in in the Limelight's
AprilTag pipeline. If the turret turns away from the tag, make the gain negative.

The target is only updated when a new camera frame arrives, and frames older than
`LIMELIGHT_MAX_STALENESS_MS` are ignored. Re-applying one frame on every loop while the turret is
already turning makes it overshoot. If no tag is present, tracking keeps the last commanded turret
target. It does not estimate a field pose or range.

The Red and Blue pipeline settings both default to index `0`; set them to match the Limelight before
using alliance selection.

Flower mode takes priority over vision. When Flower is switched off, the turret target is set to
home; a currently visible tag can immediately replace it. If no tag is visible, it moves toward
home. This differs from ordinary tag-loss behavior, which holds the last target.

### Ramp and gate

The ramp's CAD pose is treated as stowed. The level pose is treated as deployed. The CAD panel angle
and 12-tooth/48-tooth gearing imply about 60° of servo-shaft travel, or one third of the nominal
0-to-1 servo range. The servos are mechanically linked and mirrored. `RobotTuning` changes each
stow or level setting as a pair: the right command is set to `1 - left`, and either bumper jogs
both outputs together. The current endpoints (`0.14/0.86` stow and `0.483/0.517` level) are
provisional until the horns are re-indexed and the mechanism is checked through its full motion.
All commands are clamped to 0 through 1.

The gate starts at `GATE_OPEN` (`0.38`) during INIT. It uses `GATE_CLOSED` (`0.11`) during normal
idle and after the OpMode stops. Normal TeleOp and timed feeding retain the launcher-ready check.
In `BioBuzz Tuning`, Circle manually opens the gate at any RPM for position testing; releasing it
returns the gate to the closed target. No gate position sensor is mapped.

### Launcher and feed sequence

Launcher RPM is estimated as `abs(getVelocity()) * 60 / (LAUNCHER_TICKS_PER_REVOLUTION *
LAUNCHER_GEAR_RATIO)`, with motor velocity reported in encoder ticks per second.
`LAUNCHER_TICKS_PER_REVOLUTION` is the encoder count per revolution of the bare motor shaft.
With `LAUNCHER_GEAR_RATIO` at `1.0`, every RPM figure is motor-shaft RPM; set it to the launcher
motor's gearbox ratio to work in wheel RPM. A wrong value changes both displayed RPM and the
gate-ready check.

Selecting a sequence switches the launcher on and sets the remaining ball count. The launcher is
held at `LAUNCHER_TARGET_RPM` by the hub's built-in velocity control (`setVelocity`), and it coasts
when switched off because its zero-power behavior is `FLOAT`. The hub's default velocity PIDF is
used unless `LAUNCHER_CUSTOM_PIDF` is true, in which case `LAUNCHER_P/I/D/F` are applied at init.
The gate-ready threshold is `LAUNCHER_READY_RPM_FRACTION` of the target.

| Feed state | Action |
| --- | --- |
| `IDLE` | No ball is being fed; gate is normally closed and intake stopped |
| `WAIT_READY` | Wait until measured launcher RPM reaches its ready threshold |
| `OPEN` | Open the gate and run intake for `FEED_OPEN_SECONDS` |
| `GAP` | Close gate, stop intake, and wait `FEED_GAP_SECONDS` before the next ball |

A pulse only starts while the launcher is ready. Once it has started it always runs for the full
open time and counts as one ball, even if launcher speed dips as the ball passes. The next ball
then waits for the gap and for speed to recover. Manual cancellation or `Back` clears the count,
closes the gate, and stops the intake. When a sequence finishes, the launcher keeps spinning until
`Back` switches it off.

## Calibration reference

The tunable values are public static fields in [`RobotConfig.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotConfig.java).
Units and code use are summarized here. The initial number is the checked-in starting point, not a
claim about the final robot.

| Setting | Initial value | Units / meaning |
| --- | ---: | --- |
| `STICK_DEADBAND` | `0.05` | Stick movement below this is ignored |
| `DRIVE_SLOW_SCALE` | `0.40` | Drive speed multiplier in slow mode |
| `FORWARD_POD_MOTOR_NAME`, `STRAFE_POD_MOTOR_NAME` | `""`, `""` | Motor encoder ports used by the existing optional pod reader; empty = disabled |
| `SLIDE_MAX_TICKS` | `900` | Maximum slide extension from zero |
| `SLIDE_FLOWER_TICKS` | `550` | Slide target in Flower mode |
| `SLIDE_KP` | `0.0030` | Slide power per encoder tick of position error |
| `SLIDE_MAX_POWER` | `0.65` | Automatic slide power cap |
| `SLIDE_MANUAL_POWER` | `0.45` | Manual slide power scale |
| `SLIDE_TOLERANCE_TICKS` | `8` | Error below which the slide stops driving |
| `TURRET_TICKS_PER_REVOLUTION` | `537.7` | Turret output encoder ticks per 360° |
| `TURRET_LIMIT_DEGREES` | `360` | Turret soft limit in either direction |
| `TURRET_FLOWER_DEGREES` | `45` | Flower turret target relative to home |
| `TURRET_KP` | `0.0060` | Turret power per degree of target error |
| `TURRET_MIN_POWER` | `0.0` | Minimum automatic output outside tolerance; zero until measured |
| `TURRET_MAX_POWER` | `0.45` | Automatic turret power cap |
| `TURRET_MANUAL_POWER` | `0.30` | Manual turret power scale |
| `TURRET_TOLERANCE_DEGREES` | `0.5` | Error below which the turret stops driving |
| `TURRET_TEST_STEP_DEGREES` | `1.0` | Relative target step for the tuning OpMode |
| `LIMELIGHT_AIM_GAIN` | `1.0` | Multiplier from Limelight `tx` degrees to turret target degrees; negative reverses it |
| `LIMELIGHT_MAX_STALENESS_MS` | `250` | Camera frames older than this are ignored |
| `RED_TAG_MIN_ID`, `RED_TAG_MAX_ID` | `30`, `37` | AprilTag IDs the turret aims at on Red |
| `BLUE_TAG_MIN_ID`, `BLUE_TAG_MAX_ID` | `38`, `45` | AprilTag IDs the turret aims at on Blue |
| `RED_LIMELIGHT_PIPELINE`, `BLUE_LIMELIGHT_PIPELINE` | `0`, `0` | Limelight pipeline indexes |
| `RAMP_LEFT_STOW`, `RAMP_LEFT_LEVEL` | `0.14`, `0.483` | Left reference values for paired ramp targets |
| `RAMP_RIGHT_STOW`, `RAMP_RIGHT_LEVEL` | `0.86`, `0.517` | Mirrored right ramp targets |
| `GATE_CLOSED`, `GATE_OPEN` | `0.11`, `0.38` | Measured gate endpoints |
| `HOOD_POSITION` | `-1.0` | Hood starting target; -1 = no startup target; tuning range is 0 down to 1 up |
| `LAUNCHER_TICKS_PER_REVOLUTION` | `28` | Launcher encoder ticks per bare motor-shaft revolution |
| `LAUNCHER_GEAR_RATIO` | `1.0` | Launcher gearbox ratio; `1.0` = RPM values are motor-shaft RPM |
| `LAUNCHER_TARGET_RPM` | `3000` | Launcher speed held while spinning |
| `LAUNCHER_READY_RPM_FRACTION` | `0.95` | Fraction of target RPM required before feeding |
| `LAUNCHER_CUSTOM_PIDF`, `LAUNCHER_P/I/D/F` | `false`, `10/3/0/0` | Optional replacement for the hub's default velocity PIDF |
| `FEED_OPEN_SECONDS`, `FEED_GAP_SECONDS` | `0.22`, `0.16` | Gate/intake pulse and between-ball delay |
| `INTAKE_POWER` | `0.80` | Intake motor power during an open pulse |
| `FLOWER_LOCKOUT_ENABLED` | `true` | Enables Flower time restriction |
| `TELEOP_DURATION_SECONDS` | `120` | Match time basis for lockout calculation |
| `FLOWER_UNLOCK_REMAINING_SECONDS` | `60` | Remaining-time threshold for Flower |

`RobotTuning` can adjust slide limits/gain, turret encoder ratio, limits, proportional gain, minimum
and maximum power, manual power, tolerance, test step, Flower target, paired ramp and gate endpoints,
launcher ratio/target RPM, and feed timings. It provides a hood jog over 0..1 and a gate test that
does not require launcher RPM. The tuner displays assignments to copy back into the config file.
Its encoder displays are useful for measuring travel: record home and endpoint ticks, then calculate
limits from the measured change rather than relying on the initial estimates.

### `BioBuzz Tuning` controls (Gamepad 1 only)

Gamepad 2 is not used by this OpMode. The right-trigger plus D-pad chord separates turret target
testing from the D-pad controls for selecting and changing settings.

| Action | Gamepad 1 control |
| --- | --- |
| Select setting | D-pad up/down |
| Change selected setting | D-pad left/right |
| Choose fine or coarse adjustment | Hold `X` for 0.1x; hold `Y` for 10x; otherwise 1x |
| Step turret target | Hold right trigger and tap D-pad left/right |
| Return turret target to zero | Hold right trigger and tap D-pad up |
| Jog slide | Hold left stick button and move left stick |
| Jog turret manually | Hold right stick button and move right stick X |
| Jog mirrored ramp pair | Hold `LB` or `RB` and move right stick Y; both targets change together |
| Jog hood | Hold left trigger and move right stick Y; 0 is down and 1 is up |
| Spin launcher | Hold `A`; releasing it stops drive power and the wheel coasts |
| Test gate open | Hold `B`; tuning opens it at any RPM, release to close |

For `TURRET_MIN_POWER`, a normal D-pad increment is 0.01, `X` makes it 0.001, and `Y` makes it
0.1. Use `X` for the breakaway test. `RobotTuning` re-zeroes the slide and turret every time it
starts, so place both at their home positions before selecting it. Values changed in the tuner are
live only until the Robot Controller app restarts; copy the displayed `RobotConfig` assignment into
the source to store a calibration.

## Tuning and commissioning sequence

1. **Prepare the robot.** Charge the battery, stop every OpMode, and check that no one is near the
   wheels, launcher, slide, turret, or servo linkages. Support the robot before running any drive
   motor test. Use no game piece for the first launcher check.
2. **Check configuration.** Confirm all required hardware names and SDK device classes above.
   Compare them with the recorded hub port layout, then confirm the IMU orientation, motor wiring,
   servo ports, and Limelight connection. The camera may be absent during mechanical bring-up.
3. **Deploy the bring-up sources.** After the Control Hub is powered back on, update
   `RobotTuning.java`, `Turret.java`, `PedroLocalizationCheck.java`, and `PedroPathingConfig.java`
   in OnBotJava, then press **Build Everything**. The debug APK builds locally, but cannot replace
   the installed FTC app because their signatures differ.
4. **Establish references.** With power removed or mechanisms safely supported, place the slide at
   full retraction and turret at the defined home angle before the first init after the app
   starts. `BioBuzz Tuning` re-zeroes them on each start, so do this every time before selecting
   the OpMode. Check that both encoder readings are zero before moving either mechanism.
5. **Check drive polarity.** The raised-wheel `Motor Test` now passes for all four wheels. Still
   check each stick direction and field centric mode at low power before normal floor driving.
6. **Check servo starting positions.** With the one-controller map above, jog the mirrored ramp pair
   with `LB`/`RB` and right stick Y. Jog the hood with left trigger and right stick Y. Move a little
   at a time, stop before any linkage binds, and record the usable positions. Copy the paired ramp
   and hood values into `RobotConfig` after confirming their mechanical range.
7. **Measure slide travel.** Hold the left stick button and use the left stick at low manual power.
   Check that extension increases the encoder count. Record home and the safe extension endpoint;
   set `SLIDE_MAX_TICKS` below the measured mechanical endpoint. Verify Flower extension and
   retraction have clearance before trying automatic moves.
8. **Measure turret ratio, minimum power, and signs.** Start near the defined home angle, keep the
   mechanism clear, and leave `TURRET_KP` unchanged while finding breakaway power. A one-degree
   target is only about 1.5 encoder ticks at the current 537.7 ticks/revolution estimate, so watch
   `Turret encoder ticks` and `Turret power request / minimum`, not just the rounded angle. In
   `BioBuzz Tuning`, hold right trigger and tap D-pad right once for a one-degree target, then hold
   right trigger and tap D-pad up to return to zero. Repeat on the left side and confirm the encoder
   moves in the expected direction. If neither side registers movement, select `TURRET_MIN_POWER`,
   hold `X`, and tap D-pad right once per retry; this raises the minimum by 0.001 each time. Record
   the lowest setting that moves consistently in both directions, then copy the reported assignment
   into `RobotConfig.java`.
   Stop and inspect the wiring, encoder, or mechanism if the requested power rises noticeably but
   the tick count still does not change. After the ratio and sign are confirmed, tune `TURRET_KP`
   separately using small target steps and inspect for overshoot or repeated oscillation. Only then
   test the soft limits and return-to-home behavior with a spotter. The 537.7 ticks/revolution value
   is still an estimate until a measured turret rotation confirms it.
9. **Calibrate the launcher and gate.** Hold `A` with the wheel clear and no game piece loaded;
   watch RPM and ready status. Tune the target and encoder ratio from measured behavior, not the
   starting estimates. Hold `B` only after ready status to verify the gate opens and closes. Once
   that passes, stop Tuning and use `BioBuzz TeleOp` to test one feed pulse with one game piece;
   its feed-sequence request is on Gamepad 2. A second controller is needed for that check. Record
   the safe speed, ready threshold, open time, and gap before trying multi-ball sequences.
10. **Check Limelight aiming.** Confirm the `limelight` device is mapped, then set Red/Blue pipeline
   indexes and 3.25 in tag size. Check selected tag, aim direction, stale-frame rejection, loss hold,
   and interaction with manual turret control. Flip `LIMELIGHT_AIM_GAIN` if the turret turns away.
11. **Collect a foam-tile Pedro baseline.** Measure the tile's actual length and mark a clear start
   pose facing Pedro +X. Run `Pedro Localization Check` with Gamepad 1 left bumper held for its 8%
   setting. Move forward along the tile by a measured distance, restart the check at the same origin,
   then measure strafe separately. Use measured distance divided by the matching Pedro pose delta
   for the forward and strafe scales; check that positive stick motion gives the expected sign. The
   telemetry reports wheel tick deltas since OpMode start. Measure the drive wheel center-to-center
   width and length. For one measured turn, calculate
   `TURN_TICKS_TO_INCHES = turnRadians * (ROBOT_WIDTH_INCHES + ROBOT_LENGTH_INCHES) / signedTurnTicks`,
   where `signedTurnTicks = -frontLeft + frontRight - backLeft + backRight` from the reported
   deltas. Set the sign so a positive turn command produces the expected Pedro heading. Repeat the
   checks, then save the foam-tile origin and scales in `PedroPathingConfig`. The foam tile gives a
   rough local baseline; verify its dimensions and retune the start pose and scales on the official
   field.
12. **Run integrated TeleOp checks.** Verify field/robot centric switch, heading reset, slow mode,
   alliance selection, Flower lockout and Start override, independent ramp behavior, turret
   unwinding at both limits, all feed counts, cancellation, and Back spin-down. Use two gamepads for
   the full TeleOp input check, and watch the loop time on telemetry.

Compilation does not verify any of these physical behaviors. The unit tests cover only the drive
math, turret target math, and feed sequence; the mechanisms and acceptance scenarios still require
on-robot commissioning.

## Autonomous and localization status

The recorded odometry digital inputs are not decoded as pod counts. `Pedro Localization Check` uses
the four drive motor encoders through PedroPathing 2.1.2's `DriveEncoderLocalizer` and starts at
`(0, 0, 0)` on the foam tile. Its forward and strafe scales are one until measured; with those
provisional values, pose deltas are tick-equivalent units. `TURN_TICKS_TO_INCHES` is zero, so
heading stays fixed until a measured turn and wheel geometry establish the scale. PedroPathing 2.1.2
computes the heading increment as
`TURN_TICKS_TO_INCHES * (-frontLeft + frontRight - backLeft + backRight) / (ROBOT_WIDTH_INCHES + ROBOT_LENGTH_INCHES)`.
The mode reports encoder deltas since it started and is for slow, manual data collection only, not
autonomous navigation. Record the actual foam-tile measurements, set `DRIVE_SCALE_CALIBRATED` only
after all scales and wheel dimensions are confirmed, and repeat the checks. The autonomous shell
remains disabled with no path. Translate the local tile origin to a season-field starting pose only
after the field geometry is available, and retune on the official field.

`TeamCode/build.gradle` currently uses `com.pedropathing:ftc:2.1.2`. PedroPathing 3.0.2 is now
available from the Dairy Foundation Maven repository as `com.pedropathing:revhub:3.0.2`, with
`com.pedropathing:tuning:1.0.0` for its AutoTune procedures. Version 3 has a redesigned API, so this
project remains on 2.1.2 until its follower/localizer setup is deliberately migrated and verified.
The autonomous shell still has no follower or localization configuration, and remains disabled.
