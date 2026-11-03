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
| Mecanum drive | Four independently driven wheels | Field centric or robot centric open-loop mixing |
| Backboard slide | A linear mechanism named `backboardSlide` | Encoder-referenced extension with a software travel cap |
| Turret | A rotating aiming mechanism | Encoder angle, soft limits, tag tracking, Flower target, manual override |
| Launcher | Ball launching motor | Encoder-derived RPM, coast hysteresis or optional continuous correction |
| Intake and gate | Ball transfer into the launcher | Intake runs during timed pulses; gate is closed between pulses and until ready |
| Ramp | Two mirrored servos | Independent calibrated stow and level positions |
| Odometry | `odoLeft` and `odoRight` encoder devices | Mapped, but not yet consumed by a localizer |
| IMU and Limelight | Heading and fiducial sensing | Yaw for field centric drive; HIVE tag horizontal angle for turret aim |

The Flower mechanism is a coordinated slide and turret target. Flower mode does not own the ramp;
the ramp remains an independent mechanism.

## Source map

| File | Responsibility |
| --- | --- |
| [`RobotHardware.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotHardware.java) | Device lookup, motor modes and directions, encoder zeroing, startup servo positions, and IMU orientation |
| [`RobotConfig.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotConfig.java) | Shared calibration values and behavior switches |
| [`BioBuzzTeleOp.java`](src/main/java/org/firstinspires/ftc/teamcode/BioBuzzTeleOp.java) | Driver inputs, drive mix, alliance selection, target handling, and non-blocking mechanism state machines |
| [`RobotTuning.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotTuning.java) | Bring-up OpMode for live numeric changes and guarded mechanism movement |
| [`PedroAutoShell.java`](src/main/java/org/firstinspires/ftc/teamcode/PedroAutoShell.java) | Disabled autonomous placeholder with no movement path |
| [`TeamCode/build.gradle`](build.gradle) | TeamCode module dependencies |

`RobotConfig` is the source of truth used by both TeleOp and the tuner. The tuner changes static
fields while the app process is running; changes are lost when the Robot Controller app restarts.
Copy useful values from the tuner's telemetry back into `RobotConfig.java` to keep them.

## Hardware contract

Every device below is required by `RobotHardware` and must have the exact Robot Configuration name.
The OpMode fails during mapping if one is absent or configured as another device class.

| Configuration name | SDK type | Startup setup | Current use |
| --- | --- | --- | --- |
| `frontLeft` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `frontRight` | `DcMotorEx` | Forward; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backLeft` | `DcMotorEx` | Direction reversed; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `backRight` | `DcMotorEx` | Forward; `RUN_WITHOUT_ENCODER` | Mecanum drive |
| `intake` | `DcMotorEx` | `RUN_WITHOUT_ENCODER` | Runs during each feed pulse |
| `turret` | `DcMotorEx` | Encoder reset, then `RUN_USING_ENCODER` | Aiming and Flower rotation |
| `backboardSlide` | `DcMotorEx` | Encoder reset, then `RUN_USING_ENCODER` | Slide position and manual motion |
| `launcher` | `DcMotorEx` | `RUN_USING_ENCODER` | Launcher speed measurement and hold |
| `rampLeft` | `Servo` | Commanded to `RAMP_LEFT_STOW` | Left side of ramp |
| `rampRight` | `Servo` | Commanded to `RAMP_RIGHT_STOW` | Right side of ramp |
| `launcherGate` | `Servo` | Commanded to `GATE_CLOSED` | Ball gate |
| `odoLeft` | `DcMotorEx` | `RUN_WITHOUT_ENCODER`; not zeroed | Reserved odometry encoder |
| `odoRight` | `DcMotorEx` | `RUN_WITHOUT_ENCODER`; not zeroed | Reserved odometry encoder |
| `imu` | `IMU` | Logo up, USB forward; yaw reset at TeleOp start | Field centric drive heading |
| `limelight` | `Limelight3A` | Pipeline selected and polling started | Fiducial target angle for turret |

Drive motor directions are the only motor directions explicitly configured. The mechanism motors
use the SDK's default `FORWARD` direction; confirm the signs on the robot before tuning motion
targets. The IMU mounting orientation must be changed in `RobotHardware.initialize()` if the Hub is
installed differently.

### Startup reference assumption

At OpMode initialization, the code resets the slide and turret encoders and treats the current
physical locations as zero: slide fully retracted and turret at its known home angle. No home switch,
absolute encoder, or automatic homing sequence is mapped. If either mechanism is not physically at
home before init, its software position and all encoder-based bounds will be wrong for that run.

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
| `Y` | Toggle field centric / robot centric; starts field centric |
| During init, D-pad left/right | Select Red / Blue and switch to that alliance's Limelight pipeline |
| `Start` | Temporarily bypass Flower's time lockout while held |

The field centric mode resets IMU yaw when TeleOp starts, making the direction the robot faces at
start the field-forward reference. Alliance selection currently affects the Limelight pipeline; it
does not rotate the drive axes or change another robot coordinate frame.

### Gamepad 2

| Input | Behavior |
| --- | --- |
| `A` / `B` / `X` / `Y` | Start 1 / 2 / 3 / 4-ball feed sequence |
| `LB` | Toggle Flower mode, subject to the time lockout when turning it on |
| `RB` | Toggle ramp between CAD stow and level/deployed targets |
| Hold left stick button + left stick Y | Manually extend/retract the slide |
| Hold left stick button + right stick X | Manually turn the turret |
| Hold left stick button + D-pad up/down | Command ramp level/stow |
| Hold left stick button + D-pad right/left | Open/close the gate; opening requires launcher-ready speed |
| `Back` | Cancel remaining feeds, close the gate, stop intake, and turn launcher hold off |

An analog stick movement or D-pad command while manual control is enabled cancels the current feed
sequence. Cancelling a sequence does not turn the launcher off; `Back` does. A newly selected ball
sequence replaces the current remaining count rather than appending to it.

## Runtime behavior

### Per-loop structure

`BioBuzzTeleOp` does not use blocking waits for mechanisms. After init it repeatedly:

1. Reads sticks and updates the mecanum drive powers.
2. Handles Flower and ramp toggle edges.
3. Reads the latest Limelight result and updates the turret target unless manual aiming is active.
4. Applies manual slide, turret, gate, and ramp inputs; cancels feeding on manual mechanism input.
5. Updates launcher hold and advances the feed state machine.
6. Moves the slide and turret toward their active targets, unless a manual stick currently owns them.
7. Sends telemetry.

This order matters when extending the controls: manual feed cancellation runs before feed progression,
and mechanism target motion runs after manual motor commands. Keep new work per-loop and bounded so
drive, feed timing, and safety checks continue to update.

### Drive

The four stick inputs are mixed as mecanum forward, strafe, and turn terms, then normalized if any
wheel would exceed unit power. Field centric mode rotates forward/strafe by IMU yaw; turn remains a
robot-relative command. Motors run open loop without encoder velocity regulation.

### Slide and Flower

The slide is measured in encoder ticks relative to the startup zero. `SLIDE_MAX_TICKS` limits the
commanded Flower target and manual outward motion. When Flower is on, slide target is
`SLIDE_FLOWER_TICKS`; when off, its target is zero. A proportional power command drives toward the
target and stops within an 8-tick deadband. There is no position hold against gravity within that
deadband and no motion profile.

Flower activation is enabled when either the code switch `FLOWER_LOCKOUT_ENABLED` is false, Gamepad 1
`Start` is held, or the remaining time in the configured `TELEOP_DURATION_SECONDS` is at most
`FLOWER_UNLOCK_REMAINING_SECONDS`. Defaults are a 120-second TeleOp and a 60-second lockout. Turning
Flower off is always allowed. Slide and turret targets are updated concurrently in the same loop.
Ramp state is independent.

### Turret and Limelight

Turret angle is computed from encoder ticks using `TURRET_TICKS_PER_REVOLUTION`, interpreted as
encoder ticks per full turret output revolution. The normal target is clamped to ±
`TURRET_LIMIT_DEGREES`. When measured angle reaches either limit, automatic control overrides the
aim target and drives back toward zero at `TURRET_RETURN_POWER`; after entering the home tolerance,
normal target control resumes. Manual stick control can move inward from a limit but is blocked
outward at or beyond it.

While not in Flower mode or manual turret control, the OpMode inspects fiducials in the latest valid
Limelight result. The detection with the largest target area is treated as nearest, and its
horizontal target angle (`tx`) is multiplied by `LIMELIGHT_AIM_GAIN` and added to the current turret
angle. This assumes each selected Limelight pipeline only returns HIVE tags. If no tag is present,
normal tracking keeps the last commanded turret target. It does not estimate a field pose or range.

The Red and Blue pipeline settings both default to index `0`; set them to match the Limelight before
using alliance selection. The code does not contain a HIVE tag ID list or explicitly filter tag IDs.
The area-based nearest-tag choice is an approximation: tag area changes with range, view angle, and
occlusion. For more reliable selection, use calibrated tag pose/range or a known ID and alliance
filter.

Flower mode takes priority over vision. When Flower is switched off, the turret target is set to
home before that loop's tracking update; a currently visible tag can immediately replace it. If no
tag is visible, it moves toward home. This differs from ordinary tag-loss behavior, which holds the
last target.

### Ramp and gate

The ramp's CAD pose is treated as stowed. The level pose is treated as deployed. The CAD panel angle
and 12-tooth/48-tooth gearing imply about 60° of servo-shaft travel, or one third of the nominal
0-to-1 servo range. The current left and right endpoint values are separate and mirrored; all
commands are clamped to 0 through 1. These values are initial estimates, not measured PWM endpoints.

The torque-servo gate has independent closed and open targets. It remains commanded closed unless
launcher RPM reaches the configured ready fraction or manual gate control requests open while ready.
No gate position sensor is mapped.

### Launcher and feed sequence

Launcher RPM is estimated as `abs(getVelocity()) * 60 / LAUNCHER_TICKS_PER_REVOLUTION`, with motor
velocity reported in encoder ticks per second. Validate the ticks-per-revolution value against the
actual motor/gear configuration; a wrong value changes both displayed RPM and the gate-ready check.

Selecting a sequence starts launcher hold and sets the remaining ball count. The default hold mode
is hysteresis/coast: launcher power turns on below the restart fraction and turns off above the
cutoff fraction. The gate-ready threshold is independently based on the ready fraction. Set
`LAUNCHER_CONTINUOUS_VELOCITY_HOLD` true to compare the simple continuous correction mode, which
adjusts power using `LAUNCHER_CONTINUOUS_KP` around `LAUNCHER_POWER`.

| Feed state | Action |
| --- | --- |
| `IDLE` | No ball is being fed; gate is normally closed and intake stopped |
| `WAIT_READY` | Wait until measured launcher RPM reaches its ready threshold |
| `OPEN` | Open the gate and run intake for `FEED_OPEN_SECONDS` while launcher remains ready |
| `GAP` | Close gate, stop intake, and wait `FEED_GAP_SECONDS` before the next ball |

If readiness drops during an open pulse, the gate and intake close and the sequence waits for
readiness again. The ball count is decremented only after a complete open pulse. Manual cancellation
or `Back` clears the count, closes the gate, and stops the intake. When a sequence finishes, launcher
hold remains enabled until `Back` turns it off.

## Calibration reference

The tunable values are public static fields in [`RobotConfig.java`](src/main/java/org/firstinspires/ftc/teamcode/RobotConfig.java).
Units and code use are summarized here. The initial number is the checked-in starting point, not a
claim about the final robot.

| Setting | Initial value | Units / meaning |
| --- | ---: | --- |
| `SLIDE_MAX_TICKS` | `900` | Maximum slide extension from startup zero |
| `SLIDE_FLOWER_TICKS` | `550` | Slide target in Flower mode |
| `SLIDE_KP` | `0.0030` | Slide power per encoder tick of position error |
| `SLIDE_MAX_POWER` | `0.65` | Automatic slide power cap |
| `SLIDE_MANUAL_POWER` | `0.45` | Manual slide power scale |
| `TURRET_TICKS_PER_REVOLUTION` | `537.7` | Turret output encoder ticks per 360° |
| `TURRET_LIMIT_DEGREES` | `360` | Turret soft limit in either direction |
| `TURRET_FLOWER_DEGREES` | `45` | Flower turret target relative to home |
| `TURRET_KP` | `0.0060` | Turret power per degree of target error |
| `TURRET_MAX_POWER` | `0.45` | Automatic turret power cap |
| `TURRET_RETURN_POWER` | `0.55` | Power while returning to home after a soft limit |
| `TURRET_MANUAL_POWER` | `0.30` | Manual turret power scale |
| `TURRET_HOME_TOLERANCE_DEGREES` | `5` | Error around home that ends auto-return |
| `LIMELIGHT_AIM_GAIN` | `1.0` | Multiplier from Limelight `tx` degrees to turret target degrees |
| `RAMP_LEFT_STOW`, `RAMP_LEFT_LEVEL` | `0.14`, `0.483` | Left servo stowed and deployed positions |
| `RAMP_RIGHT_STOW`, `RAMP_RIGHT_LEVEL` | `0.86`, `0.517` | Right servo stowed and deployed positions |
| `GATE_CLOSED`, `GATE_OPEN` | `0.12`, `0.72` | Gate servo endpoints |
| `LAUNCHER_TICKS_PER_REVOLUTION` | `28` | Launcher encoder ticks per motor revolution for RPM conversion |
| `LAUNCHER_TARGET_RPM` | `3000` | Target speed used for hold thresholds |
| `LAUNCHER_READY_RPM_FRACTION` | `0.95` | Fraction of target RPM required before feeding |
| `LAUNCHER_RESTART_RPM_FRACTION` | `0.90` | Hysteresis fraction that turns motor power back on |
| `LAUNCHER_CUTOFF_RPM_FRACTION` | `1.00` | Hysteresis fraction that coasts the launcher |
| `LAUNCHER_POWER` | `0.85` | Motor power during hysteresis hold and continuous-mode baseline |
| `LAUNCHER_CONTINUOUS_KP` | `0.0008` | Continuous correction per RPM error |
| `LAUNCHER_CONTINUOUS_VELOCITY_HOLD` | `false` | False: hysteresis/coast; true: continuous correction |
| `FEED_OPEN_SECONDS`, `FEED_GAP_SECONDS` | `0.22`, `0.16` | Gate/intake pulse and between-ball delay |
| `INTAKE_POWER` | `0.80` | Intake motor power during an open pulse |
| `RED_LIMELIGHT_PIPELINE`, `BLUE_LIMELIGHT_PIPELINE` | `0`, `0` | Limelight pipeline indexes |
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

1. **Check configuration.** Confirm all 15 hardware names and SDK device classes above. Confirm the
   Hub orientation, motor wiring, servo ports, and Limelight connection.
2. **Establish references.** With power removed or mechanisms safely supported, place the slide at
   full retraction and turret at the defined home angle before entering init. This is required each
   time because init resets their encoder readings.
3. **Check drive polarity.** Raise the robot safely and verify each stick direction and wheel
   direction at low power. Correct drive directions or motor connections before floor testing.
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
8. **Configure Limelight.** Set Red/Blue pipeline indexes and verify those pipelines expose HIVE
   fiducials only. Check aim direction, selected tag, loss hold, and interaction with manual control.
9. **Run integrated checks.** Verify field/robot centric switch, alliance selection, Flower lockout
   and Start override, independent ramp behavior, turret recovery at both limits, all feed counts,
   cancellation, and Back spin-down.

Compilation does not verify any of these physical behaviors. The initial code compiled against the
current project setup; the mechanisms and acceptance scenarios still require on-robot commissioning.

## Autonomous and localization status

`odoLeft` and `odoRight` are mapped but not used for localization. The autonomous shell is disabled
and issues no drive commands. Add a localizer only after encoder polarity, wheel geometry, and sensor
placement are measured. Add paths after field coordinates, start pose, and alliance transforms are
defined and tested.

`TeamCode/build.gradle` currently uses `com.pedropathing:ftc:2.1.2`. The requested `3.0.2` artifact
was not available in Maven Central or the PedroPathing Maven repository when this foundation was
added. The autonomous shell has no PedroPathing API setup yet, so its follower/localizer construction
still needs to be implemented against whichever library version the project adopts.
