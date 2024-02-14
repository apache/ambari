"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs_1 = require("fs");
var FileUtils = (function () {
    function FileUtils() {
    }
    FileUtils.isThere = function (file) {
        var exists = true;
        try {
            fs_1.accessSync(file);
        }
        catch (e) {
            exists = false;
        }
        return exists;
    };
    FileUtils.MODULE_DIR = 'node_modules';
    return FileUtils;
}());
exports.FileUtils = FileUtils;
