const fs = require('fs');
const { execSync } = require('child_process');

const adbPath = '"C:\\Users\\ASUS\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe"';

try {
    console.log("Fetching logcat logs...");
    // Clear logcat buffer first to capture fresh logs if needed, but let's just dump current buffer
    const buffer = execSync(`${adbPath} logcat -d -t 10000`);
    const logcat = buffer.toString('utf8');
    fs.writeFileSync('logcat_full.txt', logcat);
    console.log("Logs written to logcat_full.txt");

    const lines = logcat.split('\n');
    let found = false;

    console.log("Scanning logs for 'com.unifurniture.mobile'...");
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (line.includes("com.unifurniture.mobile") && (line.includes("Fatal") || line.includes("Exception") || line.includes("Error") || line.includes("FATAL") || line.includes("NullPointerException") || line.includes("Runtime"))) {
            console.log("\n==================================================");
            console.log(`Potential Crash Log at line ${i + 1}:`);
            const start = Math.max(0, i - 5);
            const end = Math.min(lines.length, i + 35);
            for (let j = start; j < end; j++) {
                const prefix = j === i ? ">>> " : "    ";
                console.log(prefix + lines[j]);
            }
            console.log("==================================================\n");
            found = true;
            i += 35; // Skip print of same traceback
        }
    }

    if (!found) {
        console.log("No specific exception containing 'com.unifurniture.mobile' found in the logs.");
        console.log("Printing last 50 lines of logcat error level logs as fallback:");
        const errBuffer = execSync(`${adbPath} logcat -d -t 50 *:E`);
        console.log(errBuffer.toString('utf8'));
    }
} catch (err) {
    console.error("Error running logcat scan:", err);
}
