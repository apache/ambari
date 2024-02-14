"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var fs_1 = require("fs");
var path = require("path");
var ts = require("typescript");
var vinyl_file_1 = require("./vinyl_file");
var UserError = (function (_super) {
    __extends(UserError, _super);
    function UserError(message) {
        var _this = _super.call(this, message) || this;
        // Required for TS 2.1, see
        // https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
        Object.setPrototypeOf(_this, UserError.prototype);
        var nativeError = new Error(message);
        _this._nativeError = nativeError;
        return _this;
    }
    Object.defineProperty(UserError.prototype, "message", {
        get: function () { return this._nativeError.message; },
        set: function (message) {
            if (this._nativeError)
                this._nativeError.message = message;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(UserError.prototype, "name", {
        get: function () { return this._nativeError.name; },
        set: function (name) {
            if (this._nativeError)
                this._nativeError.name = name;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(UserError.prototype, "stack", {
        get: function () { return this._nativeError.stack; },
        set: function (value) {
            if (this._nativeError)
                this._nativeError.stack = value;
        },
        enumerable: true,
        configurable: true
    });
    UserError.prototype.toString = function () { return this._nativeError.toString(); };
    return UserError;
}(Error));
exports.UserError = UserError;
var DEBUG = false;
function debug(msg) {
    var o = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        o[_i - 1] = arguments[_i];
    }
    // tslint:disable-next-line:no-console
    if (DEBUG)
        console.log.apply(console, [msg].concat(o));
}
function formatDiagnostics(diags) {
    return diags
        .map(function (d) {
        var res = ts.DiagnosticCategory[d.category];
        if (d.file) {
            res += ' at ' + d.file.fileName + ':';
            var _a = d.file.getLineAndCharacterOfPosition(d.start), line = _a.line, character = _a.character;
            res += (line + 1) + ':' + (character + 1) + ':';
        }
        res += ' ' + ts.flattenDiagnosticMessageText(d.messageText, '\n');
        return res;
    })
        .join('\n');
}
exports.formatDiagnostics = formatDiagnostics;
function check(diags) {
    if (diags && diags.length && diags[0]) {
        throw new UserError(formatDiagnostics(diags));
    }
}
exports.check = check;
function validateAngularCompilerOptions(options) {
    if (options.annotationsAs) {
        switch (options.annotationsAs) {
            case 'decorators':
            case 'static fields':
                break;
            default:
                return [{
                        file: null,
                        start: null,
                        length: null,
                        messageText: 'Angular compiler options "annotationsAs" only supports "static fields" and "decorators"',
                        category: ts.DiagnosticCategory.Error,
                        code: 0
                    }];
        }
    }
}
exports.validateAngularCompilerOptions = validateAngularCompilerOptions;
var Tsc = (function () {
    function Tsc(readFile, readDirectory) {
        if (readFile === void 0) { readFile = ts.sys.readFile; }
        if (readDirectory === void 0) { readDirectory = ts.sys.readDirectory; }
        this.readFile = readFile;
        this.readDirectory = readDirectory;
        this.parseConfigHost = {
            useCaseSensitiveFileNames: true,
            fileExists: fs_1.existsSync,
            readDirectory: this.readDirectory,
            readFile: ts.sys.readFile
        };
    }
    Tsc.prototype.readConfiguration = function (project, basePath, existingOptions) {
        var _this = this;
        // Allow a directory containing tsconfig.json as the project value
        // Note, TS@next returns an empty array, while earlier versions throw
        try {
            if (!vinyl_file_1.isVinylFile(project) && this.readDirectory(project).length > 0) {
                project = path.join(project, 'tsconfig.json');
            }
        }
        catch (e) {
            // Was not a directory, continue on assuming it's a file
        }
        var _a = (function () {
            // project is vinyl like file object
            if (vinyl_file_1.isVinylFile(project)) {
                return { config: JSON.parse(project.contents.toString()), error: null };
            }
            else {
                return ts.readConfigFile(project, _this.readFile);
            }
        })(), config = _a.config, error = _a.error;
        check([error]);
        var parsed = ts.parseJsonConfigFileContent(config, this.parseConfigHost, basePath, existingOptions);
        check(parsed.errors);
        // Default codegen goes to the current directory
        // Parsed options are already converted to absolute paths
        var ngOptions = config.angularCompilerOptions || {};
        ngOptions.genDir = path.join(basePath, ngOptions.genDir || '.');
        for (var _i = 0, _b = Object.keys(parsed.options); _i < _b.length; _i++) {
            var key = _b[_i];
            ngOptions[key] = parsed.options[key];
        }
        check(validateAngularCompilerOptions(ngOptions));
        return { parsed: parsed, ngOptions: ngOptions };
    };
    Tsc.prototype.typeCheck = function (compilerHost, program) {
        debug('Checking global diagnostics...');
        check(program.getGlobalDiagnostics());
        var diagnostics = [];
        debug('Type checking...');
        for (var _i = 0, _a = program.getSourceFiles(); _i < _a.length; _i++) {
            var sf = _a[_i];
            diagnostics.push.apply(diagnostics, ts.getPreEmitDiagnostics(program, sf));
        }
        check(diagnostics);
    };
    Tsc.prototype.emit = function (program) {
        debug('Emitting outputs...');
        var emitResult = program.emit();
        var diagnostics = [];
        diagnostics.push.apply(diagnostics, emitResult.diagnostics);
        return emitResult.emitSkipped ? 1 : 0;
    };
    return Tsc;
}());
exports.Tsc = Tsc;
exports.tsc = new Tsc();
//# sourceMappingURL=tsc.js.map