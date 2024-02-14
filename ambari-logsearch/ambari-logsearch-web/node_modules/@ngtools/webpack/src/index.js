"use strict";
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
Object.defineProperty(exports, "__esModule", { value: true });
// @ignoreDep @angular/compiler-cli
const path = require("path");
let version;
// Check that Angular is available.
try {
    version = require('@angular/compiler-cli').VERSION;
}
catch (e) {
    throw new Error('The "@angular/compiler-cli" package was not properly installed. Error: ' + e);
}
// Check that Angular is also not part of this module's node_modules (it should be the project's).
const compilerCliPath = require.resolve('@angular/compiler-cli');
if (compilerCliPath.startsWith(path.dirname(__dirname))) {
    throw new Error('The @ngtools/webpack plugin now relies on the project @angular/compiler-cli. '
        + 'Please clean your node_modules and reinstall.');
}
// Throw if we're neither 2.3.1 or more, nor 4.x.y, nor 5.x.y.
if (!(version.major == '5'
    || version.major == '4'
    || (version.major == '2'
        && (version.minor == '4'
            || version.minor == '3' && version.patch == '1')))) {
    throw new Error('Version of @angular/compiler-cli needs to be 2.3.1 or greater. '
        + `Current version is "${version.full}".`);
}
__export(require("./plugin"));
__export(require("./extract_i18n_plugin"));
var loader_1 = require("./loader");
exports.default = loader_1.ngcLoader;
var paths_plugin_1 = require("./paths-plugin");
exports.PathsPlugin = paths_plugin_1.PathsPlugin;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/index.js.map