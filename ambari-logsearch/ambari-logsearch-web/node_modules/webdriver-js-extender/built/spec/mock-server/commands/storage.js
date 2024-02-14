"use strict";
/**
 * In this file we define a factory which can be used to create the commands for either
 * sessionStorage or localStorage
 */
var selenium_mock_1 = require("selenium-mock");
function storageFactory(type) {
    var storageCmds = {};
    function cmdFactory(method, relPath, fun) {
        return new selenium_mock_1.Command(method, type + '_storage' + relPath, function (session, params) {
            return fun(session[type + '_storage'], params['key'], params['value']);
        });
    }
    storageCmds.getKeys = cmdFactory('GET', '', function (store) {
        return Object.keys(store);
    });
    storageCmds.getValue = cmdFactory('GET', '/key/:key', function (store, key) {
        return store[key];
    });
    storageCmds.setValue = cmdFactory('POST', '', function (store, key, value) {
        store[key] = value;
    });
    storageCmds.deleteEntry = cmdFactory('DELETE', '/key/:key', function (store, key) {
        delete store[key];
    });
    storageCmds.deleteAll = cmdFactory('DELETE', '', function (store) {
        for (var key in store) {
            delete store[key];
        }
    });
    storageCmds.getSize = cmdFactory('GET', '/size', function (store) {
        return Object.keys(store).length;
    });
    return storageCmds;
}
exports.storageFactory = storageFactory;
;
//# sourceMappingURL=storage.js.map