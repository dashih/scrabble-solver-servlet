'use strict';

const refreshPeriod = 500; // ms
var statusTask = undefined;
var operation = undefined;
var statusPending = false;

window.onload = async () => {
    document.getElementById('running').style.display = 'none';
    document.getElementById('currentlyRunningDiv').style.display = 'none';

    const response = await fetch('/api/getVersions', { method: 'POST' });
    if (response.ok) {
        const v = await response.json();
        document.getElementById('versions').innerText = 
            `${v.app}
            Spring Boot ${v.springBoot}
            ${v.tomcat}
            Java ${v.java}
            ${v.numCores} cores (using ${v.maxConcurrentOperations})`;
    } else {
        alert('Error getting app info: ' + response.status);
    }

    const responseCr = await fetch('/api/getCurrentlyRunning', { method: 'POST' });
    if (responseCr.ok) {
        const currentlyRunning = await responseCr.json();
        if (Object.keys(currentlyRunning.operations).length > 0) {
            // Blank option first, so user must select one to watch.
            const defOp = document.createElement('option');
            defOp.value = '';
            defOp.text = '';
            document.getElementById('currentlyRunning').appendChild(defOp);
            for (const key of Object.keys(currentlyRunning.operations)) {
                const op = document.createElement('option');
                op.value = key;
                op.text = currentlyRunning.operations[key];
                document.getElementById('currentlyRunning').appendChild(op);
            }

            document.getElementById('currentlyRunningDiv').style.display = 'block';
        }
    } else {
        alert('Error getting currently running operations: ' + response.status);
    }
};

function watchOperation() {
    document.getElementById('form').style.display = 'none';
    document.getElementById('running').style.display = 'block';
    document.getElementById('summary').innerText = 'Initializing';
    statusTask = setInterval(async () => {
        if (statusPending) {
            return;
        }

        const response = await fetch('/api/getProgress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(operation)
        });
        if (response.ok) {
            statusPending = true;
            const responseJson = await response.json();
            const progress = responseJson.progress;
            const solveParams = responseJson.params;

            document.getElementById('summary').innerText =
                `${solveParams.input} (${solveParams.input.length} chars)
                ${solveParams.parallelMode ? 'Parallel' : 'Sequential'} Mode
                Minimum: ${solveParams.minChars} chars
                Matching ${solveParams.regex}
                `;
            
            document.getElementById('solutions').innerText = '';
            if (progress['runStatus'] == 'Done') {
                clearInterval(statusTask);
                progress['percentDone'] = 100;
                operation = undefined;
                document.getElementById('cancelButton').style.display = 'none';
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
            if (response.status === 503) {
                document.getElementById('cancelButton').style.display = 'none';
                document.getElementById('summary').innerText = 'Cancellation pending...';
            } else if (response.status === 410) {
                clearInterval(statusTask);
                operation = undefined;
                statusPending = false;
                document.getElementById('cancelButton').style.display = 'none';
                document.getElementById('summary').innerText = 'Canceled!';
            } else {
                alert('Error from server getting progress: ' + response.status);
            }
        }
    }, refreshPeriod);
}

document.getElementById('solveButton').onclick = async () => {
    const solveParams = {
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
        operation = await solveResponse.json();
        watchOperation();
    } else {
        if (solveResponse.status === 400) {
            alert('No required TLS certificate was sent');
        } else {
            alert('Error from server starting solve operation: ' + solveResponse.status);
        }
    }
};

document.getElementById('cancelButton').onclick = async () => {
    if (operation === undefined) {
        alert('No ongoing operation.');
        return;
    }

    const response = await fetch('/api/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(operation)
    });
    if (response.ok) {
        document.getElementById('summary').innerText = 'Cancellation pending...';
    } else {
        alert('Error cancelling operation: ' + response.status);
    }
};

document.getElementById('currentlyRunning').onchange = () => {
    operation = {
        id: document.getElementById('currentlyRunning').value
    };

    watchOperation();
}
