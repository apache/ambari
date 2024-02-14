/**
 * Custom appium commands
 *
 * In this file we define all the custom commands which are part of the appium API but will probably
 * never be part of the webdriver spec or JsonWireProtocol.
 */
"use strict";
var fs = require("fs");
var selenium_mock_1 = require("selenium-mock");
var helpers_1 = require("./helpers");
var app = {};
var device = {};
exports.appium = {
    app: app,
    device: device
};
app.toBackground =
    new selenium_mock_1.Command('POST', 'appium/app/background', function (session, params) {
        return new Promise(function (resolve) {
            setTimeout(resolve, (params['seconds'] || 0) * 1000);
        });
    });
app.closeApp = helpers_1.noopFactory('appium/app/close');
app.getStrings = helpers_1.constFactory('POST', '/appium/app/strings', ['Hello', 'World']);
app.launch = helpers_1.noopFactory('appium/app/launch');
app.reset = helpers_1.noopFactory('appium/app/reset');
device.getActivity = helpers_1.getterFactory('/appium/device/current_activity', 'activity');
device.startActivity = helpers_1.setterFactory('/appium/device/start_activity', 'activity', 'appActivity');
device.hideKeyboard = helpers_1.noopFactory('/appium/device/hide_keyboard');
device.sendKeyEvent = helpers_1.noopFactory('/appium/device/keyevent');
device.pressKeyCode = helpers_1.noopFactory('/appium/device/press_keycode');
device.longPressKeyCode = helpers_1.noopFactory('/appium/device/long_press_keycode');
device.installApp =
    new selenium_mock_1.Command('POST', 'appium/device/install_app', function (session, params) {
        fs.readFile(params['appPath'], function (err, contents) {
            if (err) {
                throw 'Error while trying to read "' + params['appPath'] + ': ' + err;
            }
            session.installedApps.push(contents.toString().trim());
        });
    });
device.isAppInstalled =
    new selenium_mock_1.Command('POST', 'appium/device/app_installed', function (session, params) {
        return session.installedApps.some(function (app) {
            return app === params['bundleId'] || app === params['appId'];
        });
    });
device.removeApp =
    new selenium_mock_1.Command('POST', '/appium/device/remove_app', function (session, params) {
        session.installedApps = session.installedApps.filter(function (app) {
            return app !== params['bundleId'] && app !== params['appId'];
        });
    });
device.isLocked = helpers_1.getterFactory('/appium/device/is_locked', 'locked', 'POST');
device.lock =
    new selenium_mock_1.Command('POST', 'appium/device/lock', function (session, params) {
        return new Promise(function (resolve) {
            setTimeout(function () {
                session.locked = true;
                resolve();
            }, (params['seconds'] || 0) * 1000);
        });
    });
device.unlock =
    new selenium_mock_1.Command('POST', 'appium/device/unlock', function (session) {
        session.locked = false;
    });
device.pullFile =
    new selenium_mock_1.Command('POST', '/appium/device/pull_file', function (session, params) {
        var path = params['path'].split('/');
        if (path[0].length == 0) {
            path = path.slice(1);
        }
        ;
        var file = session.files;
        for (var _i = 0, path_1 = path; _i < path_1.length; _i++) {
            var folder = path_1[_i];
            file = file[folder];
        }
        return file;
    });
device.pullFolder =
    new selenium_mock_1.Command('POST', '/appium/device/pull_folder', function (session, params) {
        var path = params['path'].split('/');
        if (path[0].length == 0) {
            path = path.slice(1);
        }
        ;
        var folder = session.files;
        for (var _i = 0, path_2 = path; _i < path_2.length; _i++) {
            var name_1 = path_2[_i];
            folder = folder[name_1];
        }
        return folder;
    });
device.pushFile =
    new selenium_mock_1.Command('POST', 'appium/device/push_file', function (session, params) {
        var path = params['path'].split('/');
        if (path[0].length == 0) {
            path = path.slice(1);
        }
        ;
        var folder = session.files;
        for (var i = 0; i < path.length - 1; i++) {
            if (folder[path[i]] === undefined) {
                folder[path[i]] = {};
            }
            folder = folder[path[i]];
        }
        folder[path[path.length - 1]] = params['data'];
    });
device.getTime = helpers_1.constFactory('GET', '/appium/device/system_time', new Date().toString());
device.openNotifications = helpers_1.noopFactory('/appium/device/open_notifications');
device.rotate = helpers_1.noopFactory('appium/device/rotate');
device.shake = helpers_1.noopFactory('appium/device/shake');
exports.appium.getSettings = helpers_1.getterFactory('/appium/settings');
exports.appium.setSettings = helpers_1.setterFactory('/appium/settings');
exports.appium.setImmediateValue = helpers_1.noopFactory('/appium/element/:id/value');
//# sourceMappingURL=appium.js.map