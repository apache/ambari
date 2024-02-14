"use strict";
var lib_1 = require("../lib");
var deferred_executor_1 = require("../lib/deferred_executor");
var mockdriver_1 = require("./mockdriver");
describe('extender', function () {
    it('should support setting/getting the network connection', function (done) {
        var ncType;
        var baseDriver = mockdriver_1.buildMockDriver('42', function (name, method, path) { }, function (path, method, data) {
            expect(path).toEqual('/session/42/network_connection');
            if (method == 'GET') {
                expect(Object.keys(data).length).toEqual(0);
                return ncType;
            }
            else if (method == 'POST') {
                expect(JSON.stringify(Object.keys(data))).toEqual('["type"]');
                ncType = data['type'];
            }
        });
        var driver = lib_1.extend(baseDriver);
        driver.setNetworkConnection(5).then(function () {
            return driver.getNetworkConnection();
        }).then(function (connectionType) {
            expect(connectionType).toEqual(5);
            done();
        });
    });
    it('should patch selenium-webdriver', function () {
        lib_1.patch(require('selenium-webdriver/lib/command'), require('selenium-webdriver/executors'), require('selenium-webdriver/http'));
        expect(require('selenium-webdriver/executors').createExecutor('http://localhost')).toEqual(jasmine.any(deferred_executor_1.DeferredExecutor));
    });
});
//# sourceMappingURL=index_spec.js.map