export declare type paramKey = 'sessionId' | 'elementId' | 'name' | 'propertyName';
export declare enum CommandName {
    NewSession = 0,
    DeleteSession = 1,
    Status = 2,
    GetTimeouts = 3,
    SetTimeouts = 4,
    Go = 5,
    GetCurrentURL = 6,
    UNKNOWN = 7,
}
/**
 * An instance of a WebDriver command, containing the params and data for that request.
 *
 * @param commandName The enum identifying the command.
 * @param params Parameters for the command taken from the request's url.
 * @param data Optional data included with the command, taken from the body of the request.
 */
export declare class WebDriverCommand {
    commandName: CommandName;
    data: any;
    private params;
    constructor(commandName: CommandName, params?: any, data?: any);
    getParam(key: paramKey): string;
}
/**
 * Returns a new WebdriverCommand object for the resource at the given URL.
 */
export declare function parseWebDriverCommand(url: any, method: any, data: string): WebDriverCommand;
