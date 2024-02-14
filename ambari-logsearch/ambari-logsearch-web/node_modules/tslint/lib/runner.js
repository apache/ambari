/**
 * @license
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var glob = require("glob");
var path = require("path");
var ts = require("typescript");
var configuration_1 = require("./configuration");
var error_1 = require("./error");
var Linter = require("./linter");
var test_1 = require("./test");
var updateNotifier_1 = require("./updateNotifier");
var Runner = (function () {
    function Runner(options, outputStream) {
        this.options = options;
        this.outputStream = outputStream;
    }
    Runner.trimSingleQuotes = function (str) {
        return str.replace(/^'|'$/g, "");
    };
    Runner.prototype.run = function (onComplete) {
        if (this.options.version) {
            this.outputStream.write(Linter.VERSION + "\n");
            onComplete(0);
            return;
        }
        if (this.options.init) {
            if (fs.existsSync(configuration_1.CONFIG_FILENAME)) {
                console.error("Cannot generate " + configuration_1.CONFIG_FILENAME + ": file already exists");
                onComplete(1);
                return;
            }
            var tslintJSON = JSON.stringify(configuration_1.DEFAULT_CONFIG, undefined, "    ");
            fs.writeFileSync(configuration_1.CONFIG_FILENAME, tslintJSON);
            onComplete(0);
            return;
        }
        if (this.options.test) {
            var results = test_1.runTests(this.options.test, this.options.rulesDirectory);
            var didAllTestsPass = test_1.consoleTestResultsHandler(results);
            onComplete(didAllTestsPass ? 0 : 1);
            return;
        }
        // when provided, it should point to an existing location
        if (this.options.config && !fs.existsSync(this.options.config)) {
            console.error("Invalid option for configuration: " + this.options.config);
            onComplete(1);
            return;
        }
        // if both files and tsconfig are present, use files
        var files = this.options.files === undefined ? [] : this.options.files;
        var program;
        if (this.options.project != null) {
            if (!fs.existsSync(this.options.project)) {
                console.error("Invalid option for project: " + this.options.project);
                onComplete(1);
                return;
            }
            program = Linter.createProgram(this.options.project, path.dirname(this.options.project));
            if (files.length === 0) {
                files = Linter.getFileNames(program);
            }
            if (this.options.typeCheck) {
                // if type checking, run the type checker
                var diagnostics = ts.getPreEmitDiagnostics(program);
                if (diagnostics.length > 0) {
                    var messages = diagnostics.map(function (diag) {
                        // emit any error messages
                        var message = ts.DiagnosticCategory[diag.category];
                        if (diag.file) {
                            var _a = diag.file.getLineAndCharacterOfPosition(diag.start), line = _a.line, character = _a.character;
                            message += " at " + diag.file.fileName + ":" + (line + 1) + ":" + (character + 1) + ":";
                        }
                        message += " " + ts.flattenDiagnosticMessageText(diag.messageText, "\n");
                        return message;
                    });
                    throw new Error(messages.join("\n"));
                }
            }
            else {
                // if not type checking, we don't need to pass in a program object
                program = undefined;
            }
        }
        var ignorePatterns = [];
        if (this.options.exclude) {
            var excludeArguments = Array.isArray(this.options.exclude) ? this.options.exclude : [this.options.exclude];
            ignorePatterns = excludeArguments.map(Runner.trimSingleQuotes);
        }
        files = files
            .map(Runner.trimSingleQuotes)
            .map(function (file) { return glob.sync(file, { ignore: ignorePatterns, nodir: true }); })
            .reduce(function (a, b) { return a.concat(b); }, []);
        try {
            this.processFiles(onComplete, files, program);
        }
        catch (error) {
            if (error.name === error_1.FatalError.NAME) {
                console.error(error.message);
                onComplete(1);
            }
            // rethrow unhandled error
            throw error;
        }
    };
    Runner.prototype.processFiles = function (onComplete, files, program) {
        var _this = this;
        var possibleConfigAbsolutePath = this.options.config != null ? path.resolve(this.options.config) : null;
        var linter = new Linter({
            fix: !!this.options.fix,
            formatter: this.options.format,
            formattersDirectory: this.options.formattersDirectory || "",
            rulesDirectory: this.options.rulesDirectory || "",
        }, program);
        var lastFolder;
        var configFile;
        for (var _i = 0, files_1 = files; _i < files_1.length; _i++) {
            var file = files_1[_i];
            if (!fs.existsSync(file)) {
                console.error("Unable to open file: " + file);
                onComplete(1);
                return;
            }
            var buffer = new Buffer(256);
            buffer.fill(0);
            var fd = fs.openSync(file, "r");
            try {
                fs.readSync(fd, buffer, 0, 256, 0);
                if (buffer.readInt8(0) === 0x47 && buffer.readInt8(188) === 0x47) {
                    // MPEG transport streams use the '.ts' file extension. They use 0x47 as the frame
                    // separator, repeating every 188 bytes. It is unlikely to find that pattern in
                    // TypeScript source, so tslint ignores files with the specific pattern.
                    console.warn(file + ": ignoring MPEG transport stream");
                    return;
                }
            }
            finally {
                fs.closeSync(fd);
            }
            var contents = fs.readFileSync(file, "utf8");
            var folder = path.dirname(file);
            if (lastFolder !== folder) {
                configFile = configuration_1.findConfiguration(possibleConfigAbsolutePath, folder).results;
                lastFolder = folder;
            }
            linter.lint(file, contents, configFile);
        }
        var lintResult = linter.getResult();
        this.outputStream.write(lintResult.output, function () {
            if (lintResult.failureCount > 0) {
                onComplete(_this.options.force ? 0 : 2);
            }
            else {
                onComplete(0);
            }
        });
        if (lintResult.format === "prose") {
            // Check to see if there are any updates available
            updateNotifier_1.updateNotifierCheck();
        }
    };
    return Runner;
}());
exports.Runner = Runner;
