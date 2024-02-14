/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
"use strict";
var path = require("path");
// Postprocess generated JS.
function pathToModuleName(context, fileName) {
    fileName = fileName.replace(/\.js$/, '');
    if (fileName[0] === '.') {
        // './foo' or '../foo'.
        // Resolve the path against the dirname of the current module.
        fileName = path.join(path.dirname(context), fileName);
    }
    // Replace characters not supported by goog.module.
    var moduleName = fileName.replace(/\//g, '.').replace(/^[^a-zA-Z_$]/, '_').replace(/[^a-zA-Z0-9._$]/g, '_');
    return moduleName;
}
exports.pathToModuleName = pathToModuleName;

//# sourceMappingURL=cli_support.js.map
