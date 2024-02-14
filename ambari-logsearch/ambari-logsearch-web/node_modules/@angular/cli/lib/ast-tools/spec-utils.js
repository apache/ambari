"use strict";
// This file exports a version of the Jasmine `it` that understands promises.
// To use this, simply `import {it} from './spec-utils`.
// TODO(hansl): move this to its own Jasmine-TypeScript package.
Object.defineProperty(exports, "__esModule", { value: true });
function async(fn) {
    return (done) => {
        let result = null;
        try {
            result = fn();
            if (result && 'then' in result) {
                result.then(done, done.fail);
            }
            else {
                done();
            }
        }
        catch (err) {
            done.fail(err);
        }
    };
}
function it(description, fn) {
    return global['it'](description, async(fn));
}
exports.it = it;
//# sourceMappingURL=/users/hansl/sources/angular-cli/lib/ast-tools/spec-utils.js.map