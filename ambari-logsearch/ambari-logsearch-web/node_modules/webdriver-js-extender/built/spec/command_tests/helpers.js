"use strict";
var webdriver = require("selenium-webdriver");
var commandDefinitions = require("../../lib/command_definitions");
var mock_server_1 = require("../mock-server");
var commands_1 = require("../mock-server/commands");
var selenium_mock_1 = require("selenium-mock");
var lib_1 = require("../../lib");
var portfinder = require('portfinder');
var commandMap = null;
function buildCommandMap(commandList) {
    if (commandMap == null) {
        commandMap = {};
    }
    for (var commandName in commandList) {
        var command = commandList[commandName];
        if (command instanceof selenium_mock_1.Command) {
            commandMap[command.method + ':' + (command.path[0] == '/' ? '' : '/') + command.path] = command;
        }
        else {
            buildCommandMap(command);
        }
    }
}
function initMockSeleniumStandaloneServerAndGetDriverFactory(annotateCommands) {
    if (annotateCommands === void 0) { annotateCommands = false; }
    var server;
    var port;
    beforeAll(function (done) {
        lib_1.patch(require('selenium-webdriver/lib/command'), require('selenium-webdriver/executors'), require('selenium-webdriver/http'));
        portfinder.getPort(function (err, p) {
            if (err) {
                done.fail(err);
            }
            else {
                port = p;
                server = new mock_server_1.MockAppium(port);
                server.start();
                done();
            }
        });
    });
    if (annotateCommands && !commandMap) {
        buildCommandMap(commands_1.session);
    }
    return function () {
        var driver = lib_1.extend(new webdriver.Builder().
            usingServer('http://localhost:' + port + '/wd/hub').
            withCapabilities({ browserName: 'chrome' }).build());
        if (annotateCommands) {
            Object.keys(commandDefinitions).forEach(function (commandName) {
                var clientCommand = commandDefinitions[commandName];
                var serverCommand = commandMap[clientCommand.method + ':' +
                    (clientCommand.path[0] == '/' ? '' : '/') + clientCommand.path];
                var spy = spyOn(serverCommand, 'exec').and.callThrough();
                var oldFun = driver[commandName];
                driver[commandName] = function () {
                    var oldCount = spy.calls.count();
                    return oldFun.apply(this, arguments).then(function (result) {
                        expect(spy.calls.count()).toBe(oldCount + 1);
                        var args = spy.calls.mostRecent().args;
                        return {
                            result: result,
                            session: args[0],
                            params: args[1]
                        };
                    });
                };
            });
        }
        return driver;
    };
}
exports.initMockSeleniumStandaloneServerAndGetDriverFactory = initMockSeleniumStandaloneServerAndGetDriverFactory;
//# sourceMappingURL=helpers.js.map