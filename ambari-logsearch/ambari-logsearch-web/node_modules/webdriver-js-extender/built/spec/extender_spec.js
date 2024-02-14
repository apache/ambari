"use strict";
var extender_1 = require("../lib/extender");
var mockdriver_1 = require("./mockdriver");
var noop_define = function (n, m, p) { };
var noop_exec = function (p, m, d) { };
describe('extender', function () {
    var sessionId = '1234';
    it('should call executor_.defineCommand', function (done) {
        var name = 'customCommand';
        var method = 'post';
        var path = '/custom/command';
        var mockdriver = mockdriver_1.buildMockDriver(sessionId, function (n, m, p) {
            expect(n).toEqual(name);
            expect(m).toEqual(method);
            expect(p).toEqual(path);
            done();
        }, noop_exec);
        var extender = new extender_1.Extender(mockdriver);
        extender.defineCommand(name, [], method, path);
    });
    it('should schedule custom commands', function (done) {
        var name = 'customCommand';
        var method = 'post';
        var path = '/custom/command';
        var mockdriver = mockdriver_1.buildMockDriver(sessionId, noop_define, function (p, m, d) {
            expect(p).toEqual(path);
            expect(m).toEqual(method);
            expect(d['sessionId']).toEqual(sessionId);
            expect(Object.keys(d).length).toEqual(1);
            done();
        });
        var extender = new extender_1.Extender(mockdriver);
        extender.defineCommand(name, [], method, path);
        extender.execCommand(name, method, []);
    });
    it('should use command parameters', function (done) {
        var name = 'customCommand';
        var method = 'post';
        var paramNames = ['var1', 'var2'];
        var paramValues = ['val1', 'val2'];
        var path = '/custom/:var1/command';
        var mockdriver = mockdriver_1.buildMockDriver(sessionId, noop_define, function (p, m, d) {
            expect(p).toEqual('/custom/val1/command');
            expect(m).toEqual(method);
            expect(d['sessionId']).toEqual(sessionId);
            expect(d['var2']).toEqual('val2');
            expect(Object.keys(d).length).toEqual(2);
            done();
        });
        var extender = new extender_1.Extender(mockdriver);
        extender.defineCommand(name, paramNames, method, path);
        extender.execCommand(name, method, paramValues);
    });
    it('should not be able to exec a command that has not been defined', function () {
        var mockdriver = mockdriver_1.buildMockDriver(sessionId, noop_define, noop_exec);
        var extender = new extender_1.Extender(mockdriver);
        expect(function () { extender.execCommand('', '', []); }).toThrowError(RangeError);
    });
    it('should require correct number of parameters for execution', function () {
        var name = 'customCommand';
        var method = 'post';
        var path = '/custom/:command';
        var mockdriver = mockdriver_1.buildMockDriver(sessionId, noop_define, noop_exec);
        var extender = new extender_1.Extender(mockdriver);
        extender.defineCommand(name, ['command'], method, path);
        expect(function () { extender.execCommand(name, method, []); }).toThrowError(RangeError);
    });
});
//# sourceMappingURL=extender_spec.js.map