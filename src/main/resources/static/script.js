const dropArea = document.getElementById('drop-area');
const fileInput = document.getElementById('fileInput');
const uploadProgress = document.getElementById('upload-progress');

function createProgressBar(file) {
    const progressItem = document.createElement('div');
    progressItem.className = 'progress-item';
    progressItem.innerHTML = `
        <div class="progress-bar">
            <div class="progress"></div>
        </div>
        <div class="file-name">${file.name}</div>
        <div class="status">准备上传...</div>
    `;
    uploadProgress.appendChild(progressItem);
    return progressItem;
}

function uploadFile(file) {
    const progressItem = createProgressBar(file);
    const progressBar = progressItem.querySelector('.progress');
    const status = progressItem.querySelector('.status');

    const xhr = new XMLHttpRequest();
    const formData = new FormData();

    formData.append('file', file);

    xhr.open('POST', '/upload', true);

    xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
            const percentComplete = (e.loaded / e.total) * 100;
            progressBar.style.width = percentComplete + '%';
            status.textContent = Math.round(percentComplete) + '%';
        }
    };

    xhr.onload = () => {
        if (xhr.status === 200) {
            status.textContent = '上传成功';
        } else {
            status.textContent = '上传失败: ' + xhr.statusText;
        }
    };

    xhr.onerror = () => {
        status.textContent = '上传出错，请重试';
    };

    xhr.send(formData);
}

function handleFiles(files) {
    for (let i = 0; i < files.length; i++) {
        uploadFile(files[i]);
    }
}

dropArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropArea.style.borderColor = '#007bff';
});

dropArea.addEventListener('dragleave', () => {
    dropArea.style.borderColor = '#ccc';
});

dropArea.addEventListener('drop', (e) => {
    e.preventDefault();
    dropArea.style.borderColor = '#ccc';
    if (e.dataTransfer.files.length) {
        fileInput.files = e.dataTransfer.files;
        console.log('File(s) dropped: ', e.dataTransfer.files);
        // 在这里处理文件上传逻辑
        handleFiles(e.dataTransfer.files)
    }
});

fileInput.addEventListener('change', () => {
    if (fileInput.files.length) {
        console.log('File(s) selected:', fileInput.files);
        // 在这里处理文件上传逻辑
        handleFiles(fileInput.files);
    }
});