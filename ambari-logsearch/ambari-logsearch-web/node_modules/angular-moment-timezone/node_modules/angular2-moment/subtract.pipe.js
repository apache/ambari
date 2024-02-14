/* angular2-moment (c) 2015, 2016 Uri Shaked / MIT Licence */
"use strict";
var core_1 = require('@angular/core');
var moment = require('moment');
var SubtractPipe = (function () {
    function SubtractPipe() {
    }
    SubtractPipe.prototype.transform = function (value, amount, unit) {
        if (typeof amount === 'undefined' || (typeof amount === 'number' && typeof unit === 'undefined')) {
            throw new Error('SubtractPipe: missing required arguments');
        }
        return moment(value).subtract(amount, unit);
    };
    SubtractPipe.decorators = [
        { type: core_1.Pipe, args: [{ name: 'amSubtract' },] },
    ];
    /** @nocollapse */
    SubtractPipe.ctorParameters = [];
    return SubtractPipe;
}());
exports.SubtractPipe = SubtractPipe;
//# sourceMappingURL=subtract.pipe.js.map