Mobile Application (Android Studio)

To run this Android project:

1. Install the latest version of the Android Studio IDE as well as the Java Development Kit (JDK) version 17 or higher for compatibility with both Android and Spring Boot applications. 
2. Upon installation, open Android Studio and open the project folder. Initiate the Gradle synchronization, which may take several minutes. 
3. Connect a physical or virtual device
	a. A physical Android device can be connected by enabling USB debugging on the Android device. Once enabled, connect the device to the PC, select any “allow” options on any prompted dialog box confirming debugging on your phone and/or IDE, and look for your device name on the IDE’s header bar.
	b. A virtual Android device can be connected by creating an Android Virtual Device (AVD), choosing a device type and running the device through an emulator provided by Android Studio.
4. Finally, select the green play button to run the project. Ensure that the backend application is running to supplement the mobile application with data.

Backend Application (Spring Boot)

1. As previously instructed, ensure that JDK 17 or higher is installed. Additionally, download Apache Maven version 3.6 or higher to facilitate Java application building and compiling
2. Using the terminal, navigate to the project folder and build the project by executing:
	mvn clean install
3. Run the application by running:
	mvn spring-boot:run
4. The API will be available at a locally hosted web server environment at http://localhost:8080 (unless modified otherwise).
