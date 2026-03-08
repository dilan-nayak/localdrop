document.addEventListener("DOMContentLoaded", () => {
    const WS_TYPE = {
        CHAT_MESSAGE: "chat_message",
        DELIVERY_ACK: "delivery_ack",
        READ_ACK: "read_ack",
        SYNC_REQUEST: "sync_request",
        SYNC_RESPONSE: "sync_response",
        PRESENCE_UPDATE: "presence_update",
        SYSTEM_REFRESH_FILES: "system_refresh_files",
        ERROR: "error",
    };

    const fileInput = document.getElementById("fileInput");
    const dropZone = document.getElementById("drop-zone");
    const peerKey = document.getElementById("peerKey");
    const uploadStatus = document.getElementById("uploadStatus");
    const fileList = document.getElementById("fileList");
    const myPublicKey = document.getElementById("myPublicKey");
    const clipboard = document.getElementById("clipboard");
    const sendBtn = document.getElementById("sendBtn");
    const chatLog = document.getElementById("chatLog");
    const refreshBtn = document.getElementById("refreshBtn");
    const copyKeyBtn = document.getElementById("copyKeyBtn");
    const useMyKeyBtn = document.getElementById("useMyKeyBtn");
    const clearChatBtn = document.getElementById("clearChatBtn");
    const fileItemTemplate = document.getElementById("fileItemTemplate");
    const chatMessageTemplate = document.getElementById("chatMessageTemplate");
    const nicknameInput = document.getElementById("nicknameInput");
    const saveNicknameBtn = document.getElementById("saveNicknameBtn");
    const presenceChip = document.getElementById("presenceChip");

    const messageNodes = new Map();
    let socket = null;
    let myKeyCache = "";
    let identity = initIdentity();
    let lastSyncTimestamp = Number(localStorage.getItem("localdrop.lastSyncTimestamp") || "0");

    nicknameInput.value = identity.senderName;

    function initIdentity() {
        let senderId = localStorage.getItem("localdrop.senderId");
        if (!senderId) {
            senderId = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : `client-${Date.now()}`;
            localStorage.setItem("localdrop.senderId", senderId);
        }
        let senderName = localStorage.getItem("localdrop.senderName");
        if (!senderName) {
            senderName = `User-${senderId.slice(-4)}`;
            localStorage.setItem("localdrop.senderName", senderName);
        }
        return { senderId, senderName };
    }

    function humanTime(timestamp) {
        return new Date(timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }

    function statusTicks(status) {
        if (status === "read") {
            return "✓✓";
        }
        if (status === "delivered") {
            return "✓✓";
        }
        return "✓";
    }

    function statusClass(status) {
        if (status === "read") {
            return "status-read";
        }
        if (status === "delivered") {
            return "status-delivered";
        }
        return "status-sent";
    }

    function setStatus(message, isError = false) {
        uploadStatus.textContent = message;
        uploadStatus.style.color = isError ? "#b91c1c" : "";
    }

    function updateLastSync(timestamp) {
        if (!timestamp || Number.isNaN(timestamp)) {
            return;
        }
        if (timestamp > lastSyncTimestamp) {
            lastSyncTimestamp = timestamp;
            localStorage.setItem("localdrop.lastSyncTimestamp", String(lastSyncTimestamp));
        }
    }

    function renderMessage(message) {
        const isSelf = message.senderId === identity.senderId;
        if (messageNodes.has(message.messageId)) {
            updateMessageStatus(message.messageId, message.status || "", message.timestamp || Date.now());
            return;
        }

        const node = chatMessageTemplate.content.firstElementChild.cloneNode(true);
        node.dataset.messageId = message.messageId;
        node.dataset.self = isSelf ? "true" : "false";
        if (isSelf) {
            node.classList.add("self");
        }

        const content = isSelf ? message.content : `${message.senderName || "Peer"}: ${message.content}`;
        node.querySelector(".chat-content").textContent = content;
        node.querySelector(".chat-time").textContent = humanTime(message.timestamp || Date.now());
        const statusNode = node.querySelector(".chat-status");
        if (isSelf) {
            const currentStatus = message.status || "sent";
            statusNode.textContent = statusTicks(currentStatus);
            statusNode.className = `chat-status ${statusClass(currentStatus)}`;
        } else {
            statusNode.textContent = "";
        }

        chatLog.appendChild(node);
        chatLog.scrollTop = chatLog.scrollHeight;
        messageNodes.set(message.messageId, node);
        updateLastSync(message.timestamp);
    }

    function ackRenderedMessage(message) {
        if (!message || !message.messageId || message.senderId === identity.senderId) {
            return;
        }
        ackMessage(WS_TYPE.DELIVERY_ACK, message.messageId);
        if (document.visibilityState === "visible") {
            ackMessage(WS_TYPE.READ_ACK, message.messageId);
        }
    }

    function ackVisibleRemoteMessages() {
        if (document.visibilityState !== "visible") {
            return;
        }
        for (const [messageId, node] of messageNodes.entries()) {
            if (node.dataset.self === "false") {
                ackMessage(WS_TYPE.READ_ACK, messageId);
            }
        }
    }

    function updateMessageStatus(messageId, status, timestamp) {
        const node = messageNodes.get(messageId);
        if (!node) {
            return;
        }
        const statusNode = node.querySelector(".chat-status");
        if (statusNode && status) {
            statusNode.textContent = statusTicks(status);
            statusNode.className = `chat-status ${statusClass(status)}`;
        }
        if (timestamp) {
            node.querySelector(".chat-time").textContent = humanTime(timestamp);
            updateLastSync(timestamp);
        }
    }

    function renderFileItem(file) {
        const node = fileItemTemplate.content.firstElementChild.cloneNode(true);
        node.querySelector(".file-name").textContent = file.name;
        node.querySelector(".file-size").textContent = `${(file.size / 1024).toFixed(1)} KB`;
        const link = node.querySelector(".download-btn");
        link.href = `/api/files/download/${encodeURIComponent(file.name)}`;
        fileList.appendChild(node);
    }

    function sendWsEvent(event) {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            return false;
        }
        socket.send(JSON.stringify(event));
        return true;
    }

    function requestSync() {
        sendWsEvent({
            type: WS_TYPE.SYNC_REQUEST,
            senderId: identity.senderId,
            senderName: identity.senderName,
            lastSyncTimestamp,
            timestamp: Date.now(),
        });
    }

    function ackMessage(type, messageId) {
        sendWsEvent({
            type,
            messageId,
            senderId: identity.senderId,
            senderName: identity.senderName,
            timestamp: Date.now(),
        });
    }

    function handleIncomingChat(message, fromSync = false) {
        renderMessage(message);
        ackRenderedMessage(message);
        if (!fromSync) {
            chatLog.scrollTop = chatLog.scrollHeight;
        }
    }

    function parseIncoming(payload) {
        try {
            return JSON.parse(payload);
        } catch {
            return null;
        }
    }

    function connectWebSocket() {
        socket = new WebSocket(`ws://${location.host}/clipboard`);

        socket.onopen = () => {
            requestSync();
        };

        socket.onmessage = (event) => {
            const incoming = parseIncoming(event.data);
            if (!incoming || !incoming.type) {
                return;
            }

            switch (incoming.type) {
                case WS_TYPE.SYSTEM_REFRESH_FILES:
                    loadFiles();
                    return;
                case WS_TYPE.CHAT_MESSAGE:
                    handleIncomingChat(incoming);
                    return;
                case WS_TYPE.SYNC_RESPONSE:
                    (incoming.messages || []).forEach((msg) => handleIncomingChat(msg, true));
                    return;
                case WS_TYPE.DELIVERY_ACK:
                case WS_TYPE.READ_ACK:
                    updateMessageStatus(incoming.messageId, incoming.status, incoming.timestamp);
                    return;
                case WS_TYPE.PRESENCE_UPDATE:
                    presenceChip.textContent = `Live · ${incoming.onlineCount || 0} online`;
                    return;
                case WS_TYPE.ERROR:
                    console.warn(incoming.error || "WebSocket error");
                    return;
                default:
                    return;
            }
        };

        socket.onclose = () => {
            setTimeout(connectWebSocket, 2000);
        };
    }

    function handleFileUpload(file) {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("targetPublicKey", peerKey.value.trim());
        setStatus("Uploading...");

        fetch("/api/files/upload", {
            method: "POST",
            body: formData,
        })
            .then((res) => res.text())
            .then((msg) => {
                setStatus(msg, msg.toLowerCase().includes("failed"));
                loadFiles();
            })
            .catch(() => {
                setStatus("Upload failed.", true);
            });
    }

    function loadFiles() {
        fetch("/api/files/list")
            .then((res) => res.json())
            .then((files) => {
                fileList.innerHTML = "";
                files.forEach(renderFileItem);
            })
            .catch(() => {
                setStatus("Could not load files.", true);
            });
    }

    function loadPublicKey() {
        fetch("/api/keys/public")
            .then((res) => res.text())
            .then((key) => {
                myKeyCache = key;
                myPublicKey.textContent = key;
                if (!peerKey.value.trim()) {
                    peerKey.value = key;
                }
            })
            .catch(() => {
                myPublicKey.textContent = "Unable to load public key.";
            });
    }

    function loadChatHistory() {
        fetch("/api/chat/history")
            .then((res) => res.json())
            .then((messages) => {
                chatLog.innerHTML = "";
                messageNodes.clear();
                messages.forEach((message) => {
                    renderMessage(message);
                    ackRenderedMessage(message);
                });
            });
    }

    function sendMessage() {
        const content = clipboard.value.trim();
        if (!content) {
            return;
        }

        const messageId = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : `msg-${Date.now()}`;
        const optimisticMessage = {
            type: WS_TYPE.CHAT_MESSAGE,
            messageId,
            senderId: identity.senderId,
            senderName: identity.senderName,
            content,
            timestamp: Date.now(),
            status: "pending",
        };
        renderMessage(optimisticMessage);

        const sent = sendWsEvent({
            type: WS_TYPE.CHAT_MESSAGE,
            messageId,
            senderId: identity.senderId,
            senderName: identity.senderName,
            content,
            timestamp: Date.now(),
        });
        if (!sent) {
            updateMessageStatus(messageId, "pending", Date.now());
            setStatus("Socket disconnected. Message will sync after reconnect.", true);
        }
        clipboard.value = "";
    }

    sendBtn.addEventListener("click", sendMessage);

    clipboard.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") {
            requestSync();
            ackVisibleRemoteMessages();
        }
    });

    saveNicknameBtn.addEventListener("click", () => {
        const newName = nicknameInput.value.trim();
        if (!newName) {
            return;
        }
        identity.senderName = newName.slice(0, 24);
        localStorage.setItem("localdrop.senderName", identity.senderName);
        requestSync();
    });

    dropZone.addEventListener("click", () => fileInput.click());
    dropZone.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropZone.classList.add("active");
    });
    dropZone.addEventListener("dragleave", () => dropZone.classList.remove("active"));
    dropZone.addEventListener("drop", (e) => {
        e.preventDefault();
        dropZone.classList.remove("active");
        if (e.dataTransfer.files.length) {
            handleFileUpload(e.dataTransfer.files[0]);
        }
    });
    fileInput.addEventListener("change", () => {
        if (fileInput.files.length) {
            handleFileUpload(fileInput.files[0]);
        }
    });

    refreshBtn.addEventListener("click", () => {
        loadFiles();
        requestSync();
    });

    useMyKeyBtn.addEventListener("click", () => {
        if (myKeyCache) {
            peerKey.value = myKeyCache;
        }
    });

    copyKeyBtn.addEventListener("click", async () => {
        if (!myKeyCache) {
            return;
        }
        await navigator.clipboard.writeText(myKeyCache);
        setStatus("Public key copied.");
    });

    clearChatBtn.addEventListener("click", () => {
        chatLog.innerHTML = "";
        messageNodes.clear();
    });

    loadFiles();
    loadPublicKey();
    loadChatHistory();
    connectWebSocket();
});
