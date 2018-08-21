package odms.ui;

import processing.core.PApplet;
import processing.core.PConstants;

// A simple modular button class with parameters for position, text, and a Runnable to execute on a separate thread upon clicking
public class Button {
	// Processing Applet context
	private PApplet app;
	
	// Positional variables
	private int x, y, w, h;
	
	// Display text
	private String text;
	
	// Runnable that is executed on a separate thread upon clicking
	// The Runnable is run on a separate thread to prevent blocking in the main program
	private Runnable onClick;
	
	// Boolean indicating whether or not the button is visible
	private boolean visible;
	
	// Simple constructor
	public Button(PApplet app, int x, int y, int w, int h, String t, Runnable oc, boolean v) {
		this.app = app;
		
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.text = t;
		this.onClick = oc;
		this.visible = v;
	}
	
	// Returns true if the mouse is within the boundaries of the button
	public boolean mouseOver() {
		return app.mouseX > x && app.mouseX < x + w && app.mouseY > y && app.mouseY < y + h; 
	}
	
	// Draws the button as a gray rounded rectangle, with different shades of color depending on mouse state
	public void draw() {
		if(visible) {
			app.stroke(255);
			app.strokeWeight(5);
			
			if(mouseOver())
				if(app.mousePressed)
					// Fill the button with a dark gray when it is being pressed
					app.fill(16);
				else
					// Fill the button with a light gray when it is being hovered
					app.fill(48);
			else
				// Fill the button with a medium gray otherwise
				app.fill(32);
			
			app.rect(x, y, w, h, 10);
			
			app.fill(255);
			app.textAlign(PConstants.CENTER, PConstants.CENTER);
			app.textSize(48);
			
			app.text(text, x + w / 2, y + 2 * h / 5);
		}
	}
	
	// Called when the button is clicked and runs the onClick runnable on a new thread
	public void click() {
		if(visible)
			new Thread(onClick).start();
	}
	
	// Change the visibility of the button
	public void setVisibility(boolean v) {
		visible = v;
	}
}