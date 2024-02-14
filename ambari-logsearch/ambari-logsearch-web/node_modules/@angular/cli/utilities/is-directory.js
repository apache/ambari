"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
function isDirectory(path) {
    try {
        return fs.statSync(path).isDirectory();
    }
    catch (_) {
        return false;
    }
}
exports.isDirectory = isDirectory;
//# sourceMappingURL=/users/hansl/sources/angular-cli/utilities/is-directory.js.map