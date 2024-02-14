/// <reference types="node" />
import * as stream from 'stream';
import { WebDriverCommand } from './webdriverCommands';
/**
 * Logs WebDriver commands, transforming the command into a user-friendly description.
 */
export declare class WebDriverLogger {
    logStream: stream.Writable;
    readonly logName: string;
    constructor();
    /**
     * Start logging to the specified directory. Will create a file named
     * 'webdriver_log_<process id>.txt'
     *
     * @param logDir The directory to create log files in.
     */
    setLogDir(logDir: string): void;
    /**
     * Logs a webdriver command to the log file.
     *
     * @param command The command to log.
     */
    logWebDriverCommand(command: WebDriverCommand): void;
    printCommand(command: WebDriverCommand): string;
    timestamp(): string;
}
