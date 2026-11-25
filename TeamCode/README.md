# BioBuzz TeamCode

See [ROBOT_SYSTEM.md](ROBOT_SYSTEM.md) for the in-depth robot, software, calibration, and
commissioning reference.

## Official FTC rules and resources

Use [FTC Resources](https://ftc-resources.firstinspires.org/) to look up the current season's rules,
game manuals, and official competition resources. Check the applicable season and manual before
making rule-dependent robot changes.

## Hardware names

Create these exact names in the Robot Configuration: `frontLeft`, `frontRight`, `backLeft`,
`backRight`, `intake`, `turret`, `backboardSlide`, `launcher`, `rampLeft`, `rampRight`,
`launcherGate`, `hood`, `odometryLeft`, `odometryRight`, and `imu`. The eight motors are mapped as
`DcMotorEx`; the two ramp servos, gate, and hood are `Servo` devices. The odometry names are mapped
as digital inputs, but pod encoder counts are not decoded yet. The hood is mapped and can be jogged
in Tuning, but no startup position is stored until calibrated. If any required name is missing,
init stops with one error listing all of them. The Limelight is optional during bring-up; when
configured, name the Limelight 3A `limelight` to enable tag aiming.

See [ROBOT_SYSTEM.md](ROBOT_SYSTEM.md#recorded-hub-port-layout) for the stored hub, port, and
device-name layout. The optional odometry reader still expects pod encoders connected through motor
encoder ports, configured in
`RobotConfig.FORWARD_POD_MOTOR_NAME` / `STRAFE_POD_MOTOR_NAME`.

The controller assumes the slide is fully retracted and turret is at its known home angle the
first time an OpMode initializes after the app starts, and zeroes both encoders then. Later OpModes
keep that zero. To zero them again, put both at home and press Gamepad 1 `X` during TeleOp init.
The IMU orientation in `RobotHardware` assumes its logo faces up and USB faces forward; change it
to match the installed Hub.

## Driver controls

The small badge beside an OpMode name in the Driver Station indicates which programming tool
created it. `OBJ` means OnBot Java; it does not change what the OpMode does.

Gamepad 1 sticks drive. Hold `RB` for slow mode. `Y` switches between field and robot centric
drive, and `Back` resets the field centric heading to the way the robot is facing. During init,
D-pad left selects Red and right selects Blue, and `X` re-zeroes the slide and turret. Hold `Start`
to override the Flower lockout.

Gamepad 2 `A`, `B`, `X`, and `Y` request one, two, three, and four balls. `LB` toggles Flower mode;
`RB` toggles the ramp between stowed and level. Hold the left stick button to enable manual control:
the left stick moves the slide, right stick moves the turret, D-pad up/down deploys/stows the ramp,
and D-pad right/left opens/closes the gate. The gate opens only while launcher speed is ready. Any
manual mechanism input cancels queued feeds. `Back` cancels feeding and switches the launcher off.
After a manual move the slide and turret hold where they were released.

Flower activation is locked until the final 60 seconds of a 120-second TeleOp by default. Set
`RobotConfig.FLOWER_LOCKOUT_ENABLED` false to disable that lockout. Ramp movement is independent of
Flower mode.

When the Limelight 3A is configured, it is mounted on the turret. The turret only aims at your
alliance's HIVE tags: IDs 30-37 on Red and 38-45 on Blue (`RED_TAG_MIN_ID`/`MAX_ID` and
`BLUE_TAG_MIN_ID`/`MAX_ID` in `RobotConfig`). Narrow a range to one sticker's four IDs to aim at a
single CELL. Tracking keeps its last angle after detections disappear. Set the Red and Blue pipeline
indexes to match the Limelight configuration.

## Calibration

Run `Motor Test` first with the wheels off the ground to check each drive motor's direction.

Run `BioBuzz Tuning` with the slide retracted and turret at home. It uses Gamepad 1 only. D-pad
up/down selects a value and left/right changes it; hold `X` for fine steps and `Y` for coarse
steps. Hold right trigger plus D-pad left/right to step the turret target; right trigger plus D-pad
up returns it to zero. Hold the left stick button and move the left stick to jog the slide; hold the
right stick button and move right stick X to jog the turret. Hold `LB` or `RB` and move right stick
Y to jog the left or right ramp servo. Hold left trigger and move right stick Y to jog the hood.
Hold `A` to spin up the launcher; hold `B` to request the gate after launcher speed reaches its
ready threshold. For the turret minimum-power test, select `TURRET_MIN_POWER`, hold `X`, and tap
D-pad right for 0.001 increments. The OpMode displays Java assignments to copy into `RobotConfig`;
live edits reset when the controller app restarts. See [`ROBOT_SYSTEM.md`](ROBOT_SYSTEM.md) for the
full control map and commissioning procedure.

The initial encoder counts, turret ratio, Flower positions, launch RPM, and timing values are bring
up estimates. Calibrate slide full extension, turret output-shaft counts per revolution, motor
encoder ticks per launcher revolution, servo travel, and launch/feed timing with the mechanism
clear. Ramp stow-to-level endpoints begin about one third of the servo's 0-to-1 range apart (60° of
180° nominal servo travel), with independent values for the mirrored sides.

`Pedro Localization Check` is a low-power manual mode for collecting drive-encoder deltas from the
foam tile. It starts at the local pose `(0, 0, 0)`, with raw tick-equivalent position units and
heading scale disabled until measured. It does not follow a path. Keep autonomous disabled until the
tile scale, wheel signs, robot geometry, and turn scale are calibrated; retune on the official field.

PedroPathing 3.0.2 is available through the Dairy Foundation Maven repository as
`com.pedropathing:revhub:3.0.2`, with the optional AutoTune package
`com.pedropathing:tuning:1.0.0`. This workspace remains on `com.pedropathing:ftc:2.1.2` for now;
the 3.x release has a new API and needs a deliberate migration. The autonomous shell remains
disabled and contains no movement path.
