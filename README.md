# YAEN - Yet Another Encrypted Notepad

Yet Another Encrypted Notepad - a Notepad-like simple text editor where files are saved (and later loaded) encrypted with industrial strength algorithms. Available both for the desktop (as a Java application) and on Android.

Desktop and encryption part based on originl [Encrypted Notepad](https://sourceforge.net/projects/enotes)

Desctop application
<img src="https://raw.github.com/AlexBesk/yaen/master/doc/images/05_desktop_app.png" >
YAEN main screen
<img src="https://raw.github.com/AlexBesk/yaen/master/doc/images/01_text.png" width="300">
YAEN recent files list
<img src="https://raw.github.com/AlexBesk/yaen/master/doc/images/02_recent_files.png" width="300">
YAEN swipe
<img src="https://raw.github.com/AlexBesk/yaen/master/doc/images/03_recent_files_swipe.png" width="300">
YAEN settings
<img src="https://raw.github.com/AlexBesk/yaen/master/doc/images/04_settings.png" width="300">

Original description:

Encrypted Notepad is a very simple to use application providing state of the art industrial-strength encryption to users who want nothing more than to store sensitive information in text documents. Users can be completely at ease in the security provided by Encrypted Notes because it follows best practices in dealing with encryption algorithms and does not try to do anything that is not standard procedure. This is important because "being too clever" is one of the most common reasons for security failures in the application of cryptography.

Documents encrypted with Encrypted Notes can never be recovered if the password is lost. Data encryption is done using AES-128 in CBC mode, with SHA-1 for purposes of computing hash digests.

# Notes about Android application

Android by nature have a lot of internal build-in mechanism to store application state between application switchover or screen orientation changes. In normal case it is OK and it is what you want - seamless user experience.
In case of YAEN it is bad idea to store Undo/Redo buffer or EditText content to the temporary files. So here's where the Paranoid Mode comes in handy. When the Paranoid Mode is On nothing to save in the temporary files.
The downside is that the app is losing its state, so it's better to use Paranoid Mode in conjunction with the Autoseva feature.

# License

Source is released (on GitHub) under the BSD license.

# Contact
<alex.bes.work@gmail.com>
