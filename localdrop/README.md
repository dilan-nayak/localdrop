# 🔒 LocalDrop: Secure LAN-based File & Clipboard Sharing

LocalDrop is a full-stack cross-platform file and clipboard sharing app that works entirely on your **local network** without needing the Internet or cloud. Think of it as your private, secure AirDrop + LAN chat for macOS, Windows, and Linux!

---

## 🚀 Features

| Feature                      | Description                                                                 |
|------------------------------|-----------------------------------------------------------------------------|
| 📂 File Upload & Download    | Share files with AES-256 encryption and RSA-4096 secure key exchange        |
| 💬 Clipboard Sync (Chat)     | Real-time LAN chat using WebSocket for sharing clipboard or text            |
| 🔐 End-to-End Encryption     | AES-256 for file content, RSA-4096 for key exchange                         |
| 🌐 Local-Only Access         | No cloud or internet required; all traffic stays inside your LAN            |
| 📡 Peer Discovery (Planned)  | Placeholder for mDNS device discovery (like AirDrop)                        |
| 📜 Public Key Sharing        | Displays your RSA public key for peer encryption                            |
| ♻️ Auto Refresh              | Files auto-refresh in all connected peers after upload                      |
| 📱 Browser Interface         | Fully responsive UI built with Bootstrap 5                                  |

---

## 🏗️ Project Structure

```
localdrop/
├── backend/
│   ├── config/                 # WebSocket configuration
│   ├── controller/             # REST & WebSocket endpoints
│   ├── model/                  # Data model classes
│   ├── service/                # Core logic (encryption, file I/O, messaging)
│   ├── util/                   # Crypto utilities (AES/RSA/mDNS)
│   ├── repository/             # (Optional for DB support)
│   └── storage/                # Encrypted file store (.data/.key)
│   └── config/                 # Private/public keys
│   └── LocalDropApplication.java
│   └── pom.xml                 # Maven dependencies
├── frontend/
│   ├── app.html                # Main UI
│   ├── assets/
│   │   ├── js/main.js          # Upload, chat, refresh
│   │   ├── css/style.css       # Custom styling
├── run.sh / run.bat           # Startup scripts
├── README.md                  # You're reading it ;)
```

---

## ⚙️ How It Works (Flow)

When two or more devices on the same Wi-Fi network open LocalDrop in their browsers, each of them connects to their own local Spring Boot server running on their machine. The main web interface (`app.html`) loads using plain HTML + Bootstrap, and its interactivity is driven by `main.js`. When a user wants to share a file, they paste the peer's public RSA key and either drag a file or select one manually. This triggers the `handleFileUpload()` function in `main.js`, which sends a `POST` request to `/api/files/upload` handled by `FileController`. The controller delegates to `FileService`, which performs all encryption steps: it generates a unique AES-256 key to encrypt the file content, then encrypts that AES key using the peer's RSA-4096 public key. It stores both the encrypted file (`filename.data`) and the encrypted key (`filename.key`) in the `storage/` directory. After uploading, `FileService` broadcasts a message (`__REFRESH_FILES__`) using `ClipboardService` via WebSocket to all connected clients. When the frontend receives this refresh signal, it calls `/api/files/list` to update the list of available files. If a user wants to download a file, `main.js` sends a `GET` request to `/api/files/download/{filename}`, which triggers the decryption process in `FileService`. The server reads both encrypted file and AES key, decrypts the key using its own private RSA key, and decrypts the file using that AES key, then returns the file for download.

For clipboard syncing, each browser establishes a persistent WebSocket connection to `/clipboard`, managed by `ClipboardController` and `ClipboardService`. When a user sends a message from the chat box, the frontend sends it over WebSocket. The server receives it and rebroadcasts it to all other connected devices in real-time, where it's shown inside the chat log area. All public/private keys are handled using `RSAUtil`, and the AES operations are performed using `EncryptionUtil`. The private key is stored locally under `config/private.key`, and the peer’s public key is temporarily saved in `config/peer_public.key`. This system ensures that all file transfers are end-to-end encrypted using hybrid cryptography (RSA + AES), real-time updates happen via WebSocket, and all traffic is restricted to the local network for privacy and speed. In the future, the `mDNSUtil` utility can be used to discover peers automatically, just like AirDrop.

---

## 🛡️ Security: Hybrid Encryption

| Part                     | Algorithm        | File          |
|--------------------------|------------------|---------------|
| 🔐 AES Key Generation     | AES-256          | `EncryptionUtil.java`
| 🔒 RSA Key Exchange       | RSA-4096         | `RSAUtil.java`
| 🔐 File Encryption        | AES              | `FileService.java`
| 📁 Private Key Storage    | Serialized file  | `config/private.key`
| 📬 Peer Public Key        | Base64 string    | `config/peer_public.key`

> Files are never stored in plaintext. Keys are exchanged only using secure RSA.

---

## 🌍 LAN Setup

1. Run the project on **each device** (Windows/Linux/macOS)
2. Access it in browser: `http://<local-ip>:8080`
3. Copy/paste public key to your peer
4. Upload and download files with end-to-end encryption
5. Chat using clipboard sync 💬

---

## 🧪 Tech Stack

- **Backend**: Java 17, Spring Boot, WebSocket
- **Frontend**: HTML5, Bootstrap 5, JavaScript
- **Encryption**: Java Cryptography (AES + RSA)
- **Networking**: HTTP, WebSocket, mDNS (planned)

---

## 🛠️ Developer Setup

### 1. Prerequisites
- JDK 17+
- Maven

### 2. Run the App
```bash
chmod +x run.sh
./run.sh
```
Or on Windows:
```bat
run.bat
```

Access at: [http://localhost:8080](http://localhost:8080)

---

## 📦 Key Java Classes (Backend)

| File                         | Description                                               |
|------------------------------|-----------------------------------------------------------|
| `FileController.java`        | Handles upload, list, download                            |
| `FileService.java`           | Encrypts, saves, and decrypts files                       |
| `ClipboardService.java`      | Broadcasts messages to WebSocket peers                   |
| `EncryptionUtil.java`        | AES key generation and AES encryption                    |
| `RSAUtil.java`               | RSA keypair generation, encryption, decryption           |
| `WebSocketConfig.java`       | Configures `/clipboard` WebSocket endpoint               |

---

## 🧠 Developer Tips

- Start building core logic in `service/` and `util/`
- Test endpoints with Postman before doing frontend
- Use WebSocket for real-time peer sync (no polling needed)
- Customize styling via `style.css`
- You can plug in mDNS later using libraries like `JmDNS`

---

## ✨ Credits & Inspiration

This project was designed with simplicity, privacy, and cross-platform LAN use in mind — inspired by apps like AirDrop, Snapdrop, and local-first tools.

Built with ❤️ by [Dilan Nayak](https://github.com/your-profile)

---

## 📃 License
MIT License – free to use, learn, modify, and share.

---

Need help running it or want to contribute? Open an issue or drop a message. Let's build local-first tech together! 💻📡