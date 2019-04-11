package org.firstinspires.ftc.teamcode;

import com.disnodeteam.dogecv.CameraViewDisplay;
import com.disnodeteam.dogecv.DogeCV;
import com.disnodeteam.dogecv.detectors.roverrukus.GoldAlignDetector;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
/**
 * Created by Manjesh on 12/4/2018.
 */
@Autonomous(name="Depot Autonomous",group = "British Columbia Competition")
public class DepotAutonomous extends LinearOpMode {

    //Declare all variables
    final float CIRCUMFERENCE = (float)(3.93701 * Math.PI);
    final int ENCODERTICKS = 1120;
    final double GEARRATIO = 0.67;
    final double COUNTS_PER_INCH = (ENCODERTICKS * GEARRATIO) / (CIRCUMFERENCE);
    final double DRIVE_SPEED             = 1;     // Nominal speed for better accuracy.
    final double TURN_SPEED              = 0.5;   // Nominal half speed for better accuracy.
    final double HEADING_THRESHOLD       = 1 ;    // As tight as we can make it with an integer gyro
    final double P_TURN_COEFF            = 0.1;   // Larger is more responsive, but also less stable
    final double P_DRIVE_COEFF           = 0.15;  // Larger is more responsive, but also less stable
    final int value = 7600;

    //Motor Count -- 8 / 8 (Max Usage)
    public DcMotor rightMotorFront;
    public DcMotor leftMotorFront;
    public DcMotor rightMotorBack;
    public DcMotor leftMotorBack;
    public DcMotor liftMotor;
    public DcMotor extendMotor;
    public DcMotor linearactuatorMotor;

    //Servo Count -- 4 / 12 (8 left)
    public Servo rotateServoOne;//black box
    //public Servo rotateServoTwo;//black box
    public Servo depositServo;
    public CRServo vacuumServo;//collection mechanism

    public ModernRoboticsI2cGyro gyro;
    private GoldAlignDetector detector;

    @Override
    public void runOpMode() throws InterruptedException
    {
        rightMotorFront = hardwareMap.dcMotor.get("rightMotorFront");
        leftMotorFront = hardwareMap.dcMotor.get("leftMotorFront");
        rightMotorBack = hardwareMap.dcMotor.get("rightMotorBack");
        leftMotorBack = hardwareMap.dcMotor.get("leftMotorBack");
        liftMotor = hardwareMap.dcMotor.get("liftMotor");
        extendMotor = hardwareMap.dcMotor.get("extendMotor");
        linearactuatorMotor = hardwareMap.dcMotor.get("linearactuatorMotor");
        rotateServoOne = hardwareMap.servo.get("rotateServoOne");
        depositServo = hardwareMap.servo.get("depositServo");
        //rotateServoTwo = hardwareMap.servo.get("rotateServoTwo");
        vacuumServo = hardwareMap.crservo.get("vacuumServo");
        leftMotorBack.setDirection(DcMotor.Direction.FORWARD);
        leftMotorFront.setDirection(DcMotor.Direction.FORWARD);
        rightMotorFront.setDirection(DcMotor.Direction.REVERSE);
        rightMotorBack.setDirection(DcMotor.Direction.REVERSE);
        linearactuatorMotor.setDirection(DcMotor.Direction.FORWARD);
        extendMotor.setDirection(DcMotor.Direction.FORWARD);
        liftMotor.setDirection(DcMotor.Direction.REVERSE);
        gyro = (ModernRoboticsI2cGyro) hardwareMap.get("gyro");

        // Send telemetry message to alert driver that we are calibrating;
        telemetry.addData(">", "Calibrating Gyro");    //
        telemetry.update();

        gyro.calibrate();

        // make sure the gyro is calibrated before continuing
        while (!isStopRequested() && gyro.isCalibrating()) {
            sleep(50);
            idle();
        }

        leftMotorFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotorBack.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotorFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftMotorBack.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        linearactuatorMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        extendMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        liftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        telemetry.addData(">", "Robot Ready.");
        telemetry.update();

        leftMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        linearactuatorMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        extendMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        liftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        waitForStart();
        if (opModeIsActive()) {
            while (!isStarted()) {
                telemetry.addData(">", "Robot Heading = %d", gyro.getIntegratedZValue());
                telemetry.update();
            }
            telemetry.addData("Status", "Good Luck Drivers");

            detector = new GoldAlignDetector();
            detector.init(hardwareMap.appContext, CameraViewDisplay.getInstance());
            detector.useDefaults();

            // Optional Tuning
            detector.alignPosOffset = 0; // How far from center frame to offset this alignment zone.
            detector.downscale = 0.4; // How much to downscale the input frames

            detector.areaScoringMethod = DogeCV.AreaScoringMethod.MAX_AREA; // Can also be PERFECT_AREA
            detector.maxAreaScorer.weight = 0.005;

            detector.ratioScorer.weight = 5;
            detector.ratioScorer.perfectRatio = 1.0;

            gyro.resetZAxisIntegrator();

            detector.enable();
            sleep(1500);

            //Center
            if (detector.getXPosition() > 180 && detector.getXPosition() < 420) {

                detector.disable();
                StopDriving();
                move(linearactuatorMotor,DRIVE_SPEED,value);
                gyroTurn(TURN_SPEED,0);
                gyroHold(TURN_SPEED,0,0.5);
                gyroDrive(DRIVE_SPEED, 3, 0);
                gyroTurn(TURN_SPEED, 90);
                gyroHold(TURN_SPEED,90,1);
                gyroDrive(DRIVE_SPEED, 10, 90);
                //score();
                gyroDrive(DRIVE_SPEED, 60, 90);
                depositServo.setPosition(Servo.MAX_POSITION);
                gyroTurn(TURN_SPEED, 41);
                gyroHold(TURN_SPEED, 41, 0.5);
                gyroDrive(DRIVE_SPEED, -80, 41);

            }

            //Right
            else if (detector.getXPosition() > 420){

                detector.disable();
                StopDriving();
                move(linearactuatorMotor,DRIVE_SPEED,value);
                gyroTurn(TURN_SPEED,0);
                gyroHold(TURN_SPEED,0,0.5);
                gyroDrive(DRIVE_SPEED, 3.5, 0);
                gyroTurn(TURN_SPEED, 55);
                gyroHold(TURN_SPEED, 55, 0.5);
                gyroDrive(DRIVE_SPEED, 10, 55);
                //score();
                gyroDrive(DRIVE_SPEED, 40, 55);
                gyroTurn(TURN_SPEED, 130);
                gyroHold(TURN_SPEED, 130, 0.5);
                gyroDrive(DRIVE_SPEED, 40, 130);
                gyroTurn(TURN_SPEED,41);
                gyroHold(TURN_SPEED,41,0.5);
                depositServo.setPosition(Servo.MAX_POSITION);
                sleep(500);
                gyroDrive(DRIVE_SPEED, -80, 41);

            }

            //Left
            else if (detector.getXPosition() < 180) {

                detector.disable();
                StopDriving();
                move(linearactuatorMotor,DRIVE_SPEED,value);
                gyroTurn(TURN_SPEED,0);
                gyroHold(TURN_SPEED,0,0.5);
                gyroDrive(DRIVE_SPEED, 3.5, 0);
                gyroTurn(TURN_SPEED, 115);
                gyroHold(TURN_SPEED, 115, 0.5);
                gyroDrive(DRIVE_SPEED, 10, 115);
                //score();
                gyroDrive(DRIVE_SPEED, 43, 115);
                gyroTurn(TURN_SPEED, 45);
                gyroHold(TURN_SPEED, 45, 0.5);
                gyroDrive(DRIVE_SPEED, 15, 45);
                depositServo.setPosition(Servo.MAX_POSITION);
                sleep(1000);
                gyroDrive(DRIVE_SPEED, -65, 41);
            }

            telemetry.addData("Path", "Complete");
            telemetry.update();

            StopDriving();
        }

    }

    public void score(){
        move(extendMotor,0.5,-500);
        vacuumServo.setPower(1);
        rotateServoOne.setPosition(Servo.MAX_POSITION);
        //rotateServoTwo.setPosition(Servo.MIN_POSITION);
        sleep(2000);
        move(extendMotor,0.5,500);
        //rotateServoTwo.setPosition(Servo.MAX_POSITION);
        rotateServoOne.setPosition(Servo.MIN_POSITION);
        move(liftMotor,DRIVE_SPEED, 3000);
        sleep(1000);
        move(liftMotor,DRIVE_SPEED, 0);
    }

    public void move (DcMotor motor, double power, int distance)
    {
        int moveNumber;
        int move;

        // Ensure that the opmode is still active
        if (opModeIsActive())
        {
            // Determine new target position, and pass to motor controller
            moveNumber = (int)(distance);
            move = motor.getCurrentPosition() + moveNumber;

            // Set Target and Turn On RUN_TO_POSITION
            motor.setTargetPosition(move);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // start motion
            power = Range.clip(Math.abs(power), 0.0, 1.0);
            motor.setPower(power);

            while (opModeIsActive() && motor.getCurrentPosition() < motor.getTargetPosition()) {
                telemetry.addData("Current Value", motor.getCurrentPosition());
                telemetry.addData("Target Value", motor.getTargetPosition());
                telemetry.update();
                idle();
            }
        }
    }

    public void StopDriving ()
    {
        leftMotorFront.setPower(0);
        rightMotorBack.setPower(0);
        rightMotorFront.setPower(0);
        leftMotorBack.setPower(0);
        linearactuatorMotor.setPower(0);
    }


    public void strafe(double speed, double distance, double angle){
        int     newLeftTargetFront;
        int     newLeftTargetBack;
        int     newRightTargetFront;
        int     newRightTargetBack;
        int     moveCounts;
        double angleInterval = speed/45;
        double speedInterval;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftTargetFront = leftMotorFront.getCurrentPosition() + moveCounts;
            newLeftTargetBack = leftMotorBack.getCurrentPosition() + moveCounts;
            newRightTargetFront = rightMotorFront.getCurrentPosition() + moveCounts;
            newRightTargetBack = rightMotorBack.getCurrentPosition() + moveCounts;

            // Set Target and Turn On RUN_TO_POSITION
            rightMotorFront.setTargetPosition(newRightTargetFront);
            rightMotorBack.setTargetPosition(newRightTargetBack);
            leftMotorFront.setTargetPosition(newLeftTargetFront);
            leftMotorBack.setTargetPosition(newLeftTargetBack);

            rightMotorFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightMotorBack.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftMotorFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftMotorBack.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            //start motion
            while(opModeIsActive()){
                if (angle >= 0 && angle < 45){
                    speedInterval = 45 - angle;
                    leftMotorFront.setPower(speed);
                    rightMotorBack.setPower(speed);
                    rightMotorFront.setPower(-1*(speedInterval*angleInterval));
                    leftMotorBack.setPower(-1*(speedInterval*angleInterval));
                } else if(angle >= 45 && angle < 90){
                    speedInterval = angle - 45;
                    leftMotorFront.setPower(speed);
                    rightMotorBack.setPower(speed);
                    rightMotorFront.setPower(speedInterval*angleInterval);
                    leftMotorBack.setPower(speedInterval*angleInterval);
                } else if (angle >= 90 && angle < 135){
                    speedInterval = 135 - angle;
                    leftMotorFront.setPower(speedInterval*angleInterval);
                    rightMotorBack.setPower(speedInterval*angleInterval);
                    rightMotorFront.setPower(speed);
                    leftMotorBack.setPower(speed);
                } else if (angle >= 135 && angle < 180){
                    speedInterval = angle - 135;
                    leftMotorFront.setPower(-1*(speedInterval*angleInterval));
                    rightMotorBack.setPower(-1*(speedInterval*angleInterval));
                    rightMotorFront.setPower(speed);
                    leftMotorBack.setPower(speed);
                } else if (angle >= 180 && angle < 225){
                    speedInterval = 225 - angle;
                    leftMotorFront.setPower(-1*speed);
                    rightMotorBack.setPower(-1*speed);
                    rightMotorFront.setPower(speedInterval*angleInterval);
                    leftMotorBack.setPower(speedInterval*angleInterval);
                } else if (angle >= 225 && angle < 270){
                    speedInterval = angle - 225;
                    leftMotorFront.setPower(-1*speed);
                    rightMotorBack.setPower(-1*speed);
                    rightMotorFront.setPower(-1*(speedInterval*angleInterval));
                    leftMotorBack.setPower(-1*(speedInterval*angleInterval));
                } else if (angle >= 270 && angle < 315){
                    speedInterval = 315 - angle;
                    leftMotorFront.setPower(-1*(speedInterval*angleInterval));
                    rightMotorBack.setPower(-1*(speedInterval*angleInterval));
                    rightMotorFront.setPower(-1*speed);
                    leftMotorBack.setPower(-1*speed);
                } else if (angle >= 315 && angle < 360){
                    speedInterval = angle - 315;
                    leftMotorFront.setPower(speedInterval*angleInterval);
                    rightMotorBack.setPower(speedInterval*angleInterval);
                    rightMotorFront.setPower(-1*speed);
                    leftMotorBack.setPower(-1*speed);
                }
            }

            // Stop all motion;
            leftMotorFront.setPower(0);
            leftMotorBack.setPower(0);
            rightMotorFront.setPower(0);
            rightMotorBack.setPower(0);

            // Turn off RUN_TO_POSITION
            rightMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rightMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            leftMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            leftMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
    }

    public void gyroDrive ( double speed, double distance, double angle) {

        int     newLeftTargetFront;
        int     newLeftTargetBack;
        int     newRightTargetFront;
        int     newRightTargetBack;
        int     moveCounts;
        double  max;
        double  error;
        double  steer;
        double  leftSpeed;
        double  rightSpeed;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftTargetFront = leftMotorFront.getCurrentPosition() + moveCounts;
            newLeftTargetBack = leftMotorBack.getCurrentPosition() + moveCounts;
            newRightTargetFront = rightMotorFront.getCurrentPosition() + moveCounts;
            newRightTargetBack = rightMotorBack.getCurrentPosition() + moveCounts;

            // Set Target and Turn On RUN_TO_POSITION
            rightMotorFront.setTargetPosition(newRightTargetFront);
            rightMotorBack.setTargetPosition(newRightTargetBack);
            leftMotorFront.setTargetPosition(newLeftTargetFront);
            leftMotorBack.setTargetPosition(newLeftTargetBack);

            rightMotorFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightMotorBack.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftMotorFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            leftMotorBack.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // start motion.
            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            rightMotorFront.setPower(speed);
            rightMotorBack.setPower(speed);
            leftMotorFront.setPower(speed);
            leftMotorBack.setPower(speed);

            // keep looping while we are still active, and BOTH motors are running.
            while (opModeIsActive() &&
                    (rightMotorFront.isBusy() && rightMotorBack.isBusy() && leftMotorFront.isBusy() && leftMotorBack.isBusy())) {

                // adjust relative speed based on heading error.
                error = getError(angle);
                steer = getSteer(error, P_DRIVE_COEFF);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;

                leftSpeed = speed - steer;
                rightSpeed = speed + steer;

                // Normalize speeds if either one exceeds +/- 1.0;
                max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
                if (max > 1.0)
                {
                    leftSpeed /= max;
                    rightSpeed /= max;
                }

                leftMotorFront.setPower(leftSpeed);
                leftMotorBack.setPower(leftSpeed);
                rightMotorFront.setPower(rightSpeed);
                rightMotorBack.setPower(rightSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d",      newRightTargetFront, newRightTargetBack, newLeftTargetFront, newLeftTargetBack);
                telemetry.addData("Actual",  "%7d:%7d",      leftMotorFront.getCurrentPosition(), leftMotorBack.getCurrentPosition(), rightMotorFront.getCurrentPosition(), rightMotorBack.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  leftSpeed, rightSpeed);
                telemetry.update();
            }

            // Stop all motion;
            leftMotorFront.setPower(0);
            leftMotorBack.setPower(0);
            rightMotorFront.setPower(0);
            rightMotorBack.setPower(0);

            // Turn off RUN_TO_POSITION
            rightMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rightMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            leftMotorFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            leftMotorBack.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
    }

    public void gyroTurn (  double speed, double angle) {

        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF)) {
            // Update telemetry & Allow time for other processes to run.
            telemetry.update();
        }
    }

    public void gyroHold( double speed, double angle, double holdTime) {

        ElapsedTime holdTimer = new ElapsedTime();

        // keep looping while we have time remaining.
        holdTimer.reset();
        while (opModeIsActive() && (holdTimer.time() < holdTime)) {
            // Update telemetry & Allow time for other processes to run.
            onHeading(speed, angle, P_TURN_COEFF);
            telemetry.update();
        }

        // Stop all motion;
        leftMotorFront.setPower(0);
        leftMotorBack.setPower(0);
        rightMotorFront.setPower(0);
        rightMotorBack.setPower(0);
    }

    boolean onHeading(double speed, double angle, double PCoeff) {
        double   error ;
        double   steer ;
        boolean  onTarget = false ;
        double leftSpeed;
        double rightSpeed;

        // determine turn power based on +/- error
        error = getError(angle);

        if (Math.abs(error) <= HEADING_THRESHOLD) {
            steer = 0.0;
            leftSpeed  = 0.0;
            rightSpeed = 0.0;
            onTarget = true;
        }
        else {
            steer = getSteer(error, PCoeff);
            rightSpeed  = speed * steer;
            leftSpeed   = -rightSpeed;
        }

        // Send desired speeds to motors.
        leftMotorFront.setPower(leftSpeed);
        leftMotorBack.setPower(leftSpeed);
        rightMotorFront.setPower(rightSpeed);
        rightMotorBack.setPower(rightSpeed);

        // Display it for the driver.
        telemetry.addData("Target", "%5.2f", angle);
        telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);

        return onTarget;
    }

    public double getError(double targetAngle) {

        double robotError;

        // calculate error in -179 to +180 range  (
        robotError = targetAngle - gyro.getIntegratedZValue();
        while (robotError > 180)  robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
    }

    public double getSteer(double error, double PCoeff) {
        return Range.clip(error * PCoeff, -1, 1);
    }
}