"use strict";
const fs = require("fs");
const path = require("path");
const webdriverCommands_1 = require("./webdriverCommands");
// Generate a random 8 character ID to avoid collisions.
function getLogId() {
    return Math.floor(Math.random() * Number.MAX_SAFE_INTEGER).toString(36).slice(0, 8);
}
/**
 * Logs WebDriver commands, transforming the command into a user-friendly description.
 */
class WebDriverLogger {
    constructor() {
        this.logName = `webdriver_log_${getLogId()}.txt`;
    }
    /**
     * Start logging to the specified directory. Will create a file named
     * 'webdriver_log_<process id>.txt'
     *
     * @param logDir The directory to create log files in.
     */
    setLogDir(logDir) {
        this.logStream = fs.createWriteStream(path.join(logDir, this.logName), { flags: 'a' });
    }
    /**
     * Logs a webdriver command to the log file.
     *
     * @param command The command to log.
     */
    logWebDriverCommand(command) {
        if (!this.logStream) {
            return;
        }
        let cmdLog = this.printCommand(command);
        let logLine;
        if (command.getParam('sessionId')) {
            let session = command.getParam('sessionId').slice(0, 6);
            logLine = `${this.timestamp()} [${session}] ${cmdLog}\n`;
        }
        else {
            logLine = `${this.timestamp()} ${cmdLog}\n`;
        }
        this.logStream.write(logLine);
    }
    printCommand(command) {
        switch (command.commandName) {
            case webdriverCommands_1.CommandName.NewSession:
                let desired = command.data['desiredCapabilities'];
                return `Getting new "${desired['browserName']}" session`;
            case webdriverCommands_1.CommandName.DeleteSession:
                let sessionId = command.getParam('sessionId').slice(0, 6);
                return `Deleting session ${sessionId}`;
            case webdriverCommands_1.CommandName.Go:
                return `Navigating to ${command.data['url']}`;
            case webdriverCommands_1.CommandName.GetCurrentURL:
                return `Getting current URL`;
            default:
                return `Unknown command ${command.data['url']}`;
        }
    }
    timestamp() {
        let d = new Date();
        let hours = d.getHours() < 10 ? '0' + d.getHours() : d.getHours();
        let minutes = d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes();
        let seconds = d.getSeconds() < 10 ? '0' + d.getSeconds() : d.getSeconds();
        let millis = d.getMilliseconds().toString();
        millis = '000'.slice(0, 3 - millis.length) + millis;
        return `[${hours}:${minutes}:${seconds}.${millis}]`;
    }
}
exports.WebDriverLogger = WebDriverLogger;
//# sourceMappingURL=webdriverLogger.js.map