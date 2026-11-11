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
| Intake and gate | Ball transfer into the launcher | Intake runs during timed pulses; gate is closed between pulses and until ready |
| Ramp | Two mirrored servos | Independent calibrated stow and level positions |
| Odometry | One forward pod and one strafe pod, read through spare motor encoder ports | Optional; mapped when the ports are named, not yet consumed by a localizer |
| IMU and Limelight | Heading and fiducial sensing | Yaw for field centric drive; goal tag horizontal angle for turret aim |

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
| [`PedroAutoShell.java`](src/main/java/org/firstinspires/ftc/teamcode/PedroAutoShell.java) | Disabled autonomous placeholder with no movement path |
| [`TeamCode/build.gradle`](build.gradle) | TeamCode module dependencies |

`RobotConfig` is the source of truth used by both TeleOp and the tuner. The tuner changes static
fields while the app process is running; changes are lost when the Robot Controller app restarts.
Copy useful values from the tuner's telemetry back into `RobotConfig.java` to keep them.

The unit tests run on a laptop without a robot: in Android Studio, right-click
`TeamCode/src/test` and choose Run Tests, or run `./gradlew :TeamCode:testDebugUnitTest`.

## Hardware contract

Every device below is required by `RobotHardware` and must have the exact Robot Configuration name.
If any are absent or configured as another device class, the OpMode stops at init with one error
that lists every missing name.

| Configuration name | SDK type | Startup setup | Current use |
| --- | --- | --- | --- |
| `frontLeft` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `frontRight` | `DcMotorEx` | Forward; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backLeft` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backRight` | `DcMotorEx` | Forward; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `intake` | `DcMotorEx` | `RUN_WITHOUT_ENCODER` | Runs during each feed pulse |
| `turret` | `DcMotorEx` | Encoder reset on first init, then `RUN_USING_ENCODER`; brakes at zero power | Aiming and Flower rotation |
| `backboardSlide` | `DcMotorEx` | Encoder reset on first init, then `RUN_USING_ENCODER`; brakes at zero power | Slide position and manual motion |
| `launcher` | `DcMotorEx` | `RUN_USING_ENCODER`; zero-power behavior `FLOAT` (coasts) | Launcher velocity control |
| `rampLeft` | `Servo` | Commanded to `RAMP_LEFT_STOW` | Left side of ramp |
| `rampRight` | `Servo` | Commanded to `RAMP_RIGHT_STOW` | Right side of ramp |
| `launcherGate` | `Servo` | Commanded to `GATE_CLOSED` | Ball gate |
| `imu` | `IMU` | Logo up, USB forward; yaw reset at TeleOp start | Field centric drive heading |
| `limelight` | `Limelight3A` | Pipeline selected and polling started | Fiducial target angle for turret |

Drive motor directions are the only motor directions explicitly configured. The mechanism motors
use the SDK's default `FORWARD` direction; confirm the signs on the robot before tuning motion
targets. The IMU mounting orientation must be changed in `RobotHardware.initialize()` if the Hub is
installed differently.

### Odometry pods

The robot has two goBILDA odometry pods mounted 90 degrees apart: one measures forward travel and
one measures strafe. A pod has no motor, so it cannot have its own entry in the Robot
Configuration. Its cable plugs into the encoder port of an existing motor, and the code reads it
through that motor. The eight motors already fill every motor port on the two hubs.

Set `FORWARD_POD_MOTOR_NAME` and `STRAFE_POD_MOTOR_NAME` in `RobotConfig` to the configuration
name of the motor whose encoder port each pod uses. Only `frontLeft`, `frontRight`, `backLeft`,
`backRight`, and `intake` are allowed, because those motors run without their own encoder; the
turret, slide, and launcher ports carry the encoders those mechanisms need. While both names are
empty the robot runs normally without odometry. `Motor Test` shows the pod counts once they are
set.

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
0-to-1 servo range. The current left and right endpoint values are separate and mirrored; all
commands are clamped to 0 through 1. These values are initial estimates, not measured PWM endpoints.

The torque-servo gate has independent closed and open targets. It remains commanded closed unless
launcher RPM reaches the configured ready fraction or manual gate control requests open while ready.
No gate position sensor is mapped.

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
| `FORWARD_POD_MOTOR_NAME`, `STRAFE_POD_MOTOR_NAME` | `""`, `""` | Motor whose encoder port each odometry pod uses; empty = not installed |
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
| `TURRET_MAX_POWER` | `0.45` | Automatic turret power cap |
| `TURRET_MANUAL_POWER` | `0.30` | Manual turret power scale |
| `TURRET_TOLERANCE_DEGREES` | `0.5` | Error below which the turret stops driving |
| `LIMELIGHT_AIM_GAIN` | `1.0` | Multiplier from Limelight `tx` degrees to turret target degrees; negative reverses it |
| `LIMELIGHT_MAX_STALENESS_MS` | `250` | Camera frames older than this are ignored |
| `RED_TAG_MIN_ID`, `RED_TAG_MAX_ID` | `30`, `37` | AprilTag IDs the turret aims at on Red |
| `BLUE_TAG_MIN_ID`, `BLUE_TAG_MAX_ID` | `38`, `45` | AprilTag IDs the turret aims at on Blue |
| `RED_LIMELIGHT_PIPELINE`, `BLUE_LIMELIGHT_PIPELINE` | `0`, `0` | Limelight pipeline indexes |
| `RAMP_LEFT_STOW`, `RAMP_LEFT_LEVEL` | `0.14`, `0.483` | Left servo stowed and deployed positions |
| `RAMP_RIGHT_STOW`, `RAMP_RIGHT_LEVEL` | `0.86`, `0.517` | Right servo stowed and deployed positions |
| `GATE_CLOSED`, `GATE_OPEN` | `0.12`, `0.72` | Gate servo endpoints |
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

`RobotTuning` can directly adjust slide maximum and Flower position/gain, turret encoder ratio/limits/
Flower position/gain, the four ramp endpoints, gate endpoints, launcher encoder ratio/target RPM, and
the feed open/gap times. Other values remain code-tuned. The tuner displays an assignment to copy
back into the config file. Its encoder displays are useful for measuring travel: record home and
endpoint ticks, then calculate the limit from the measured change rather than relying on the initial
estimate.

## Tuning and commissioning sequence

1. **Check configuration.** Confirm all 13 hardware names and SDK device classes above. Confirm the
   Hub orientation, motor wiring, servo ports, and Limelight connection.
2. **Establish references.** With power removed or mechanisms safely supported, place the slide at
   full retraction and turret at the defined home angle before the first init after the app
   starts. If the init screen does not read 0 for both, press Gamepad 1 `X` with them at home.
3. **Check drive polarity.** Raise the robot safely and run `Motor Test` to verify each wheel
   direction at low power, then check each stick direction and the field centric mode. Correct drive directions or motor connections before floor testing.
4. **Measure slide travel.** Use tuning mode at low manual power. Record the encoder delta from home
   to the safe full-extension point; set `SLIDE_MAX_TICKS`. Verify Flower extension and automatic
   retraction have clearance.
5. **Measure turret ratio and signs.** Move a known angle at low manual power. Derive encoder ticks
   per output-shaft revolution and verify the positive encoder sign corresponds to the documented
   positive rotation. Test both soft limits and return-to-home behavior with a spotter.
6. **Calibrate ramp and gate endpoints.** Jog each side independently, record no-bind stow/level
   positions, and ensure no servo is commanded beyond a safe mechanical stop. Verify the gate closes
   fully and only opens at ready speed.
7. **Calibrate launcher and feed.** Check encoder ticks per motor revolution, measure actual speed,
   and tune the lowest reliable launch speed. Then tune readiness threshold, pulse duration, and gap
   with one ball before trying multi-ball sequences.
8. **Configure Limelight.** Set Red/Blue pipeline indexes and a 3.25 in tag size, and confirm the
   camera can see the tags under the CELLS from shooting positions. Check aim
   direction (flip the sign of `LIMELIGHT_AIM_GAIN` if the turret turns away), selected tag, loss
   hold, and interaction with manual control.
9. **Run integrated checks.** Verify field/robot centric switch, heading reset, slow mode, alliance
   selection, Flower lockout and Start override, independent ramp behavior, turret unwinding at both
   limits, all feed counts, cancellation, and Back spin-down. Watch the loop time on telemetry.

Compilation does not verify any of these physical behaviors. The unit tests cover only the drive
math, turret target math, and feed sequence; the mechanisms and acceptance scenarios still require
on-robot commissioning.

## Autonomous and localization status

The forward and strafe pods are readable once their ports are named in `RobotConfig`, but nothing
uses them for localization yet. With one forward pod, one strafe pod, and the IMU for heading, the
matching PedroPathing setup is its two-wheel localizer. The autonomous shell is disabled
and issues no drive commands. Add a localizer only after encoder polarity, wheel geometry, and sensor
placement are measured. Add paths after field coordinates, start pose, and alliance transforms are
defined and tested.

`TeamCode/build.gradle` currently uses `com.pedropathing:ftc:2.1.2`. The requested `3.0.2` artifact
was not available in Maven Central or the PedroPathing Maven repository when this foundation was
added. The autonomous shell has no PedroPathing API setup yet, so its follower/localizer construction
still needs to be implemented against whichever library version the project adopts.
