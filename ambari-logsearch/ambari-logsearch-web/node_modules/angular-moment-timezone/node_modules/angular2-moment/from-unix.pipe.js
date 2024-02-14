/* angular2-moment (c) 2015, 2016 Uri Shaked / MIT Licence */
"use strict";
var core_1 = require('@angular/core');
var moment = require('moment');
var FromUnixPipe = (function () {
    function FromUnixPipe() {
    }
    FromUnixPipe.prototype.transform = function (value) {
        var args = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            args[_i - 1] = arguments[_i];
        }
        if (typeof value === 'string') {
            value = +value;
        }
        return moment.unix(value);
    };
    FromUnixPipe.decorators = [
        { type: core_1.Pipe, args: [{ name: 'amFromUnix' },] },
    ];
    /** @nocollapse */
    FromUnixPipe.ctorParameters = [];
    return FromUnixPipe;
}());
exports.FromUnixPipe = FromUnixPipe;
//# sourceMappingURL=from-unix.pipe.js.map