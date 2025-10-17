(function () {
    const status = document.createElement('div');
    status.className = 'extractor-status';
    document.body.appendChild(status);

    function showStatus(message, intent) {
        status.textContent = message || '';
        status.dataset.intent = intent || '';
    }

    window.AstralSamplesBridge = {
        onSampleImported: function () {},
        onSampleSaved: function (json) {
            try {
                const payload = typeof json === 'string' ? JSON.parse(json) : json;
                showStatus(`Sample "${payload.name}" tersimpan.`, 'success');
            } catch (error) {
                console.warn('Invalid sample payload', error);
            }
        }
    };

    window.addEventListener('astral:autodetect-start', function () {
        showStatus('Menganalisis...', 'info');
    });
    window.addEventListener('astral:autodetect-end', function (event) {
        const detail = event.detail || {};
        if (detail.success) {
            showStatus('Posisi watermark diperbarui.', 'success');
        } else if (detail.message) {
            showStatus(detail.message, 'warning');
        }
    });
})();
