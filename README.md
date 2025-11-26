# DetectAI App  
An open-source Android application that detects whether text or images are AI-generated. The app connects to a custom ML backend hosted on Hugging Face Spaces and provides fast, simple, and accurate detection.

---

## 📱 Features

- Detects AI-generated **text**
- Detects AI-generated **images**
- Clean and modern Android UI
- Fast responses
- Local history of past checks
- Light and dark theme support
- Privacy-friendly

---

## 🧠 How It Works

DetectAI communicates with a Flask-based API that runs multiple machine learning models to determine whether content is AI-generated.

### Text Detection  
Model used:
- `Hello-SimpleAI/chatgpt-detector-roberta`

### Image Detection  
Models used (ensemble):
- `umm-maybe/AI-image-detector`
- `Organika/sdxl-detector`

The app sends text or image data to the backend and receives a probability score and a human-readable explanation.

---

## 🌐 Backend API

Backend hosted on Hugging Face Spaces:  
`https://kushalkv-detectai-api.hf.space/`

### Endpoints
| Method | Endpoint        | Description                   |
|--------|------------------|-------------------------------|
| GET    | `/health`        | Health/status check           |
| POST   | `/analyze`       | Text AI detection             |
| POST   | `/analyze-image` | Image AI detection            |

### Android Base URL  
```kotlin
const val BASE_URL = "https://kushalkv-detectai-api.hf.space/"
```

The backend uses a Docker-based Space with a `Dockerfile` and CI/CD syncing via GitHub Actions.

---

## 🛠️ Tech Stack

### Mobile
- Kotlin / Android Studio
- Fragments + Activities
- ViewBinding
- RecyclerView (History, FAQ)
- Custom UI animations

### Backend
- Python + Flask
- Hugging Face Transformers
- Docker (Hugging Face Spaces)

---

## 📦 Installation

### Install via APK  
1. Go to the **Releases** section on GitHub  
2. Download the latest `DetectAI.apk`  
3. Install it on your Android device (enable "Install from unknown sources")

---

## 📁 Releases & APK Distribution

The official APK is available in GitHub Releases.

Releases include:
- Version tag  
- Changelog / notes  
- Downloadable APK asset (`DetectAI.apk`)  

---

## 🤝 Contributing

Contributions, improvements, and feature suggestions are welcome.

To contribute:
1. Fork the repository  
2. Create a new branch  
3. Commit your changes  
4. Submit a pull request  

Issues can be opened for bugs or feature requests.

---
