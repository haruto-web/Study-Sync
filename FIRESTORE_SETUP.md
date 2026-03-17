# Study-Sync Firestore Schema & Configuration Guide

## Overview
This document outlines the Firestore database structure, security rules, and how to set it up in your Firebase Console.

---

## Firestore Collections Structure

### 1. **users** Collection
Stores user profile information

```
users/
├── {userId}/
│   ├── userId (string)
│   ├── email (string)
│   ├── fullName (string)
│   ├── profileImageUrl (string, optional)
│   ├── bio (string, optional)
│   ├── createdAt (timestamp)
│   ├── updatedAt (timestamp)
│   ├── lastLogin (timestamp)
│   ├── totalQuizzesTaken (integer)
│   ├── totalTasksCompleted (integer)
│   ├── totalStudyMinutes (integer)
│   ├── averageQuizScore (double)
│   └── quizAttempts/ (subcollection)
```

**Index**: `userId`, `createdAt`

---

### 2. **quizzes** Collection
Stores quiz metadata and questions

```
quizzes/
├── {quizId}/
│   ├── quizId (string)
│   ├── userId (string) - Quiz creator
│   ├── title (string)
│   ├── description (string)
│   ├── totalQuestions (integer)
│   ├── passingScore (double) - e.g., 60.0 for 60%
│   ├── subject (string) - e.g., "Math", "Science"
│   ├── difficulty (integer) - 1-5 scale
│   ├── isArchived (boolean)
│   ├── createdAt (timestamp)
│   ├── updatedAt (timestamp)
│   └── questions/ (subcollection)
```

**Indexes**: 
- `userId`, `isArchived`, `createdAt`
- `subject`, `isArchived`, `createdAt`
- `difficulty`, `isArchived`, `createdAt`

---

### 3. **quizzes/{quizId}/questions** Subcollection
Stores individual questions within a quiz

```
quizzes/{quizId}/questions/
├── {questionId}/
│   ├── questionId (string)
│   ├── quizId (string)
│   ├── questionText (string)
│   ├── optionA (string)
│   ├── optionB (string)
│   ├── optionC (string)
│   ├── optionD (string)
│   ├── correctAnswer (string) - "A", "B", "C", or "D"
│   ├── questionNumber (integer)
│   └── createdAt (timestamp)
```

**Index**: `quizId`, `questionNumber`

---

### 4. **tasks** Collection
Stores user's study tasks/to-do items

```
tasks/
├── {taskId}/
│   ├── taskId (string)
│   ├── userId (string)
│   ├── title (string)
│   ├── description (string)
│   ├── dueDate (timestamp)
│   ├── isCompleted (boolean)
│   ├── priority (string) - "LOW", "MEDIUM", "HIGH"
│   ├── category (string) - Topic or subject area
│   ├── createdAt (timestamp)
│   ├── updatedAt (timestamp)
│   └── completedAt (timestamp, nullable)
```

**Indexes**:
- `userId`, `isCompleted`, `dueDate`
- `userId`, `priority`, `isCompleted`
- `userId`, `category`, `isCompleted`

---

### 5. **timerSessions** Collection
Stores user's study timer session records

```
timerSessions/
├── {sessionId}/
│   ├── sessionId (string)
│   ├── userId (string)
│   ├── startTime (timestamp)
│   ├── endTime (timestamp, nullable)
│   ├── durationMinutes (integer) - Planned duration
│   ├── actualDurationMinutes (integer) - Actual time spent
│   ├── subject (string)
│   ├── notes (string, optional)
│   ├── isCompleted (boolean)
│   ├── isPaused (boolean)
│   ├── pausedDuration (integer) - Total pause time in milliseconds
│   └── createdAt (timestamp)
```

**Indexes**:
- `userId`, `isCompleted`, `startTime`
- `userId`, `subject`, `isCompleted`

---

### 6. **users/{userId}/quizAttempts** Subcollection
Stores quiz attempts/results for each user

```
users/{userId}/quizAttempts/
├── {attemptId}/
│   ├── attemptId (string)
│   ├── userId (string)
│   ├── quizId (string)
│   ├── scorePercentage (double)
│   ├── questionsAttempted (integer)
│   ├── correctAnswers (integer)
│   ├── timeTakenMinutes (integer)
│   ├── attemptedAt (timestamp)
│   ├── passed (boolean)
│   └── answers (string) - JSON: {"q1": "A", "q2": "B", ...}
```

**Index**: `userId`, `quizId`, `attemptedAt`

---

## Firestore Security Rules

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Allow authenticated users to read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      
      match /quizAttempts/{attemptId} {
        allow read, write: if request.auth.uid == userId;
      }
    }

    // Quizzes: Anyone can read, but only owner can write
    match /quizzes/{quizId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
      
      match /questions/{questionId} {
        allow read: if request.auth != null;
        allow create, update, delete: if request.auth != null && 
          get(/databases/$(database)/documents/quizzes/$(quizId)).data.userId == request.auth.uid;
      }
    }

    // Tasks: Users can only read/write their own
    match /tasks/{taskId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }

    // Timer Sessions: Users can only read/write their own
    match /timerSessions/{sessionId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow write: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }

    // Deny all other access
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## Setup Instructions

### Step 1: Create Collections in Firebase Console
1. Go to Firebase Console → Firestore Database
2. Create the following collections in order:
   - `users`
   - `quizzes`
   - `tasks`
   - `timerSessions`

### Step 2: Create Indexes
1. Go to Firebase Console → Firestore → Indexes
2. Create composite indexes as specified above under each collection

### Step 3: Set Security Rules
1. Go to Firebase Console → Firestore → Rules
2. Copy and paste the security rules from the section above
3. Click "Publish"

### Step 4: Enable Authentication
1. Go to Firebase Console → Authentication
2. Enable "Email/Password" authentication (already done in your app)

### Step 5: Enable Storage (for Profile Pictures)
1. Go to Firebase Console → Storage
2. Create a storage bucket with default rules
3. Update rules to allow authenticated users:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profilePictures/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
  }
}
```

---

## Data Flow Example: Creating a Quiz

1. **App Level** → User creates quiz in QuizFragment
2. **Repository** → `QuizRepository.createQuiz(quiz, userId)`
3. **Firestore** → Saves to `quizzes/{quizId}`
4. **Room** → Saves to local database for offline access
5. **ViewModel** → Updates UI with new quiz in list

---

## Data Sync Strategy

### On App Launch:
```
FirestoreSyncUtil.syncAllData(userId) →
- Sync user profile
- Sync quizzes
- Sync tasks
- Sync timer sessions
- Sync quiz attempts
```

### Periodic Sync (Optional - using WorkManager):
```
Every 30 minutes (configurable):
- Call FirestoreSyncUtil.performPeriodicSync()
- Syncs all data in background
```

### Manual Refresh:
```
User pulls-to-refresh or clicks refresh button:
- Call FirestoreSyncUtil.syncAllData(userId)
```

---

## Offline Support

- **Read**: Always reads from Room (local database)
- **Write**: 
  - If online: Writes to Firestore first, then Room
  - If offline: Writes to Room only, queued for sync when online
- **Sync**: Automatic on app launch if online

---

## Performance Tips

1. **Pagination**: For large lists, implement pagination with `limit(20)` queries
2. **Caching**: Room provides automatic caching
3. **Indexes**: Create indexes for frequently sorted/filtered queries
4. **Real-time Updates**: Use `addSnapshotListener()` only for critical data (user profile, active session)

---

## Testing in Firebase Console

### Create Test Data:

**Sample User:**
```json
{
  "userId": "user123",
  "email": "user@example.com",
  "fullName": "John Doe",
  "createdAt": (current timestamp)
}
```

**Sample Quiz:**
```json
{
  "quizId": "quiz123",
  "userId": "user123",
  "title": "Math Basics",
  "subject": "Math",
  "difficulty": 2,
  "totalQuestions": 5,
  "passingScore": 60.0,
  "isArchived": false,
  "createdAt": (current timestamp)
}
```

---

## Migration Path (if coming from REST API)

If your app previously used a REST API, you can:
1. Create a one-time data migration script
2. Map old data to Firestore collections
3. Update app to use new Firestore repositories
4. Keep old API endpoints for fallback (optional)

---

## Troubleshooting

- **Permission Denied**: Check security rules and user authentication
- **Data Not Syncing**: Check network connectivity with `NetworkUtil.isNetworkAvailable()`
- **Slow Queries**: Check if indexes are created for your queries
- **Data Inconsistency**: Force sync with `FirestoreSyncUtil.syncAllData(userId)`

---

For more info, visit: https://firebase.google.com/docs/firestore
