"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var INDEX_HEADER = "/**\n * Generated bundle index. Do not edit.\n */\n";
function privateEntriesToIndex(index, privates) {
    var results = [INDEX_HEADER];
    // Export all of the index symbols.
    results.push("export * from '" + index + "';", '');
    // Simplify the exports
    var exports = new Map();
    for (var _i = 0, privates_1 = privates; _i < privates_1.length; _i++) {
        var entry = privates_1[_i];
        var entries = exports.get(entry.module);
        if (!entries) {
            entries = [];
            exports.set(entry.module, entries);
        }
        entries.push(entry);
    }
    var compareEntries = compare(function (e) { return e.name; });
    var compareModules = compare(function (e) { return e[0]; });
    var orderedExports = Array.from(exports)
        .map(function (_a) {
        var module = _a[0], entries = _a[1];
        return [module, entries.sort(compareEntries)];
    })
        .sort(compareModules);
    for (var _a = 0, orderedExports_1 = orderedExports; _a < orderedExports_1.length; _a++) {
        var _b = orderedExports_1[_a], module_1 = _b[0], entries = _b[1];
        var symbols = entries.map(function (e) { return e.name + " as " + e.privateName; });
        results.push("export {" + symbols + "} from '" + module_1 + "';");
    }
    return results.join('\n');
}
exports.privateEntriesToIndex = privateEntriesToIndex;
function compare(select) {
    return function (a, b) {
        var ak = select(a);
        var bk = select(b);
        return ak > bk ? 1 : ak < bk ? -1 : 0;
    };
}
//# sourceMappingURL=index_writer.js.map