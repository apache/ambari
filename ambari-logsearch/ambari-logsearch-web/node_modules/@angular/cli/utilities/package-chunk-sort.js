"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const utils_1 = require("../models/webpack-configs/utils");
// Sort chunks according to a predefined order:
// inline, polyfills, all styles, vendor, main
function packageChunkSort(appConfig) {
    let entryPoints = ['inline', 'polyfills', 'sw-register'];
    const pushExtraEntries = (extraEntry) => {
        if (entryPoints.indexOf(extraEntry.entry) === -1) {
            entryPoints.push(extraEntry.entry);
        }
    };
    if (appConfig.styles) {
        utils_1.extraEntryParser(appConfig.styles, './', 'styles').forEach(pushExtraEntries);
    }
    entryPoints.push(...['vendor', 'main']);
    function sort(left, right) {
        let leftIndex = entryPoints.indexOf(left.names[0]);
        let rightindex = entryPoints.indexOf(right.names[0]);
        if (leftIndex > rightindex) {
            return 1;
        }
        else if (leftIndex < rightindex) {
            return -1;
        }
        else {
            return 0;
        }
    }
    // We need to list of entry points for the Ejected webpack config to work (we reuse the function
    // defined above).
    sort.entryPoints = entryPoints;
    return sort;
}
exports.packageChunkSort = packageChunkSort;
//# sourceMappingURL=/users/hansl/sources/angular-cli/utilities/package-chunk-sort.js.map