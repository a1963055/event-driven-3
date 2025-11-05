# GPS Tracker

## Run GUI
```bash
javac -cp ".;sodium.jar" Example.java GpsEvent.java GpsService.java GpsGUI.java
java -cp ".;sodium.jar" GpsGUI
```

## Run Tests
```bash
javac -cp ".;sodium.jar;junit.jar;hamcrest-core.jar" GpsGUI_Test.java
java -cp ".;sodium.jar;junit.jar;hamcrest-core.jar" org.junit.runner.JUnitCore GpsGUI_Test
```

