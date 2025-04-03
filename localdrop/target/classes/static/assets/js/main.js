// assets/js/main.js

document.addEventListener("DOMContentLoaded", () => {
    const fileInput = document.getElementById("fileInput");
    const dropZone = document.getElementById("drop-zone");
    const peerKey = document.getElementById("peerKey");
    const uploadStatus = document.getElementById("uploadStatus");
    const fileList = document.getElementById("fileList");
    const myPublicKey = document.getElementById("myPublicKey");
    const clipboard = document.getElementById("clipboard");
    const sendBtn = document.getElementById("sendBtn");
    const chatLog = document.getElementById("chatLog");

    let socket = null;

    // WebSocket setup
    function connectWebSocket() {
        socket = new WebSocket(`ws://${location.host}/clipboard`);

        socket.onopen = () => {
            console.log("🟢 WebSocket connected");
        };

        socket.onmessage = (event) => {
            const msg = event.data;

            if (msg === "__REFRESH_FILES__") {
                console.log("🔄 Refreshing file list due to peer upload");
                loadFiles();
                return;
            }

            const div = document.createElement("div");
            div.className = "text-start text-dark mb-1";
            div.textContent = msg;
            chatLog.appendChild(div);
            chatLog.scrollTop = chatLog.scrollHeight;
        };


        socket.onerror = (err) => {
            console.error("WebSocket error", err);
        };

        socket.onclose = () => {
            console.warn("🔌 WebSocket closed, retrying in 2s...");
            setTimeout(connectWebSocket, 2000);
        };
    }

    // File upload handler
    function handleFileUpload(file) {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("targetPublicKey", peerKey.value.trim());

        fetch("/api/files/upload", {
            method: "POST",
            body: formData,
        })
            .then((res) => res.text())
            .then((msg) => {
                uploadStatus.textContent = msg;
                loadFiles();
            })
            .catch((err) => {
                uploadStatus.textContent = "❌ Upload failed.";
                console.error(err);
            });
    }

    // Load available files
    function loadFiles() {
        fetch("/api/files/list")
            .then((res) => res.json())
            .then((files) => {
                fileList.innerHTML = "";
                files.forEach((f) => {
                    const li = document.createElement("li");
                    li.className = "list-group-item d-flex justify-content-between align-items-center";
                    li.innerHTML = `<span>${f.name} (${(f.size / 1024).toFixed(1)} KB)</span>
                          <a class="btn btn-sm btn-outline-primary" href="/api/files/download/${f.name}">Download</a>`;
                    fileList.appendChild(li);
                });
            });
    }

    // Load public key
    function loadPublicKey() {
        fetch("/api/keys/public")
            .then((res) => res.text())
            .then((key) => {
                myPublicKey.textContent = key;
            });
    }

    // Clipboard send
    sendBtn.addEventListener("click", () => {
        const content = clipboard.value.trim();
        if (socket && socket.readyState === WebSocket.OPEN && content) {
            socket.send(content);
            const div = document.createElement("div");
            div.className = "text-end text-primary mb-1";
            div.textContent = content;
            chatLog.appendChild(div);
            chatLog.scrollTop = chatLog.scrollHeight;
            clipboard.value = "";
        }
    });

    // Drag and drop file handling
    dropZone.addEventListener("click", () => fileInput.click());
    dropZone.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropZone.classList.add("bg-light");
    });
    dropZone.addEventListener("dragleave", () => dropZone.classList.remove("bg-light"));
    dropZone.addEventListener("drop", (e) => {
        e.preventDefault();
        dropZone.classList.remove("bg-light");
        if (e.dataTransfer.files.length) {
            handleFileUpload(e.dataTransfer.files[0]);
        }
    });
    fileInput.addEventListener("change", () => {
        if (fileInput.files.length) {
            handleFileUpload(fileInput.files[0]);
        }
    });

    // Initialize
    loadFiles();
    loadPublicKey();
    connectWebSocket();
});

