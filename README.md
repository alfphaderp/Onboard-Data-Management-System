# Onboard-Data-Management-System
A mini hack project created during my internship at UCLA to intercept, analyze, and log digital speed data signals from an old boat paddlewheel sensor that originally did not come with said features.

<p align="center">
  <img src="./img/display.png" alt="display">
</p>

## Getting Started
These instructions will help you get a copy of the project up and running.

### Prerequisites
You'll need the following before getting started:
<ul>
  <li>An Arduino board</li>
  <li>Java Development Kit</li>
</ul>

### Installation
Upload either sinewave.ino or datarelay.ino to an Arduino board and connect it via serial to the computer. To upload, use the Arduino IDE, which can be found <a href="https://www.arduino.cc/en/Main/Software">here</a>.

Use sinewave.ino for testing ODMS or datarelay.ino for live data logging. sinewave.ino continuously generates flips in a sine wave pattern. datarelay.ino counts and transmits data received from the paddlewheel sensor when connected.

By default, datarelay.ino has digital pin 3 configured for data input. If you are going to use a different pin, be sure to change the value of the #define at the top of the file.

After your Arduino is fully set up, simply plug it in, compile and run ODMS to get the on-screen display and you're good to go!

## Background
During my summer internship at Professor CJ Kim's lab at UCLA, some grad students needed to analyze and log speed data read from an old boat paddlewheel sensor. The sensor was connected to a <a href="http://www.raymarine.com/view/?id=4870">Raymarine i40 Speed</a>, which displayed the current speed of the boat in knots. The i40 uses SeaTalk, a protocol for networking marine equipment, but we did not have any systems at our lab compatiable with SeaTalk, and did not want to spend extra money to buy something that was.

As such, we needed a quick and simple solution to the problem, so I created this project. Jumper wires were soldered on to the side of existing wires connecting the paddlewheel sensor to the i40 in order to intercept the speed digital data signal being transmitted. We wanted to see if we could read the data being transmitted and possibly intercept it for ourselves. After some experimentation, we discovered that the digital signal works by flipping from HIGH to LOW and vice versa every quarter of a paddlewheel rotation. After some calculation and experimentation, we found a way to mathematically convert the digital flips into speed in knots.

Now being able to intercept data as well as read it, we used an <a href="https://mellbell.cc/">Arduino Pico</a> to read data from the intercepted jumper wires. The Arduino counts flips and periodically sends it via serial communication to the boat laptop, where the data is analyzed and logged.

Using the <a href="https://processing.org/">Processing</a> library to quickly make some graphics alongside <a href="https://fazecast.github.io/jSerialComm/">jSerialComm</a> to read serial data, I created a program written in Java that allows users to see the boat speed converted to knots from flips as well as save the data in .csv log files. When I worked on the project without physical access to the paddlewheel, I used a program that simulated boat data in a sine wave so I could see how the display interacts with live data.

All in all, this project was a pretty nifty experience where I got to have a taste of some simple electrical engineering as well as programming in multiple languages for a formal project.

## Authors
<ul>
  <li>Ryan Zhu</li>
</ul>

## Support
If you have any questions, feel free to email me at <a href="mailto:ryanzhu2018@gmail.com">ryanzhu2018@gmail.com</a>
