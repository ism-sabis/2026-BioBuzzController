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
| [`EncoderCheck.java`](src/main/java/org/firstinspires/ftc/teamcode/EncoderCheck.java) | Displays all eight motor encoder counts and applies short, one-at-a-time test pulses |
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

The launcher gate was measured at `0.11` closed (blocks balls) and `0.38` open. The user confirmed
the hood range is `0.0` fully down to `1.0` maximum up; `1.0` is the requested startup position,
while the shooting angle remains uncalibrated. Ramp endpoints still need final mechanical
verification; other configuration values remain estimates.

**2026-11-25 — Tuning and Android Studio deployment:** The telemetry separates turret target,
measured angle, error, encoder ticks, profile speed, and normalized power request. The tuner runs
from Gamepad 1 alone. `:TeamCode:assembleDebug` builds the Robot Controller APK, and ADB installs it
on the Control Hub; see [TeamCode README](README.md#build-and-deploy-with-adb) for the PowerShell
commands. The first switch from the stock FTC-signed app required a one-time uninstall because the
signatures differed. Later APKs using the same project key install with `adb install -r`. During the
first replacement on this robot, its SSID remained `20037-RC` and ADB stayed connected, so verify
the network after replacement instead of assuming its credentials reset. The project source is now
the Android Studio workspace; OnBotJava **Build Everything** is not part of this deployment path.

**2026-11-25 — Historical OnBotJava connection check:** The old OnBotJava page at `192.168.43.1:8080` reported **Robot Controller disconnected** and files read-only. That check applied to the previous OnBotJava workflow; the active project deployment path is the Android Studio APK installed through ADB, documented in `TeamCode/README.md`.

**2026-11-25 — Turret endpoint correction:** The user confirmed the software stops should match the
The user originally reported -210 degrees CCW and +90 degrees CW, then corrected the positive mechanical endpoint to +180 degrees.
Current workspace software limits use -210 and +180 degrees with no margin. The Hub readback confirmed the file saved.
The user confirmed no OpMode was running, then OnBotJava Build Everything succeeded in 6.8 seconds.
The angle-to-encoder scale still needs physical validation, because any scale or zero error moves the effective physical stop.

**2026-11-25 — Control Hub startup failure (post-reset recurrence diagnosed and fixed):** The
Robot Controller app reached **Initializing Robot** and failed before any OpMode was selected or
run. Logcat captured an uncaught `NoClassDefFoundError` in
`FetchAutocompleteJavaScript.parseClassForAutocomplete`. The Control Hub v1.0 runs Android 7.1.2/API
25; PedroPathing 2.1.2's Kotlin stdlib 2.1.20 external library included optional `kotlin.io.path`
methods that reference `java.nio.file.Path`, plus `kotlin.time.jdk8` conversions that reference
`java.time.Duration` and `java.time.Instant`. Those platform types are absent on API 25. OnBotJava
12 reflects external-library method signatures for `/java/js/editor/autocomplete`; the missing
types caused an uncaught exception that killed the Robot Controller process, after which RobotCore
reported `EMERGENCY STOP, unable to start robot`. The earlier partial filter removed only
`kotlin.io.path`, which is why the next captured exception named `java.time.Duration`. The separate
`META-INF.versions.9.module-info` class lookup was caught during startup and was not the fatal error.

The Hub now has an API 25-compatible Kotlin JAR and matching DEX. The filter removes
`kotlin/io/path/**`, `kotlin/time/jdk8/DurationConversionsJDK8Kt.class`,
`kotlin/time/jdk8/InstantConversionsJDK8Kt.class`, and `META-INF/versions/**`; the JAR manifest no
longer declares `Multi-Release`. The DEX was built for min API 25 and is DEX version 037. The active
files are `/sdcard/FIRST/ExternalLibraries/kotlin_stdlib_2_1_20.jar` and
`/sdcard/FIRST/ExternalLibraries/kotlin_stdlib_2_1_20_jar/dex/classes.dex`. A pre-fix copy is at
`/sdcard/FIRST/ExternalLibraries_backup_2026-11-25/before_api25_compat_fix/`; the original Maven
JAR and DEX are also preserved in the surrounding backup directory. Do not replace the active pair
with the unfiltered Kotlin 2.1.20 JAR.

After the replacement, RobotCore reported `running`; three consecutive requests to the autocomplete
endpoint returned HTTP 200, and the same Robot Controller process remained alive for at least 45
seconds without another fatal exception. No OpMode was selected or run during this repair. The
pre-reset crash log was not preserved, so this cause is confirmed for the post-reset recurrence and
consistent with, but cannot conclusively prove, the earlier loop. The `backboardSlides` name was
entered after the factory reset and is unrelated. The legacy `MarkII.java` file was not involved in
the captured fatal stack.

After a full Control Hub power cycle, ADB over Wi-Fi confirmed the filtered JAR and DEX were still
present. A new Robot Controller process loaded the hardware configuration, including `backboardSlide`,
`frontLeft`, `turret`, `launcherGate`, and `limelight`; `EventLoopManager` reached `RUNNING` and
RobotCore reported `running`. The current OpMode remained `$Stop$Robot$`, and the captured post-boot
log contained no `FATAL EXCEPTION` or `NoClassDefFoundError`. This confirms the fix survives the
power cycle that had triggered the startup loop. No OpMode was run and no motors were commanded.

**2026-11-25 — Startup positions and elevator direction:** The requested startup ramp targets are
now `0.0` for both servos; the launcher gate remains open through INIT and active start, and the
hood starts at `1.0` (maximum up). After the user reported that stick-up moved the backboard slide
down, `backboardSlide` was set to `REVERSE` so positive power and encoder counts should mean
extension. `RobotConfig.java`, `RobotHardware.java`, `Shooter.java`, and `RobotTuning.java` were
synced to OnBotJava and **Build Everything** succeeded. The reversed slide direction still needs a
short physical jog confirmation; no OpMode or mechanism was run after this build.

**2026-11-25 — Slide endpoint and turret encoder:** The user measured slide home at `0` ticks and
the raised endpoint at `1850` ticks, with a small clearance; `SLIDE_MAX_TICKS` is now set to `1850`.
The user also reported that turret encoder ticks remain unchanged while the turret moves. This
leaves turret angle and its software limits untrustworthy; do not tune or command automatic turret
targets until the encoder signal is restored and confirmed.

**2026-11-25 — Turret encoder restored and initial tuning:** The user reports the turret encoder is
working. A rough measurement was about `315` ticks over an estimated `90°`, but the user is unsure
of the angle accuracy. The CAD pulley sizes are 24 teeth on the drive pulley and 80 teeth around
the turret, giving `80 / 24 = 3.333` drive-pulley revolutions per turret revolution. Using the
existing `537.7` motor-tick/revolution estimate gives a trial CAD scale of `537.7 * 80 / 24 =
1792.3` ticks per turret revolution. This predicts about `448` ticks per turret quarter-turn; the
rough `315` reading would correspond to about `63°` at this scale. The user measured
`TURRET_MIN_POWER = 0.08` and selected `TURRET_KP = 0.0058`. Verify the CAD-derived scale with a
measured physical angle before relying on turret targets or limits; the turret's safe physical limits
also need verification.

**2026-11-25 -- Turret scale corrected:** The user measured a physical `90` degree turret move while
the prior `1792.3` ticks/revolution scale displayed `64` degrees. Correcting with
`oldScale * displayed / measured` gives `1792.3 * 64 / 90 = 1274.5` ticks per turret revolution.
This corresponds to about `319` ticks per `90` degrees and `3.54` ticks per degree. The earlier
CAD-derived scale is superseded. Recheck a small move from home and confirm direction before
expanding the turret test range.

`EncoderCheck` can be used after reseating the encoder leads with power off. It does not reset motor
encoders or move servos. It reports raw counts for the four drive motors, intake, launcher, slide,
and turret, and gives only the selected motor a 0.18-power pulse for 0.20 seconds. The robot must be
supported with the drive wheels clear; keep the launcher, slide, turret, and intake paths clear.

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
| `backboardSlide` | `DcMotorEx` | Reversed direction; encoder reset on first init, then `RUN_USING_ENCODER`; brakes at zero power | Slide position and manual motion; positive ticks mean extension |
| `launcher` | `DcMotorEx` | `RUN_USING_ENCODER`; zero-power behavior `FLOAT` (coasts) | Launcher velocity control |
| `rampLeft` | `Servo` | Starts at `RAMP_LEFT_STOW` (`0.0`) | Left side of ramp |
| `rampRight` | `Servo` | Starts at `RAMP_RIGHT_STOW` (`0.0`) | Right side of ramp |
| `launcherGate` | `Servo` | Starts at `GATE_OPEN` during INIT and stays open through the active start until a gate command or feed request | Ball gate |
| `hood` | `Servo` | Starts at `HOOD_POSITION` (`1.0`, maximum up) during INIT; range is 0 down to 1 up | Hood mechanism |
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

The current slide and turret limits are software checks. They do not detect a jam or protect against encoder failure, incorrect motor polarity, loose couplings, or inertia past the boundary. The slide has no programmed extension switch. User-reported turret mechanical travel and exact software bounds are -210 degrees counterclockwise and +180 degrees clockwise from home. Flip-test destinations stay 60 degrees inside those bounds at -150 and +120 degrees; verify encoder zero and scale before moving there.

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
| Hold left stick button + right stick X | Jog the turret angle target; the controller moves and holds it |
| Hold left stick button + D-pad up/down | Command ramp level/stow |
| Hold left stick button + D-pad right/left | Open/close the gate; opening requires launcher-ready speed |
| `Back` | Cancel remaining feeds, close the gate, stop intake, and let the launcher coast down |

An analog stick movement or D-pad command while manual control is enabled cancels the current feed
sequence. Cancelling a sequence does not turn the launcher off; `Back` does. A newly selected ball
sequence replaces the current remaining count rather than appending to it.

After a manual slide or turret move, the mechanism holds where it was released. The turret stick
changes the requested angle at a configured rate; it never commands raw motor power. The same
profiled encoder controller moves the turret in manual, preset, and Limelight modes. When the manual
control button is released, Limelight aiming can take over again outside Flower mode; the slide
stays put until Flower is toggled.

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
encoder ticks per full turret output revolution. Every automatic target move uses an acceleration
limited trapezoidal profile: it ramps speed up, cruises when the move is long enough, and brakes
according to the remaining stopping distance. Encoder position feedback corrects tracking error.
This same controller runs for manual angle jogging, presets, and Limelight aiming. The manual stick
changes the target angle at `TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC`; releasing it holds that angle.

Targets are kept within asymmetric software bounds. A target just past a limit is replaced by the
same direction one full turn back when that equivalent angle fits inside the bounds; otherwise the
target stops at the nearer bound. Power that would push farther past either bound is blocked. The
    User-reported mechanical travel is -210 degrees counterclockwise to +180 degrees clockwise; software limits match both endpoints. Wide flip-test targets stay 60 degrees inside at -150 and +120 degrees.

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

The gate starts at `GATE_OPEN` (`0.38`) during INIT and remains open through the active start until
a gate command or feed request takes control. It uses `GATE_CLOSED` (`0.11`) when closed and after
the OpMode stops. Normal TeleOp and timed feeding retain the launcher-ready check. In
`BioBuzz Tuning`, Circle manually opens the gate at any RPM for position testing; releasing it
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
| `TURRET_TICKS_PER_REVOLUTION` | `1274.5` | Calibrated from 64 degrees indicated for a measured 90-degree turret move |
| `TURRET_PHYSICAL_MIN_DEGREES`, `TURRET_PHYSICAL_MAX_DEGREES` | `-210`, `180` | Reported travel from home; negative is counterclockwise |
| `TURRET_LIMIT_MARGIN_DEGREES` | `0` | No software margin inside the reported endpoints |
| `TURRET_MIN_DEGREES`, `TURRET_MAX_DEGREES` | `-210`, `180` | Software limits match the reported CCW/CW endpoints exactly |
| `TURRET_FLOWER_DEGREES` | `45` | Flower turret target relative to home |
| `TURRET_KP` | `0.0058` | Turret power per degree of profile-position error |
| `TURRET_MIN_POWER` | `0.08` | Measured initial minimum automatic output outside tolerance |
| `TURRET_MAX_POWER` | `1.0` | Output cap for the profiled turret controller |
| `TURRET_TOLERANCE_DEGREES` | `0.5` | Error below which the turret stops driving |
| `TURRET_TEST_STEP_DEGREES` | `1.0` | Relative target step for the tuning OpMode |
| `TURRET_TEST_FLIP_DEGREES` | `210.0` | R2+Circle flip amplitude; large enough to reach the 60-degree-inset test targets |
| `TURRET_TEST_LIMIT_MARGIN_DEGREES` | `60.0` | Test-target inset from each software limit; software limits remain unchanged |
| `TURRET_PROFILE_MAX_VELOCITY_DEG_PER_SEC` | `360` | Maximum profiled speed in every automatic mode |
| `TURRET_PROFILE_ACCEL_DEG_PER_SEC_SQ` | `720` | Acceleration limit while speeding up |
| `TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ` | `190` | Deceleration used to plan earlier braking |
| `TURRET_MANUAL_TARGET_RATE_DEG_PER_SEC` | `240` | Target-angle change per second at full stick input |
| `LIMELIGHT_AIM_GAIN` | `1.0` | Multiplier from Limelight `tx` degrees to turret target degrees; negative reverses it |
| `LIMELIGHT_MAX_STALENESS_MS` | `250` | Camera frames older than this are ignored |
| `RED_TAG_MIN_ID`, `RED_TAG_MAX_ID` | `30`, `37` | AprilTag IDs the turret aims at on Red |
| `BLUE_TAG_MIN_ID`, `BLUE_TAG_MAX_ID` | `38`, `45` | AprilTag IDs the turret aims at on Blue |
| `RED_LIMELIGHT_PIPELINE`, `BLUE_LIMELIGHT_PIPELINE` | `0`, `0` | Limelight pipeline indexes |
| `RAMP_LEFT_STOW`, `RAMP_LEFT_LEVEL` | `0.0`, `0.483` | Left startup and level targets; startup is zero |
| `RAMP_RIGHT_STOW`, `RAMP_RIGHT_LEVEL` | `0.0`, `0.517` | Right startup and level targets; startup is zero |
| `GATE_CLOSED`, `GATE_OPEN` | `0.11`, `0.38` | Measured gate endpoints |
| `HOOD_POSITION` | `1.0` | Hood startup target (maximum up); tuning range is 0 down to 1 up |
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

`RobotTuning` can adjust slide limits/gain, turret encoder ratio, asymmetric turret limits, position
gain, minimum and maximum power, profile velocity, acceleration and braking, manual target rate, flip angle, tolerance,
test step, Flower target, paired ramp and gate endpoints, launcher ratio/target RPM, and feed timings.
It provides a hood jog over 0..1 and a gate test that does not require launcher RPM. The tuner
displays assignments to copy back into the config file. Its encoder displays are useful for
measuring travel: record home and endpoint ticks, then calculate limits from the measured change
rather than relying on the initial estimates.

### `BioBuzz Tuning` controls (Gamepad 1 only)

Gamepad 2 is not used by this OpMode. The right-trigger plus D-pad chord separates turret target
testing from the D-pad controls for selecting and changing settings.

| Action | Gamepad 1 control |
| --- | --- |
| Select setting | D-pad up/down |
| Change selected setting | D-pad left/right |
| Choose fine or coarse adjustment | Hold `X` for 0.1x; hold `Y` for 10x; otherwise 1x |
| Step turret target | Hold right trigger and tap D-pad left/right |
| Flip between two test angles | Hold right trigger and tap Circle; default targets are 60 degrees inside the limits (`-150` and `+120`) |
| Return turret target to zero | Hold right trigger and tap D-pad up |
| Jog slide | Press and hold the left stick down while moving the left stick |
| Jog turret angle | Press and hold the right stick down while moving right stick X; the controller moves and holds the target |
| Jog mirrored ramp pair | Hold `LB` or `RB` and move right stick Y; both targets change together |
| Jog hood | Hold left trigger and move right stick Y; 0 is down and 1 is up |
| Spin launcher | Hold `A`; releasing it stops drive power and the wheel coasts |
| Test gate open | Hold `B` without right trigger; tuning opens it at any RPM, release to close |

On the PS5-style controller used here, Cross maps to FTC `A`, Circle to `B`, Square to `X`, and
Triangle to `Y`; `R2` is the right trigger.

For `TURRET_MIN_POWER`, a normal D-pad increment is 0.01, `X` makes it 0.001, and `Y` makes it
0.1. Use `X` for fine changes. `RobotTuning` re-zeroes the slide and turret every time it starts,
so place both at their home positions before selecting it. Values changed in the tuner are live only
until the Robot Controller app restarts; copy the displayed `RobotConfig` assignment into the source
to store a calibration. With right trigger held, Circle is the turret flip command and does not
open the gate.

For `TURRET_PROFILE_BRAKE_ACCEL_DEG_PER_SEC_SQ`, a normal D-pad tap changes it by `10`, `X` changes
it by `1`, and `Y` by `100`. Lower values make the profile begin braking earlier while preserving
the separate speed-up acceleration and maximum velocity.

## Tuning and commissioning sequence

1. **Prepare the robot.** Charge the battery, stop every OpMode, and check that no one is near the
   wheels, launcher, slide, turret, or servo linkages. Support the robot before running any drive
   motor test. Use no game piece for the first launcher check.
2. **Check configuration.** Confirm all required hardware names and SDK device classes above.
   Compare them with the recorded hub port layout, then confirm the IMU orientation, motor wiring,
   servo ports, and Limelight connection. The camera may be absent during mechanical bring-up.
3. **Build and deploy.** Stop the OpMode, connect to the Control Hub over USB or Wi-Fi ADB, then
   follow the PowerShell build and install commands in [TeamCode README](README.md#build-and-deploy-with-adb).
   The first install over the stock FTC-signed app needs a one-time uninstall if Android reports a
   signing conflict; preserve the project signing key so later installs use `adb install -r`.
   Confirm the Robot Controller app starts and the Driver Station reconnects after installation.
4. **Establish references.** With power removed or mechanisms safely supported, place the slide at
   full retraction and turret at the defined home angle before the first init after the app
   starts. `BioBuzz Tuning` re-zeroes them on each start, so do this every time before selecting
   the OpMode. Check that both encoder readings are zero before moving either mechanism.
5. **Check drive polarity.** The raised-wheel `Motor Test` now passes for all four wheels. Still
   check each stick direction and field centric mode at low power before normal floor driving.
6. **Check servo starting positions.** The hood range is confirmed as `0.0` down and `1.0` maximum
   up; it should start at `1.0`. Ramp calibration is deferred until its horns are re-indexed. When
   resuming it, use the one-controller map above to jog the mirrored pair with `LB`/`RB` and right
   stick Y, moving only a little at a time and stopping before any linkage binds.
7. **Measure slide travel.** Hold the left stick button and use the left stick at low manual power.
   Check that extension increases the encoder count. Record home and the safe extension endpoint;
   set `SLIDE_MAX_TICKS` below the measured mechanical endpoint. Verify Flower extension and
   retraction have clearance before trying automatic moves.
8. **Verify turret calibration and response.** The corrected scale is `1274.5` ticks/revolution,
   based on a measured 90-degree turret move that displayed 64 degrees at the previous setting. It
   predicts about `319` ticks per quarter-turn and `35` ticks per 10 degrees. The current minimum
   power is `0.08` and Kp is `0.0058`. The profile uses 360 degrees/second maximum velocity,
   720 degrees/second-squared speed-up acceleration, and 190 degrees/second-squared braking.
   R2+Circle now flips between `-150` and `+120` degrees, 60 degrees inside the exact
   software limits of -210 and +180 degrees. Confirm the home zero, encoder scale, direction, and cable
   clearance before using this wide test. Watch actual and target angles, profile speed, error, and
   power request; stop if motion is reversed, binds, or overshoots.

   The planned stopping distance is `speed^2 / (2 * braking acceleration)`. At 360 degrees/second
   and 190 degrees/second-squared, it is about 341 degrees. The 270-degree test sweep should peak
   near 285 degrees/second and brake without a cruise segment. Speed-up remains 720
   degrees/second-squared; Kp remains 0.0058. A short move will not reach the speed cap.

   If it still overshoots, capture the peak angle, measured speed, profile speed, and power request
   before changing another setting. Keep `TURRET_MIN_POWER = 0.08` for this run so the braking
   profile is the only changed value. Copy the displayed assignment into `RobotConfig.java` after
   validating it in both directions.
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
