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
`launcherGate`, `imu`, and `limelight`. The eight motors are mapped as `DcMotorEx`; the two ramp
servos and gate are `Servo` devices. If any name is missing, init stops with one error listing all
of them.

The two odometry pods (one forward, one strafe) are not separate devices. Each plugs into the
encoder port of a drive motor or the intake motor; put that motor's name in
`RobotConfig.FORWARD_POD_MOTOR_NAME` / `STRAFE_POD_MOTOR_NAME`. Leave them empty until the ports
are decided.

The controller assumes the slide is fully retracted and turret is at its known home angle the
first time an OpMode initializes after the app starts, and zeroes both encoders then. Later OpModes
keep that zero. To zero them again, put both at home and press Gamepad 1 `X` during TeleOp init.
The IMU orientation in `RobotHardware` assumes its logo faces up and USB faces forward; change it
to match the installed Hub.

## Driver controls

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

The Limelight is mounted on the turret. The turret only aims at your alliance's HIVE tags: IDs
30-37 on Red and 38-45 on Blue (`RED_TAG_MIN_ID`/`MAX_ID` and `BLUE_TAG_MIN_ID`/`MAX_ID` in
`RobotConfig`). Narrow a range to one sticker's four IDs to aim at a single CELL. Tracking keeps
its last angle after detections disappear. Set the Red and Blue pipeline indexes to match the Limelight configuration.

## Calibration

Run `Motor Test` first with the wheels off the ground to check each drive motor's direction.

Run `BioBuzz Tuning` with the slide retracted and turret at home. D-pad up/down selects a value;
D-pad left/right changes it. Hold `X` for fine steps and `Y` for coarse steps. Hold Gamepad 2 left
or right bumper and move right stick Y to jog the corresponding ramp servo. Hold the left or right
stick button to enable slide or turret manual movement. Gamepad 1 `A` spins up the launcher; hold
Gamepad 2 `B` to test the gate after launcher speed reaches its ready threshold. The OpMode displays
the current Java assignment to copy into `RobotConfig`; edits made during tuning reset when the
controller app restarts.

The initial encoder counts, turret ratio, Flower positions, launch RPM, and timing values are bring
up estimates. Calibrate slide full extension, turret output-shaft counts per revolution, motor
encoder ticks per launcher revolution, servo travel, and launch/feed timing with the mechanism
clear. Ramp stow-to-level endpoints begin about one third of the servo's 0-to-1 range apart (60° of
180° nominal servo travel), with independent values for the mirrored sides.

`PedroAutoShell` is disabled and contains no movement paths. The requested
`com.pedropathing:ftc:3.0.2` artifact is not available from Maven Central or the PedroPathing Maven
repository; this project uses the latest published FTC artifact found during setup,
`com.pedropathing:ftc:2.1.2`, until 3.0.2 is published.
