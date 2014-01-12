#include <Servo.h> 


int rowZeroYPositionPin = 3;
int rowZeroAnglePin = 5;
int rowOneYPositionPin = 6;
int rowOneAnglePin = 9;
int ledPin = 13;


Servo rowZeroYPositionServo;
Servo rowZeroAngleServo;
Servo rowOneYPositionServo;
Servo rowOneAngleServo;

int bytesRead = 0;
int data = 0;

void setup(){
  //Serial
  Serial.begin(9600);

  //Attach Servos
  rowZeroYPositionServo.attach(rowZeroYPositionPin);
  rowZeroAngleServo.attach(rowZeroAnglePin);
  rowOneYPositionServo.attach(rowOneYPositionPin);
  rowOneAngleServo.attach(rowOneAnglePin);
  rowZeroYPositionServo.write(90);
  rowZeroAngleServo.write(90);
  rowOneYPositionServo.write(90);
  rowOneAngleServo.write(90);
  
  bytesRead = 0;
}

void loop(){
  // Input serial information:
  
  if (Serial.available() >= 1) {
      data = Serial.read();
      if(data == 255) {
        bytesRead = -1;
      } else if(bytesRead == 0) {
        rowZeroYPositionServo.write(data);
      } else if(bytesRead == 1) {
        rowZeroAngleServo.write(data);
      } else if(bytesRead == 2) {
        rowOneYPositionServo.write(data);
      } else {
        rowOneAngleServo.write(data);
      }
      bytesRead++;
      if(bytesRead == 4) {
        bytesRead = 0;
      }
  }
  //  digitalWrite(ledPin, HIGH);
  //  rowZeroYPositionServo.write(85);
  //  rowZeroAngleServo.write(0);
  //  rowOneYPositionServo.write(65);
  //  rowOneAngleServo.write(0);
  //  delay(1000);
  //  digitalWrite(ledPin, LOW);
  //  rowZeroYPositionServo.write(124);
  //  rowZeroAngleServo.write(180);
  //  rowOneYPositionServo.write(105);
  //  rowOneAngleServo.write(180);
  //  delay(1000);

}
