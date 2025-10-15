const url = new URL(window.location.href);
window.scouter = window.scouter || {};
window.scouter.server_addr = url.searchParams.get("server") || "http://210.1.1.113:6180";


