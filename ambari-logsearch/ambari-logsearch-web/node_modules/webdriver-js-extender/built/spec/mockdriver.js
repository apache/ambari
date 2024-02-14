"use strict";
var webdriver = require("selenium-webdriver");
var buildPath = require('selenium-webdriver/http').buildPath;
function buildMockDriver(sessionId, defineCallback, execCallback) {
    var paths = {};
    var methods = {};
    var mockSession = new webdriver.Session(sessionId, {});
    return new webdriver.WebDriver(mockSession, {
        execute: function (command) {
            command.setParameter('sessionId', sessionId);
            var params = command.getParameters();
            return webdriver.promise.fulfilled(execCallback(buildPath(paths[command.getName()], params), methods[command.getName()], params));
        },
        defineCommand: function (name, method, path) {
            paths[name] = path;
            methods[name] = method;
            defineCallback(name, method, path);
        }
    });
}
exports.buildMockDriver = buildMockDriver;
//# sourceMappingURL=mockdriver.js.map