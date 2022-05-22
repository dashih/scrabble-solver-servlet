'use strict';

const refreshPeriod = 500; // ms
var statusTask = undefined;
var operationId = undefined;
var statusPending = false;

async function sha256(password) {
    const passwordEncoded = new TextEncoder().encode(password);
    const hashBuffer =  await crypto.subtle.digest('SHA-256', passwordEncoded);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    return hashHex;
}

window.onload = () => {
    document.getElementById('running').style.display = 'none';
};

document.getElementById('solveButton').onclick = async () => {
    const solveParams = {
        passwordHash: await sha256(document.getElementById('password').value),
        parallelMode: document.getElementById('mode').value === 'true',
        input: document.getElementById('input').value,
        regex: document.getElementById('regex').value,
        minChars: document.getElementById('minChars').value
    };
    if (solveParams.input === '') {
        alert('Must provide input!');
        return;
    }

    if (solveParams.regex == '') {
        solveParams.regex = '[A-Z]+';
    }

    const solveResponse = await fetch('/api/solve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(solveParams)
    });
    if (solveResponse.ok) {
        let idJson = await solveResponse.json();
        operationId = idJson['id'];
        document.getElementById('summary').innerText =
            `${solveParams.input}
            ${solveParams.parallelMode ? 'Parallel' : 'Sequential'} Mode
            ${solveParams.minChars}-chars or greater
            Matching ${solveParams.regex}
            `;
        document.getElementById('form').style.display = 'none';
        document.getElementById('running').style.display = 'block';
        statusTask = setInterval(async () => {
            if (statusPending) {
                return;
            }

            const response = await fetch('/api/getProgress?' + new URLSearchParams({
                id: operationId
            }));
            if (response.ok) {
                statusPending = true;
                let progress = await response.json();
                document.getElementById('solutions').innerText = '';
                if (progress['runStatus'] == 'Done') {
                    clearInterval(statusTask);
                    progress['percentDone'] = 100;
                    operationId = undefined;
                    document.getElementById('solutions').innerText =
                        `Found ${progress['solutions'].length} solutions!

                    `;
                }

                for (let i = 0; i < progress['solutions'].length; i++) {
                    document.getElementById('solutions').innerText += progress['solutions'][i] + '\n';
                }

                let elapsedPretty = new Date(progress['elapsed']).toISOString().substring(11, 19);
                document.getElementById('progressBar').value = progress['percentDone'];
                document.getElementById('progressText').innerText =
                    `${progress['percentDone'].toFixed(1)}% of ${progress['total'].toLocaleString()}
                    ${elapsedPretty}
                    `;
                statusPending = false;
            } else {
                alert('Error from server getting progress: ' + response.status);
            }
        }, refreshPeriod);
    } else {
        alert('Error from server starting solve operation: ' + solveResponse.status);
    }
};
