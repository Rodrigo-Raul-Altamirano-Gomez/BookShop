const avisoConexion = document.getElementById("components-reconnect-modal");

avisoConexion.addEventListener("components-reconnect-state-changed", event => {
    if (event.detail.state === "show") {
        avisoConexion.showModal();
    } else if (event.detail.state === "hide") {
        avisoConexion.close();
    }
});
