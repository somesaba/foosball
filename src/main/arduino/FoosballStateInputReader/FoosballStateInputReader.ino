#define rowZeroYPositionPin = 3
#define rowZeroAnglePin = 5
#define rowOneYPositionPin = 7
#define rowOneAnglePin = 9

int numOfControllableRows = 2;
int numOfBytesPerCommand = numOfControllableRows * 2;

void setup(){
  Serial.begin(9600);
}

void loop(){
  // Input serial information:
  byte rowYPosition;
  byte rowAngle;
  if (Serial.available() >= numOfBytesPerCommand){
    for(int row = 0; row < numOfControllableRows; row++) {
      rowYPosition = Serial.read();
      rowAngle = Serial.read();
      if(row == 0) {
        analogWrite(rowZeroYPositionPin, rowYPosition);
        analogWrite(rowZeroAnglePin, rowAngle);
    } else {
        analogWrite(rowOneYPositionPin, rowYPosition);
        analogWrite(rowOneAnglePin, rowAngle);
    }
  }
}

