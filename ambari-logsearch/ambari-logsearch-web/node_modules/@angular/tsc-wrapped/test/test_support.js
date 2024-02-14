"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var os = require("os");
var path = require("path");
var tmpdir = process.env.TEST_TMPDIR || os.tmpdir();
function writeTempFile(name, contents) {
    // TEST_TMPDIR is set by bazel.
    var id = (Math.random() * 1000000).toFixed(0);
    var fn = path.join(tmpdir, "tmp." + id + "." + name);
    fs.writeFileSync(fn, contents);
    return fn;
}
exports.writeTempFile = writeTempFile;
function makeTempDir() {
    var id = (Math.random() * 1000000).toFixed(0);
    var dir = path.join(tmpdir, "tmp." + id);
    fs.mkdirSync(dir);
    return dir;
}
exports.makeTempDir = makeTempDir;
//# sourceMappingURL=test_support.js.map