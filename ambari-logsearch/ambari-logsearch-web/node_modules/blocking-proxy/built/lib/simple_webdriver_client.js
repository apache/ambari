"use strict";
const http = require('http');
const url = require('url');
/**
 * Super dumb and simple WebDriver client. Works with selenium standalone, may or may not work yet
 * directly with other drivers.
 */
class SimpleWebDriverClient {
    constructor(seleniumAddress) {
        this.seleniumAddress = seleniumAddress;
    }
    /**
     * Send an execute script command.
     *
     * @param sessionId
     * @param data A JSON blob with the script and arguments to execute.
     */
    execute(sessionId, data) {
        const url = ['session', sessionId, 'execute'].join('/');
        return this.createSeleniumRequest('POST', url, data);
    }
    /**
     * Send an execute async script command.
     *
     * @param sessionId
     * @param data A JSON blob with the script and arguments to execute.
     */
    executeAsync(sessionId, data) {
        const url = ['session', sessionId, 'execute_async'].join('/');
        return this.createSeleniumRequest('POST', url, data);
    }
    /**
     * Get the location of an element.
     *
     * @param sessionId
     * @param elementId
     * @returns Promise<{}> A promise that resolves with the x and y coordinates of the element.
     */
    getLocation(sessionId, elementId) {
        const url = ['session', sessionId, 'element', elementId, 'location'].join('/');
        return this.createSeleniumRequest('GET', url);
    }
    /**
     * Get the size of an element.
     *
     * @param sessionId
     * @param elementId
     * @returns Promise<{}> A promise that resolves with the height and width of the element.
     */
    getSize(sessionId, elementId) {
        const url = ['session', sessionId, 'element', elementId, 'size'].join('/');
        return this.createSeleniumRequest('GET', url);
    }
    createSeleniumRequest(method, messageUrl, data) {
        let parsedUrl = url.parse(this.seleniumAddress);
        let options = {};
        options['method'] = method;
        options['path'] = parsedUrl.path + '/' + messageUrl;
        options['hostname'] = parsedUrl.hostname;
        options['port'] = parseInt(parsedUrl.port);
        let request = http.request(options);
        return new Promise((resolve, reject) => {
            if (data) {
                request.write(data);
            }
            request.end();
            request.on('response', (resp) => {
                let respData = '';
                resp.on('data', (d) => {
                    respData += d;
                });
                resp.on('error', (err) => {
                    reject(err);
                });
                resp.on('end', () => {
                    let response = JSON.parse(respData);
                    if (response.state !== 'success') {
                        reject(response.value);
                    }
                    resolve(response.value);
                });
            });
        });
    }
    ;
}
exports.SimpleWebDriverClient = SimpleWebDriverClient;
//# sourceMappingURL=simple_webdriver_client.js.map