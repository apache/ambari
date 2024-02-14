"use strict";
var selenium_webdriver_1 = require("selenium-webdriver");
var commandDefinitions = require("./command_definitions");
var deferred_executor_1 = require("./deferred_executor");
var extender_1 = require("./extender");
function extend(baseDriver, fallbackGracefully) {
    if (fallbackGracefully === void 0) { fallbackGracefully = false; }
    var extender = new extender_1.Extender(baseDriver);
    var extendedDriver = baseDriver;
    for (var commandName in commandDefinitions) {
        extendedDriver[commandName] =
            commandDefinitions[commandName].compile(extender, fallbackGracefully);
    }
    return extendedDriver;
}
exports.extend = extend;
/**
 * Patches webdriver so that the extender can defie new commands.
 *
 * @example
 * patch(require('selenium-webdriver/lib/command'),
 *     require('selenium-webdriver/executors'),
 *     require('selenium-webdriver/http'));
 *
 * @param {*} lib_command The object at 'selenium-webdriver/lib/command'
 * @param {*} executors The object at 'selenium-webdriver/executors'
 * @param {*} http The object at 'selenium-webdriver/http'
 */
function patch(lib_command, executors, http) {
    if (lib_command.DeferredExecutor === undefined) {
        throw new Error('The version of `selenium-webdriver` you provided does ' +
            'not use Deferred Executors.  Are you using version 3.x or above? If ' +
            'so, you do not need to call the `patch()` function.');
    }
    lib_command.DeferredExecutor = deferred_executor_1.DeferredExecutor;
    executors.DeferredExecutor = deferred_executor_1.DeferredExecutor;
    // Based off of
    // https://github.com/SeleniumHQ/selenium/blob/selenium-2.53.0/javascript/node/selenium-webdriver/executors.js#L45
    executors.createExecutor = function (url, opt_agent, opt_proxy) {
        return new deferred_executor_1.DeferredExecutor(selenium_webdriver_1.promise.when(url, function (url) {
            var client = new http.HttpClient(url, opt_agent, opt_proxy);
            return new http.Executor(client);
        }));
    };
}
exports.patch = patch;
//# sourceMappingURL=index.js.map