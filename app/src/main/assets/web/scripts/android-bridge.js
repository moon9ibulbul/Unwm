(function () {
    const sampleListEl = document.getElementById('sample-list');
    const searchInput = document.getElementById('sample-search');
    const autoToggle = document.getElementById('auto-detect-toggle');
    const importBtn = document.getElementById('import-sample');
    const refreshBtn = document.getElementById('refresh-samples');
    const statusBar = document.createElement('div');
    statusBar.className = 'status-bar';
    sampleListEl.parentElement.appendChild(statusBar);

    const state = {
        builtIn: [],
        userSamples: [],
        activeId: null,
        isDetecting: false
    };

    function toListItem(sample) {
        const item = document.createElement('button');
        item.type = 'button';
        item.className = 'sample-item';
        item.dataset.id = sample.id;
        item.textContent = sample.name;
        item.addEventListener('click', () => selectSample(sample));
        if (sample.id === state.activeId) {
            item.classList.add('active');
        }
        return item;
    }

    function renderSamples() {
        const query = (searchInput.value || '').trim().toLowerCase();
        sampleListEl.innerHTML = '';
        const samples = [...state.builtIn, ...state.userSamples];
        const filtered = samples.filter((sample) => {
            if (!query) return true;
            return sample.name.toLowerCase().includes(query);
        });
        if (!filtered.length) {
            const empty = document.createElement('p');
            empty.className = 'sample-empty';
            empty.textContent = 'Sample tidak ditemukan.';
            sampleListEl.appendChild(empty);
        } else {
            filtered.forEach((sample) => sampleListEl.appendChild(toListItem(sample)));
        }
    }

    async function fetchBuiltInSamples() {
        try {
            const response = await fetch('watermark-samples.json');
            const raw = await response.json();
            state.builtIn = raw.map((item) => ({
                id: `asset:${item.file}`,
                name: item.name.replace(/\s+/g, ' ').trim(),
                file: item.file,
                type: 'asset'
            }));
        } catch (error) {
            console.error('Failed to load sample list', error);
        }
    }

    function loadUserSamples() {
        if (window.AstralSamples && typeof window.AstralSamples.getUserSamples === 'function') {
            try {
                const raw = window.AstralSamples.getUserSamples();
                const parsed = raw ? JSON.parse(raw) : [];
                state.userSamples = parsed.map((item) => ({
                    id: item.id,
                    name: item.name,
                    type: 'user'
                }));
            } catch (error) {
                console.warn('Cannot parse user samples', error);
                state.userSamples = [];
            }
        }
    }

    function updateStatus(message, intent) {
        statusBar.textContent = message || '';
        statusBar.dataset.intent = intent || '';
    }

    async function selectSample(sample) {
        state.activeId = sample.id;
        renderSamples();
        try {
            updateStatus('Memuat watermark…', 'info');
            if (sample.type === 'asset') {
                const url = `watermark-samples/${encodeURIComponent(sample.file)}`;
                const blob = await fetch(url).then((res) => res.blob());
                const dataUrl = await blobToDataUrl(blob);
                window.loadWatermarkFromDataUrl(dataUrl, sample.name);
            } else if (window.AstralSamples && typeof window.AstralSamples.loadUserSample === 'function') {
                const dataUrl = window.AstralSamples.loadUserSample(sample.id);
                if (dataUrl) {
                    window.loadWatermarkFromDataUrl(dataUrl, sample.name);
                } else {
                    updateStatus('Sample tidak ditemukan di penyimpanan.', 'error');
                }
            }
        } catch (error) {
            console.error('Failed to load sample', error);
            updateStatus('Gagal memuat watermark.', 'error');
        }
    }

    function blobToDataUrl(blob) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = (err) => reject(err);
            reader.readAsDataURL(blob);
        });
    }

    function handleAutoDetectStart() {
        state.isDetecting = true;
        updateStatus('Mendeteksi posisi watermark…', 'info');
    }

    function handleAutoDetectEnd(event) {
        state.isDetecting = false;
        const detail = event.detail || {};
        if (detail.success) {
            updateStatus(`Watermark terdeteksi di (${detail.x.toFixed(0)}, ${detail.y.toFixed(0)})`, 'success');
        } else if (detail && detail.message) {
            updateStatus(detail.message, 'warning');
        } else {
            updateStatus('Deteksi otomatis tidak berhasil.', 'warning');
        }
    }

    function handlePreview(event) {
        const detail = event.detail || {};
        if (detail.after) {
            updateStatus(`Preview siap: ${detail.name || 'hasil terbaru'}`, 'success');
        }
    }

    function bindEvents() {
        searchInput.addEventListener('input', renderSamples);
        refreshBtn.addEventListener('click', () => {
            loadUserSamples();
            renderSamples();
            updateStatus('Daftar sample diperbarui.', 'info');
        });
        importBtn.addEventListener('click', () => {
            if (window.AstralSamples && typeof window.AstralSamples.requestImportSample === 'function') {
                window.AstralSamples.requestImportSample();
            } else {
                updateStatus('Import tidak tersedia.', 'warning');
            }
        });
        autoToggle.addEventListener('change', () => {
            if (autoToggle.checked && typeof window.autoDetectWatermarkPosition === 'function') {
                try {
                    window.autoDetectWatermarkPosition();
                } catch (error) {
                    console.warn('Auto detect toggle error', error);
                }
            }
        });
        document.addEventListener('astral:autodetect-start', handleAutoDetectStart);
        document.addEventListener('astral:autodetect-end', handleAutoDetectEnd);
        document.addEventListener('astral:preview-ready', handlePreview);
        document.addEventListener('astral:watermark-ready', () => {
            if (!autoToggle.checked) {
                updateStatus('Watermark siap. Jalankan deteksi manual jika diperlukan.', 'info');
            }
        });
        document.addEventListener('astral:image-loaded', (event) => {
            const detail = event.detail || {};
            if (detail.name) {
                updateStatus(`Gambar dimuat: ${detail.name}`, 'info');
            }
        });
    }

    function installBridge() {
        window.AstralSamplesBridge = {
            onSampleImported: function (json) {
                try {
                    const sample = JSON.parse(json);
                    state.userSamples.push({
                        id: sample.id,
                        name: sample.name,
                        type: 'user'
                    });
                    renderSamples();
                    updateStatus('Sample baru ditambahkan.', 'success');
                } catch (error) {
                    console.error('Invalid sample payload', error);
                }
            }
        };
    }

    async function init() {
        bindEvents();
        installBridge();
        await fetchBuiltInSamples();
        loadUserSamples();
        renderSamples();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
