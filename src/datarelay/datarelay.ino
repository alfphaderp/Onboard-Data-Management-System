/*
 * Program that continuously counts the number of digital flips from the paddlewheel sensor every 250 milliseconds and transmits it via serial to the computer.
 */

// Set the pin for reading speed data to pin 3
#define SPEEDPIN 3

// Called once at the start of the program
void setup() {
  // Begin serial at 9600 baud
  Serial.begin(9600);

  // Configure the speed pin to accept input
  pinMode(SPEEDPIN, INPUT);
}

// Previous time of data transmission
unsigned long previousTime = 0;

// Previous state of the speed pin
boolean previousState = digitalRead(SPEEDPIN);

// Number of flips counted
unsigned int flips = 0;

// Called continuously
void loop() {
  // Poll for current speed pin state and time in milliseconds
  boolean currentState = digitalRead(SPEEDPIN);
  long currentTime = millis();

  // If a change in state has occurred, negate the previous state and add to the flip count
  if(currentState != previousState) {
    previousState = !previousState;
    flips++;
  }

  // If it has been 250 milliseconds since last transmission, send a new transmission and reset time and flips
  if(currentTime - previousTime >= 250) {
    Serial.write(flips);
        
    previousTime = currentTime;
    flips = 0;
  }
}

