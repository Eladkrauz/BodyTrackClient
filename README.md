# BodyTrack - Android Client

## Overview
This repository contains the Android client application of the BodyTrack system - a real-time body posture analysis and feedback platform developed as a final software engineering project.

The Android application is responsible for capturing live video during training sessions, communicating with the backend server, and presenting real-time feedback to the user through visual and audio cues.

## Application Role
The Android client serves as the user-facing component of the system.

Its main responsibilities include:
- Capturing live camera frames during exercise
- Sending frames to the backend server for analysis
- Receiving posture feedback and session updates
- Presenting feedback clearly and in real time
- Managing the user session lifecycle

All posture analysis and decision logic are handled by the backend server.
The client focuses on interaction, responsiveness, and usability.

## Technologies Used
- Kotlin
- Android SDK
- CameraX - camera frame capture
- Android Jetpack Components
- HTTP networking for server communication
- Text-to-Speech (TTS) for audio feedback

## Application Structure (Simplified)
```
AndroidClient/
│
├── ui/                 # User interface and screens
├── camera/             # Camera handling and frame capture
├── network/            # Server communication layer
├── session/            # Session state and lifecycle
├── feedback/           # Audio and visual feedback logic
└── README.md           # This file
```

The structure follows a modular design, separating camera handling, networking, session management, and user interface concerns.

## Requirements
- Android Studio (latest stable version)
- Android SDK (API level compatible with CameraX)
- Physical Android device with camera (recommended)
- Active backend server instance

## Running the Application
- Clone the repository: ```git clone <repository-url>```
- Open the project in Android Studio
- Allow Gradle to sync and download dependencies
- Connect a physical Android device (or use a compatible emulator)
- Build and run the application from Android Studio

## Permissions
The application requires the following permissions:
- Camera - for capturing live exercise video
- Internet - for communication with the backend server
- Audio output - for spoken feedback (Text-to-Speech)

Permissions are requested at runtime according to Android guidelines.

## Application Flow
- The user starts a training session
- The camera captures live video frames
- Frames are sent to the backend server
- The server analyzes posture and returns feedback
- Feedback is presented to the user in real time
- The session ends and results are summarized

This flow repeats continuously during the active session.

## Server Communication
The client communicates with the backend server over HTTP.

Configuration includes:
- Server IP address
- Server port
- API endpoints

These values are centralized and can be updated without modifying the core application logic.

## Notes and Limitations
- The application assumes a stable network connection
- Performance depends on lighting conditions and camera quality
- Posture accuracy improves with consistent camera placement
- The client is designed to work closely with the backend server and does not perform posture analysis locally.

## Intended Audience
This repository is intended for:
- Developers building or extending the Android client
- Academic reviewers evaluating system implementation
- Future maintainers of the BodyTrack project

End users interact with the application through the provided user interface and do not need to access the source code.

## Academic Context
This Android client was developed as part of a software engineering capstone project, with emphasis on:
- Real-time interaction
- Clean separation of responsibilities
- Maintainable and modular design
- Integration with a distributed backend system

For system-wide architecture, processing logic, and evaluation results, refer to the project documentation and the backend server repository.