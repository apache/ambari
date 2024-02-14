"use strict";
var webdriver = require("selenium-webdriver");
var deferred_executor_1 = require("../lib/deferred_executor");
describe('Deferred Executor', function () {
    function makeExecutor(done) {
        return {
            execute: function (command) {
                expect(command).toBe('command');
                done();
            },
            defineCommand: function (name, method, path) {
                expect(name).toBe('name');
                expect(method).toBe('method');
                expect(path).toBe('path');
                done();
            }
        };
    }
    ;
    it('should call execute on pending executors', function (done) {
        var deferred = webdriver.promise.defer();
        var deferredExecutor = new deferred_executor_1.DeferredExecutor(deferred.promise);
        deferredExecutor.execute('command');
        deferred.fulfill(makeExecutor(done));
    });
    it('should call execute on fulfilled executors', function (done) {
        var deferred = webdriver.promise.defer();
        var deferredExecutor = new deferred_executor_1.DeferredExecutor(deferred.promise);
        deferred.fulfill(makeExecutor(done));
        deferredExecutor.execute('command');
    });
    it('should call defineCommand on pending executors', function (done) {
        var deferred = webdriver.promise.defer();
        var deferredExecutor = new deferred_executor_1.DeferredExecutor(deferred.promise);
        deferredExecutor.defineCommand('name', 'method', 'path');
        deferred.fulfill(makeExecutor(done));
    });
    it('should call defineCommand on fulfilled executors', function (done) {
        var deferred = webdriver.promise.defer();
        var deferredExecutor = new deferred_executor_1.DeferredExecutor(deferred.promise);
        deferred.fulfill(makeExecutor(done));
        deferredExecutor.defineCommand('name', 'method', 'path');
    });
});
//# sourceMappingURL=deferred_executor_spec.js.map