"use strict";
/**
 * Helpers for defining commands more quickly.
 *
 * In this file we define some helpers for quickly defining commands with either do nothing,
 * set/get a value on the session, or return a constant value.
 */
var selenium_mock_1 = require("selenium-mock");
function noopFactory(path, method) {
    if (method === void 0) { method = 'POST'; }
    return new selenium_mock_1.Command(method, path, function () { });
}
exports.noopFactory = noopFactory;
function getterFactory(path, name, method) {
    if (method === void 0) { method = 'GET'; }
    name = name || path.split('/').pop();
    return new selenium_mock_1.Command(method, path, function (session) {
        return session[name];
    });
}
exports.getterFactory = getterFactory;
function setterFactory(path, name, paramName) {
    name = name || path.split('/').pop();
    paramName = paramName || name;
    return new selenium_mock_1.Command('POST', path, function (session, params) {
        session[name] = params[paramName];
    });
}
exports.setterFactory = setterFactory;
function constFactory(method, path, val) {
    return new selenium_mock_1.Command(method, path, function () {
        return val;
    });
}
exports.constFactory = constFactory;
//# sourceMappingURL=helpers.js.map