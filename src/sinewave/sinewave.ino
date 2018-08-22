/*
 * Program that continuously sends data in the form of a sine wave. Used for testing ODMS without the need to connect to the paddlewheel sensor.
 */

// Called once at the start of the program
void setup() {
  // Begin serial at 9600 baud
  Serial.begin(9600);
}

float value = 0;

// Called continuously
void loop() {
  // Transmit data that forms a sine wave every 250 milliseconds
  Serial.write(round(75 + 75 * sin(value)));
  
  value += PI / 25;
  delay(250);
}
