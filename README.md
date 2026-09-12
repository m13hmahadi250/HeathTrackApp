# Heath Track

Heath Track is a modern Android personal wellness application that brings fitness, nutrition, hydration, sleep, habit tracking, and Islamic Salat management together in a single platform.

The application is designed around a simple principle: users should be able to manage their everyday health and wellness activities from one place while maintaining complete control over their personal data.

Heath Track combines real user-generated health information, Firebase cloud synchronization, offline-first functionality, Android Health Connect integration, personalized reminders, and location-based prayer calculations to provide a practical everyday wellness experience.

---

## Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Fitness Tracking](#fitness-tracking)
- [Nutrition Management](#nutrition-management)
- [Hydration Tracking](#hydration-tracking)
- [Sleep Tracking](#sleep-tracking)
- [Habit Tracking](#habit-tracking)
- [Salat Tracking](#salat-tracking)
- [Qibla](#qibla)
- [Prayer Time System](#prayer-time-system)
- [Notifications and Reminders](#notifications-and-reminders)
- [Health Connect Integration](#health-connect-integration)
- [Authentication](#authentication)
- [Firebase Integration](#firebase-integration)
- [Offline-First Architecture](#offline-first-architecture)
- [Data Privacy and Security](#data-privacy-and-security)
- [Performance](#performance)
- [User Experience](#user-experience)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Data Management](#data-management)
- [Application Flow](#application-flow)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Firebase Configuration](#firebase-configuration)
- [Health Connect Configuration](#health-connect-configuration)
- [Permissions](#permissions)
- [Build and Run](#build-and-run)
- [Testing](#testing)
- [Security Considerations](#security-considerations)
- [Development Principles](#development-principles)
- [Future Improvements](#future-improvements)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Heath Track is designed as a personal wellness companion for everyday users.

Instead of treating fitness, nutrition, hydration, sleep, habits, and Salat as completely separate systems, the application connects them into a unified daily experience.

Users can:

- Track their physical activity
- Record meals and nutrition
- Monitor daily water consumption
- Track sleep
- Create personal habits
- Track the five daily prayers
- View prayer times based on their location
- Receive prayer and wellness reminders
- Check the Qibla direction
- Synchronize supported health data through Android Health Connect
- Store personal information securely through Firebase
- Continue using supported features without an internet connection
- Review daily, weekly, and monthly progress

The application is designed to be simple enough for everyday use while maintaining a scalable architecture suitable for future development.

---

# Core Features

## 1. Personal Dashboard

The dashboard provides a centralized overview of the user's current day.

Depending on available data, it can display:

- Daily activity
- Steps
- Exercise
- Nutrition
- Hydration
- Sleep
- Habits
- Salat progress
- Upcoming reminders
- Wellness summary

The dashboard is based on real user data.

New accounts start with an empty state rather than artificial statistics or sample records.

---

# Fitness Tracking

Heath Track provides tools for monitoring daily physical activity.

Supported areas include:

- Step tracking
- Walking
- Running
- Cycling
- Exercise tracking
- Workout sessions
- Workout history
- Exercise library
- Workout timer
- Rest timer
- Activity goals
- Weekly activity summaries
- Monthly activity trends

Where supported, activity information can be synchronized through Android Health Connect.

The application does not generate artificial activity records.

---

# Nutrition Management

The nutrition system allows users to record and monitor their food consumption.

Users can record:

- Breakfast
- Lunch
- Dinner
- Snacks
- Food items
- Portion sizes
- Meal times

The application can provide nutritional information such as:

- Calories
- Protein
- Carbohydrates
- Fat
- Fiber

Nutrition information should be based on reliable food data or user-provided information.

The application can also maintain:

- Daily food history
- Weekly food history
- Monthly food history
- Frequently consumed foods
- Nutrition summaries
- Meal patterns

The system is designed to support common Bangladeshi foods and everyday meals.

---

# Hydration Tracking

The hydration system helps users monitor their daily water intake.

Features include:

- Personalized hydration targets
- One-tap water logging
- Custom glass sizes
- Custom bottle sizes
- Daily hydration progress
- Weekly hydration history
- Monthly hydration history
- Water reminders
- Offline water logging

Users can quickly record water consumption without navigating through complicated screens.

---

# Sleep Tracking

The sleep system provides tools for monitoring sleep patterns.

Features include:

- Sleep duration
- Bedtime
- Wake-up time
- Sleep history
- Sleep consistency
- Bedtime reminders
- Weekly sleep summaries
- Monthly sleep trends
- Health Connect synchronization

Sleep information can be entered manually or synchronized from supported health sources.

---

# Habit Tracking

Users can create and monitor personal habits.

Examples include:

- Drink water
- Exercise
- Walk
- Sleep on time
- Eat balanced meals
- Read
- Pray
- Custom personal habits

Habit tracking supports:

- Daily completion
- Weekly progress
- Monthly progress
- Habit history
- Completion statistics

The system is designed to encourage consistency without using excessive or guilt-based messaging.

---

# Salat Tracking

Heath Track includes a dedicated Salat section for users who want to track their five daily prayers.

The five prayers are:

- Fajr
- Dhuhr
- Asr
- Maghrib
- Isha

Users can:

- View today's prayer schedule
- See the current prayer
- See the next prayer
- View a live countdown
- Mark prayers as completed
- Review prayer history
- Receive prayer notifications
- Configure prayer reminders

The Salat system is designed to work independently from the fitness features while still being part of the overall daily wellness experience.

---

# Qibla

Heath Track can provide Qibla direction using the device's available sensors.

The Qibla feature is designed to work offline where the required device sensors are available.

The application should clearly handle:

- Missing sensors
- Sensor calibration
- Permission limitations
- Location requirements

No internet connection should be required for the basic compass functionality.

---

# Prayer Time System

Prayer times are dynamically calculated according to the user's selected configuration.

Relevant factors include:

- Location
- Date
- Timezone
- Calculation method
- Madhab

The location system is focused on Bangladesh and supports cities such as:

- Dhaka
- Chattogram
- Sylhet
- Rajshahi
- Khulna
- Barishal
- Rangpur
- Mymensingh
- Cox's Bazar

The application should display the user's selected location independently from the calculation method.

For example:

`Chattogram, Bangladesh`

rather than combining a calculation method with the location name.

Prayer times are recalculated as dates change because prayer times vary throughout the year.

---

# Calculation Method

Users can select an appropriate prayer calculation method from the available supported methods.

The system can support commonly used methods such as:

- Muslim World League
- University of Islamic Sciences, Karachi
- Egyptian General Authority of Survey
- Umm Al-Qura University, Makkah
- Institute of Geophysics, University of Tehran
- Moonsighting Committee

The application also supports Madhab selection for Asr calculation where applicable:

- Shafi
- Maliki
- Hanbali
- Hanafi

The selected configuration is used consistently across:

- Prayer schedules
- Next-prayer calculations
- Countdown timers
- Notifications
- Daily prayer calculations

---

# Location and Timezone

Users can either:

- Use their device location
- Manually select a Bangladesh city

The application stores the selected location and timezone as part of the user's configuration.

Prayer calculations respond to:

- Location changes
- Date changes
- Timezone changes
- Calculation method changes
- Madhab changes

The next prayer is determined using the actual current device time and the calculated prayer schedule.

---

# Offline Prayer Support

Heath Track follows an offline-first approach wherever technically possible.

When the device is offline:

- Previously calculated prayer schedules remain available
- Prayer countdowns continue using the device clock
- Prayer completion can be recorded
- Qibla can continue working where device sensors are available
- Previously scheduled notifications can continue
- User changes can be stored locally

When connectivity returns:

- Data is synchronized with Firebase
- Updated prayer schedules can be retrieved or recalculated
- Future notification schedules can be updated
- Pending user data can be synchronized

The application must never use fake prayer times to fill missing information.

---

# Islamic Date

The Salat section can display:

- Gregorian date
- Hijri date

The Hijri date is presented as a calculated date and may vary slightly depending on the calculation convention used.

---

# Notifications and Reminders

Heath Track provides a centralized reminder system.

Supported reminders include:

- Prayer reminders
- Prayer start notifications
- Water reminders
- Meal reminders
- Exercise reminders
- Sleep reminders
- Habit reminders
- Custom reminders

Notifications use Android notification channels and platform-appropriate background scheduling.

Users can control:

- Notification enable/disable state
- Individual prayer notifications
- Reminder timing
- Sound
- Vibration
- Haptic feedback where supported

On Android versions requiring notification permission, the application requests permission appropriately.

Notifications are designed to continue working when the application is not open.

---

# Haptic Feedback

The application can provide subtle haptic feedback for important interactions.

Examples include:

- Button actions
- Completing a prayer
- Logging water
- Completing a habit
- Starting or completing an activity
- Successful operations

Haptic feedback should remain subtle and can be disabled through settings.

---

# Health Connect Integration

Heath Track is designed to integrate with Android Health Connect.

Supported health information may include:

- Steps
- Exercise
- Distance
- Sleep
- Other supported health metrics

Health Connect permissions are requested only when required.

The application does not create artificial health information when Health Connect data is unavailable.

If the user has not granted permission, the application clearly communicates that health data is not connected.

---

# Google Authentication

Heath Track uses Firebase Authentication with Google Sign-In.

Authentication flow:

```text
Application
    |
    v
Login Screen
    |
    v
Continue with Google
    |
    v
Google Account Selection
    |
    v
Firebase Authentication
    |
    v
Authenticated Firebase UID
    |
    v
User Profile
    |
    v
Heath Track Dashboard
