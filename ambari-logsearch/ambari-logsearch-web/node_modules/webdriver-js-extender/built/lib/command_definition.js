"use strict";
var path_1 = require("path");
var CommandDefinition = (function () {
    function CommandDefinition(name, params, method, path, preprocessParams) {
        if (preprocessParams === void 0) { preprocessParams = function (x) { return x; }; }
        this.name = name;
        this.params = params;
        this.method = method;
        this.path = path;
        this.preprocessParams = preprocessParams;
    }
    CommandDefinition.prototype.compile = function (extender, silentFailure) {
        var _this = this;
        try {
            extender.defineCommand(this.name, this.params, this.method, path_1.posix.join('/session/:sessionId', this.path));
            return function () {
                var args = [];
                for (var _i = 0; _i < arguments.length; _i++) {
                    args[_i] = arguments[_i];
                }
                return extender.execCommand(_this.name, _this.method, _this.preprocessParams(args));
            };
        }
        catch (e) {
            if (silentFailure) {
                return function () {
                    var args = [];
                    for (var _i = 0; _i < arguments.length; _i++) {
                        args[_i] = arguments[_i];
                    }
                    throw new Error('Command "' + _this.name + '" could not be extended onto WebDriver instance. ' +
                        'This is generally a result of using `directConnect` in protractor.');
                };
            }
            else {
                throw e;
            }
        }
    };
    return CommandDefinition;
}());
exports.CommandDefinition = CommandDefinition;
//# sourceMappingURL=command_definition.js.map