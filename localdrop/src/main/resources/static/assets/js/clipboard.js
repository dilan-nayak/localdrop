const textArea = document.getElementById("clipboard");
const sendBtn = document.getElementById("sendBtn");

let socket = null;

function connectWebSocket() {
    const url = `ws://${location.host}/clipboard`;
    socket = new WebSocket(url);

    socket.onopen = () => {
        console.log("🟢 WebSocket connected");
    };

    socket.onmessage = (event) => {
        console.log("📥 Received:", event.data);
        textArea.value = event.data;
    };

    socket.onerror = (err) => {
        console.error("WebSocket error", err);
    };

    socket.onclose = () => {
        console.warn("🔌 WebSocket closed, retrying in 2s...");
        setTimeout(connectWebSocket, 2000);
    };
}

sendBtn.addEventListener("click", () => {
    const content = textArea.value.trim();
    if (socket && socket.readyState === WebSocket.OPEN && content) {
        socket.send(content);
        console.log("📤 Sent to peers:", content);
    }
});

connectWebSocket();

sendBtn.addEventListener("click", () => {
    console.log("📤 Button clicked"); // ✅ Add this

    const content = textArea.value.trim();
    console.log("✏️ Content to send:", content); // ✅ Add this

    if (socket && socket.readyState === WebSocket.OPEN && content) {
        socket.send(content);
        console.log("📤 Sent to peers:", content);
    } else {
        console.warn("⚠️ WebSocket not open or content empty");
    }
});
