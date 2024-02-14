"use strict";
const core_1 = require('@angular/core');
class MaxPipe {
    transform(value, ...args) {
        let allowed = args[0];
        let received = value.length;
        if (received > allowed && allowed !== 0) {
            let toCut = allowed - received;
            return value.slice(0, toCut);
        }
        return value;
    }
}
MaxPipe.decorators = [
    { type: core_1.Pipe, args: [{ name: 'max' },] },
];
MaxPipe.ctorParameters = [];
exports.MaxPipe = MaxPipe;
//# sourceMappingURL=max.pipe.js.map