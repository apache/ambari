"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/*
 * This is an implementation of the Browserstack Driver Provider.
 * It is responsible for setting up the account object, tearing
 * it down, and setting up the driver correctly.
 */
const https = require("https");
const q = require("q");
const util = require("util");
const exitCodes_1 = require("../exitCodes");
const logger_1 = require("../logger");
const driverProvider_1 = require("./driverProvider");
let logger = new logger_1.Logger('browserstack');
class BrowserStack extends driverProvider_1.DriverProvider {
    constructor(config) {
        super(config);
    }
    /**
     * Hook to update the BrowserStack job status.
     * @public
     * @param {Object} update
     * @return {q.promise} A promise that will resolve when the update is complete.
     */
    updateJob(update) {
        let deferredArray = this.drivers_.map((driver) => {
            let deferred = q.defer();
            driver.getSession().then((session) => {
                let headers = {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic ' +
                        new Buffer(this.config_.browserstackUser + ':' + this.config_.browserstackKey)
                            .toString('base64')
                };
                let options = {
                    hostname: 'www.browserstack.com',
                    port: 443,
                    path: '/automate/sessions/' + session.getId() + '.json',
                    method: 'GET',
                    headers: headers
                };
                let req = https.request(options, (res) => {
                    res.on('data', (data) => {
                        let info = JSON.parse(data.toString());
                        if (info && info.automation_session && info.automation_session.browser_url) {
                            logger.info('BrowserStack results available at ' + info.automation_session.browser_url);
                        }
                        else {
                            logger.info('BrowserStack results available at ' +
                                'https://www.browserstack.com/automate');
                        }
                    });
                });
                req.end();
                req.on('error', (e) => {
                    logger.info('BrowserStack results available at ' +
                        'https://www.browserstack.com/automate');
                });
                let jobStatus = update.passed ? 'completed' : 'error';
                options.method = 'PUT';
                let update_req = https.request(options, (res) => {
                    let responseStr = '';
                    res.on('data', (data) => {
                        responseStr += data.toString();
                    });
                    res.on('end', () => {
                        logger.info(responseStr);
                        deferred.resolve();
                    });
                    res.on('error', (e) => {
                        throw new exitCodes_1.BrowserError(logger, 'Error updating BrowserStack pass/fail status: ' + util.inspect(e));
                    });
                });
                update_req.write('{"status":"' + jobStatus + '"}');
                update_req.end();
            });
            return deferred.promise;
        });
        return q.all(deferredArray);
    }
    /**
     * Configure and launch (if applicable) the object's environment.
     * @return {q.promise} A promise which will resolve when the environment is
     *     ready to test.
     */
    setupDriverEnv() {
        let deferred = q.defer();
        this.config_.capabilities['browserstack.user'] = this.config_.browserstackUser;
        this.config_.capabilities['browserstack.key'] = this.config_.browserstackKey;
        this.config_.seleniumAddress = 'http://hub.browserstack.com/wd/hub';
        // Append filename to capabilities.name so that it's easier to identify
        // tests.
        if (this.config_.capabilities.name && this.config_.capabilities.shardTestFiles) {
            this.config_.capabilities.name +=
                (':' + this.config_.specs.toString().replace(/^.*[\\\/]/, ''));
        }
        logger.info('Using BrowserStack selenium server at ' + this.config_.seleniumAddress);
        deferred.resolve();
        return deferred.promise;
    }
}
exports.BrowserStack = BrowserStack;
//# sourceMappingURL=browserStack.js.map