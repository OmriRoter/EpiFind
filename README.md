# EpiFind

EpiFind is an Android application designed to help users with severe allergies by providing real-time location tracking and emergency assistance alerts. The app enables users to share their location and receive notifications when nearby individuals may need assistance with an EpiPen. EpiFind integrates Firebase for user management and data storage, and utilizes various Android services to ensure continuous operation in the background.

![EpiFind Logo](path/to/logo.png)

## Features

- **Real-time Location Tracking**: Continuously tracks the user's location and updates it in Firebase.
- **SOS Alerts**: Sends notifications to nearby users when someone requires assistance with an EpiPen.
- **EpiPen Expiry Check**: Notifies users when their EpiPen is about to expire.
- **Dark Mode**: Supports both light and dark themes based on user preference.
- **Profile Management**: Allows users to manage their profiles, including updating allergies and EpiPen details.

## Installation

1. Clone the repository:
    ```bash
    git clone https://github.com/yourusername/EpiFind.git
    ```
2. Open the project in Android Studio.

3. Set up Firebase for the project:
   - Create a Firebase project.
   - Add the `google-services.json` file to the `app` directory.
   - Enable Firebase Authentication, Realtime Database, and Cloud Messaging.

4. Build and run the project on an Android device or emulator.

## Project Structure

- **`activities`**: Contains the main activities of the application, such as `LoginActivity` and `MainActivity`.
- **`adapters`**: Includes adapters for RecyclerView and ListView, such as `AllergyAdapter` and `UserAdapter`.
- **`fragments`**: Contains fragments like `HomeFragment`, `ProfileFragment`, `SOSFragment`, and `SettingsFragment`.
- **`managers`**: Manages core functionalities, such as `UserManager`, `SOSManager`, and `LocalNotificationManager`.
- **`models`**: Defines the data models used in the application, such as `UserProfile`.
- **`services`**: Contains services like `LocationUpdateService` that run in the background to provide continuous functionality.
- **`utils`**: Utility classes such as `EpiPenExpiryChecker` for additional support functions.

## Screenshots

### Main Screen

![Main Screen](path/to/main_screen.png)

## Usage

1. **Login**: Users can log in using email, phone, or Google account. Upon first login, users must complete their profile.
2. **Profile Management**: Users can update their name, allergies, and EpiPen details from the profile screen.
3. **Location Sharing**: The app tracks the user's location and updates it in the database for emergency purposes.
4. **SOS Alert**: Users can trigger an SOS alert that notifies nearby users who have an EpiPen.
5. **Notifications**: Users receive notifications for SOS alerts and when their EpiPen is about to expire.

## Contributing

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch-name`).
3. Make your changes.
4. Commit your changes (`git commit -m 'Add some feature'`).
5. Push to the branch (`git push origin feature-branch-name`).
6. Open a pull request.


## Contact

For any inquiries or issues, please contact [Omri99Roter@Gmail.com](mailto:your-email@example.com).
