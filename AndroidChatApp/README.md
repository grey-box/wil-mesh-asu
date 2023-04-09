# GreyboxChatApp 
This chat app is created in Android Studio using Java and Firebase


IMPORTANT:

To modify the Firebase OTP Authentication used in the coding program or add in any additional Firebase features, a Firebase account/project must be relinked to the project.
Go to Firebase and follow the steps to add a new project and when prompted for a "google-services.json" file, replace the one found under the app/ folder with the new file
given by the Firebase project. Make sure to add the new "SHA-1" and "SHA-256" Keys too under the Firebase Project Properties which can be found using ./gradlew signingReport.

After adding Authentication, make sure to add Firestore Database, Realtime Database, and Storage all with read/write permissions set to true or until a certain date if it
is for testing. There should be no need to relink anything in the code as the new json file will connect to these new location.
