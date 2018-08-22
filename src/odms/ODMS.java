package odms;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import com.fazecast.jSerialComm.SerialPort;

import odms.ui.Button;
import processing.core.PApplet;

/*
 * This program creates an interactive interface that allows users to select a log file, start, and stop logging speed data
 * read from the Arduino via serial communication. Additionally, a timer, speed indicator, graph, and colored background
 * provide ease of accessibility and additional information to the user for easier use of the program.
 * 
 * Raw data received is first stored in a temporary buffer, then transferred into a deque for storage. The deque is used to
 * create the background graph as well as calculate the current speed in knots based on the sample size.
 * 
 * Processing is a flexible software sketchbook and is used here to generate graphics with ease. More information for Processing
 * can be found at https://processing.org
 * 
 * Processing comes with its own serial communication library, but requires additional setup outside of the Processing IDE
 * and is overall kinda wonky and I don't really like it. As such, this program uses jSerialComm. More information for jSerialComm
 * can be found at https://fazecast.github.io/jSerialComm/
 */
public class ODMS extends PApplet {
	// Bound constants for speed that will be used by various parts of the program
	// LOWER_BOUND and UPPER_BOUND should be set to the lowest and highest speed respectively
	public static final float LOWER_BOUND = 0;
	public static final float MIDDLE_BOUND = 9;
	public static final float UPPER_BOUND = 18;
	
	// Size constant in samples for the deque of raw data received from the Arduino
	// Each sample represents 0.25 seconds, so 2 samples represent 0.50 seconds, 5 samples represent 1.25 seconds, etc.
	public static final int DEQUE_SIZE = 80;
	
	// Size constant for how long a period of time should be accounted for each speed calculation
	public static final int SAMPLE_SIZE = 2;
	
	// Conversion constant for multiplying the number of flips in an interval determined by the sample size to get speed in knots
	// The number 0.111045f was determined through experimentation and linear regression
	public static final float CONVERSION_CONSTANT = 0.111045f;
	
	// Serial port for communication with the Arduino
	private SerialPort arduinoSerial;
	
	// Buffer that temporarily stores received byte data
	private final byte[] buffer = new byte[1];
	
	// Deque that stores raw data received from the Arduino
	private final Deque<Integer> data = new LinkedList<Integer>();
	
	// PrintWriter that writes data to the log file
	private PrintWriter logger;
	
	// Float that stores the current speed in knots
	private float knots;
	
	// Color that represents the current speed with red being slow and green being fast relative to specified bounds
	private int color;
	
	// Longs that keep track of the timer and its offset
	private long timer, timerOffset;
	
	// Boolean that tracks whether or not data is currently being logged
	private boolean logging;
	
	// Buttons to select a file, start logging, and stop logging data
	// TODO: Make these fields final and migrate instantiation from setup to here
	private Button selectButton, startButton, stopButton;
	
	// Main method, creates and runs the PApplet based on this class
	public static void main(String[] args) {
		PApplet.main("odms.ODMS");
	}
	
	// Configure program settings before the start of the program
	@Override
	public void settings() {
		// Set the size of the window to 640px by 360px
		size(640, 360);
	}
	
	// Called once at the start of the program
	@Override
	public void setup() {
		// Set the window title
		surface.setTitle("Onboard Data Management System 1.1");
		
		// Attempt to configure the Arduino serial port for reading
		try {
			arduinoSerial = SerialPort.getCommPorts()[0];
		} catch(ArrayIndexOutOfBoundsException e) {
			// If the port is not found, display and error and halt the program
			background(0);
			textAlign(CENTER, CENTER);
			fill(255, 255, 0);
			triangle(width / 2, height / 7, width / 2 - 100, 4 * height / 7, width / 2 + 100, 4 * height / 7);
			textSize(160);
			fill(255, 0, 0);
			text("!", width / 2, height / 3);
			textSize(24);
			fill(255);
			text("Arduino not detected.\nPlease plug the Arduino in and try again.", width / 2, 2 * height / 3);
			return;
		}
		arduinoSerial.openPort();
		
		// Populate the data deque
		// There probably is a much more elegant way of doing this, but this is the only way I know
		for(int i = 0; i < DEQUE_SIZE; i++)
			data.add(0);
		
		// Create buttons to start and stop logging
		Runnable selectButtonRunnable = () -> {
			// Select a file to save logged data to
			selectOutput("Select a File:", "selectFile", null, this);
			
			// Visibility toggling is done in the selectFile method
		};
		selectButton = new Button(this, (width - 350) / 2, 43 * (height - 75) / 48, 350, 75, "Select a File", selectButtonRunnable, true);
		
		Runnable startButtonRunnable = () -> {
			// The logger should already be instantiated at this point in time
			assert(logger != null);
			
			// Create the log header with a timestamp and column names
			logger.println(year() + "-" + month() + "-" + day() + "T" + hour() + ":" + minute() + ":" + second());
			logger.println("Time Elapsed (ms),Speed (kts)");
			logger.flush();
			
			// Start logging
			logging = true;
			
			// Set the timer offset to the current time
			timerOffset = millis();
			
			// Toggle button visibility
			stopButton.setVisibility(true);
			startButton.setVisibility(false);
		};
		startButton = new Button(this, (width - 350) / 2, 43 * (height - 75) / 48, 350, 75, "Start Logging", startButtonRunnable, false);
		
		Runnable stopButtonRunnable = () -> {
			// Stop logging
			logging = false;
			
			// Close the logger
			logger.close();
			
			// Toggle button visibility
			selectButton.setVisibility(true);
			stopButton.setVisibility(false);
		};
		stopButton = new Button(this, (width - 350) / 2, 43 * (height - 75) / 48, 350, 75, "Stop Logging", stopButtonRunnable, false);
	}
	
	// Called to select a file to log
	public void selectFile(File logfile) {
		// Only execute if a file has been selected
		// If a file has not been selected, do nothing and let the user select another file
		if(logfile != null) {
			// Create the logger to log data to the log file
			try {
				logger = new PrintWriter(new FileWriter(logfile));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			startButton.setVisibility(true);
			selectButton.setVisibility(false);
		}
	}
	
	// Main loop, called at 60 times per second assuming no lag
	@Override
	public void draw() {
		// Call dataReceived() when data is received from Arduino serial port
		if(arduinoSerial.bytesAvailable() > 0)
			dataReceived();
		
		// Update the timer by calculating the current time minus the offset if logging
		if(logging)
			timer = millis() - timerOffset;
		
		// Set the background color to reflect the current speed
		background(color);
		
		// Display previously collected data as a graph of speed over time
		noStroke();
		fill(color + 0x00202020);
		beginShape();
		vertex(0, height);
		Iterator<Integer> it = data.iterator();
		for(int i = 0; i < DEQUE_SIZE; i++) {
			curveVertex(i / (DEQUE_SIZE - 1f) * (width + 60) - 30, height - map(it.next(), 0, UPPER_BOUND / CONVERSION_CONSTANT, 0, height));
		}
		vertex(width, height);
		endShape();
		
		stroke(255);
		strokeWeight(3);
		for(int i = 0; i < 10; i++) {
			line(i / 8.0f * width, height, i / 8.0f * width, height - 5);
			line(0, i / 8.0f * height, 5, i / 8.0f * height);
		}
		
		fill(255);
		textSize(16);
		text("t-0s", width - 20, height - 20);
		text("t-" + DEQUE_SIZE / 8 + "s", width / 2, height - 20);
		text(LOWER_BOUND + "kts/t-" + DEQUE_SIZE / 4 + "s", 60, height - 20);
		text(MIDDLE_BOUND + "kts", 40, height / 2);
		text(UPPER_BOUND + "kts", 40, 10);
		
		// Display knots and timer
		textSize(128);
		text(nf(timer / 1000.0f, 1, 2), width / 2, height / 6);
		text(nf(knots, 1, 2) + "kts", width / 2, height / 2);
		
		// Draw buttons
		selectButton.draw();
		startButton.draw();
		stopButton.draw();
	}
	
	// Called every time data is received from the Arduino
	private void dataReceived() {
		// Read data and store it into the buffer
		arduinoSerial.readBytes(buffer, 1);
		
		// Add the new value to the data deque and poll off a value to maintain size
		data.add(Byte.toUnsignedInt(buffer[0]));
		data.poll();
		
		// Update the knots variable as new values have been added
		updateKnots();
		
		// Update the color variable to reflect the new speed
		if(knots < MIDDLE_BOUND)
			color = color(127, map(knots, LOWER_BOUND, MIDDLE_BOUND, 0, 127), 0);
		else
			color = color(map(knots, MIDDLE_BOUND, UPPER_BOUND, 127, 0), 127, 0);
		
		// Append data to the logfile if we are currently logging
		if(logging) {
			logger.println(timer + "," + knots);
		}
			
	}
	
	// Updates the knots variable by recalculating the current speed in knots based on the amount of flips counted the sample size 
	private void updateKnots() {
		// Count the number of flips in the past samples
		int sumFlips = 0;
		Iterator<Integer> it = data.descendingIterator();
		for(int i = 0; i < SAMPLE_SIZE; i++)
			sumFlips += it.next();
		
		// Return the speed in knots by multiplying the sum by the conversion constant
		knots = sumFlips * CONVERSION_CONSTANT / SAMPLE_SIZE;
	}
	
	// Called every time the mouse is released
	@Override
	public void mouseReleased() {
		// Run the click method of each button if the mouse is over the button when released
		if(selectButton.mouseOver())
			selectButton.click();
		if(startButton.mouseOver())
			startButton.click();
		if(stopButton.mouseOver())
			stopButton.click();
	}
	
	// Called once on program exit
	@Override
	public void exit() {
		// Close the Arduino serial port
		if(arduinoSerial != null)
			arduinoSerial.closePort();
		
		// Terminate the PApplet
		super.exit();
	}
}
