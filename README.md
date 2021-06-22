# DataCrypt
An application that helps to securely and conveniently store user data.
# Features
- File manager
- File encryption
- Private folder
- Password manager
- Autofill service
- Upload encrypted files to Google Drive
- Encrypted notes
- Backup the database from the password manager
- MessageCrypt
- Dark and light theme
- Changeable application icon
- Log in by password or fingerprint
# Issues
- Dark theme doesn't work on some devices
# Building app
- Install Android Studio if you haven't already
- Open Android Studio
- Select "Get from Version Control"
- In URL paste this: https://www.github.com/Suslanium/DataSec
- Wait until gradle build finishes
- You already can build this app, but all Google Drive-related features won't work. To fix this, follow instructions below:
# Setting up Google Drive
- Open Google Developer Console in your browser(https://console.cloud.google.com/)
- Sign in to your Google Account
- Agree with Terms of Service
- Create new project (Select Project -> New Project)
- Set any project name(For example, "Encryptor")
- Create project
- Select your new project
- Scroll down to "Getting started" and press "Explore and enable APIs"
- Press "Enable APIs and services"
- Search for "Google Drive API"
- Open it and press "Enable"
- Once Google Drive API dashboard is opened, press "Create credentials"
- In "Which API are you using?" select "Google Drive API"
- In "What data will you be accessing?" select "User data"
- Press next
- Enter your app name(For example, "Encryptor")
- Select any user support email
- Enter your email address in "Developer contact information"
- Press "Save and continue"
- Press "Add or remove scopes"
- Select following scopes:".../auth/userinfo.email",".../auth/userinfo.profile","openid",".../auth/drive.appdata",".../auth/drive.file",".../auth/drive.install"
- Press "Update"
- Scroll down, press "Save and Continue"
- In "OAuth Client ID" under application type select "Android"
- Set any name(For example, "Encryptor")
- Set "Package name" to "com.suslanium.encryptor"
- In "SHA-1 certificate fingerprint" enter SHA-1 certificate fingerprint from your keystore. If you don't have one, do this:
Open project in Android Studio, on right side press "Gradle". After a menu pops up, open "Encryptor", then "Tasks", then "android" and double tap "signingReport". After this task is executed, find "SHA1" in run window and copy the string after it. This string is a required fingerprint. But warning: this fingerprint will work only on debug builds, so you'll need to create your own keystore later, get it's certificate fingerprint, update it in Google Developer Console and put the path to your keystore in build.gradle(uncomment line "storeFile file(path-to-your-keystore)").
- Press "Create"
- Press "Done"
- Select "OAuth consent screen"
- Press "Publish app"
- Done! Now you can build this app and use all it's features :)

# About
This application is a project of one of the students of Samsung IT School.
