/**
 * @license
 * Copyright 2016 Palantir Technologies, Inc.
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
var colors = require("colors");
var diff = require("diff");
var fs = require("fs");
var glob = require("glob");
var path = require("path");
var ts = require("typescript");
var rule_1 = require("./language/rule/rule");
var Linter = require("./linter");
var parse = require("./test/parse");
var MARKUP_FILE_EXTENSION = ".lint";
var FIXES_FILE_EXTENSION = ".fix";
function runTests(pattern, rulesDirectory) {
    return glob.sync(pattern + "/tslint.json")
        .map(function (directory) { return runTest(path.dirname(directory), rulesDirectory); });
}
exports.runTests = runTests;
function runTest(testDirectory, rulesDirectory) {
    // needed to get colors to show up when passing through Grunt
    colors.enabled = true;
    var filesToLint = glob.sync(path.join(testDirectory, "**/*" + MARKUP_FILE_EXTENSION));
    var tslintConfig = Linter.findConfiguration(path.join(testDirectory, "tslint.json"), "").results;
    var tsConfig = path.join(testDirectory, "tsconfig.json");
    var compilerOptions = { allowJs: true };
    if (fs.existsSync(tsConfig)) {
        var _a = ts.readConfigFile(tsConfig, ts.sys.readFile), config = _a.config, error = _a.error;
        if (error) {
            throw new Error(JSON.stringify(error));
        }
        var parseConfigHost = {
            fileExists: fs.existsSync,
            readDirectory: ts.sys.readDirectory,
            readFile: function (file) { return fs.readFileSync(file, "utf8"); },
            useCaseSensitiveFileNames: true,
        };
        compilerOptions = ts.parseJsonConfigFileContent(config, parseConfigHost, testDirectory).options;
    }
    var results = { directory: testDirectory, results: {} };
    var _loop_1 = function (fileToLint) {
        var fileBasename = path.basename(fileToLint, MARKUP_FILE_EXTENSION);
        var fileCompileName = fileBasename.replace(/\.lint$/, "");
        var fileText = fs.readFileSync(fileToLint, "utf8");
        var fileTextWithoutMarkup = parse.removeErrorMarkup(fileText);
        var errorsFromMarkup = parse.parseErrorsFromMarkup(fileText);
        var program = void 0;
        if (tslintConfig !== undefined && tslintConfig.linterOptions && tslintConfig.linterOptions.typeCheck) {
            var compilerHost = {
                fileExists: function () { return true; },
                getCanonicalFileName: function (filename) { return filename; },
                getCurrentDirectory: function () { return ""; },
                getDefaultLibFileName: function () { return ts.getDefaultLibFileName(compilerOptions); },
                getDirectories: function (_path) { return []; },
                getNewLine: function () { return "\n"; },
                getSourceFile: function (filenameToGet) {
                    var target = compilerOptions.target === undefined ? ts.ScriptTarget.ES5 : compilerOptions.target;
                    if (filenameToGet === ts.getDefaultLibFileName(compilerOptions)) {
                        var fileContent = fs.readFileSync(ts.getDefaultLibFilePath(compilerOptions)).toString();
                        return ts.createSourceFile(filenameToGet, fileContent, target);
                    }
                    else if (filenameToGet === fileCompileName) {
                        return ts.createSourceFile(fileBasename, fileTextWithoutMarkup, target, true);
                    }
                    else if (fs.existsSync(path.resolve(path.dirname(fileToLint), filenameToGet))) {
                        var text = fs.readFileSync(path.resolve(path.dirname(fileToLint), filenameToGet), { encoding: "utf-8" });
                        return ts.createSourceFile(filenameToGet, text, target, true);
                    }
                    throw new Error("Couldn't get source file '" + filenameToGet + "'");
                },
                readFile: function (x) { return x; },
                useCaseSensitiveFileNames: function () { return true; },
                writeFile: function () { return null; },
            };
            program = ts.createProgram([fileCompileName], compilerOptions, compilerHost);
            // perform type checking on the program, updating nodes with symbol table references
            ts.getPreEmitDiagnostics(program);
        }
        var lintOptions = {
            fix: false,
            formatter: "prose",
            formattersDirectory: "",
            rulesDirectory: rulesDirectory,
        };
        var linter = new Linter(lintOptions, program);
        linter.lint(fileBasename, fileTextWithoutMarkup, tslintConfig);
        var failures = linter.getResult().failures;
        var errorsFromLinter = failures.map(function (failure) {
            var startLineAndCharacter = failure.getStartPosition().getLineAndCharacter();
            var endLineAndCharacter = failure.getEndPosition().getLineAndCharacter();
            return {
                endPos: {
                    col: endLineAndCharacter.character,
                    line: endLineAndCharacter.line,
                },
                message: failure.getFailure(),
                startPos: {
                    col: startLineAndCharacter.character,
                    line: startLineAndCharacter.line,
                },
            };
        });
        // test against fixed files
        var fixedFileText = "";
        var newFileText = "";
        try {
            var fixedFile = fileToLint.replace(/\.lint$/, FIXES_FILE_EXTENSION);
            var stat = fs.statSync(fixedFile);
            if (stat.isFile()) {
                fixedFileText = fs.readFileSync(fixedFile, "utf8");
                var fixes = failures.filter(function (f) { return f.hasFix(); }).map(function (f) { return f.getFix(); });
                newFileText = rule_1.Fix.applyAll(fileTextWithoutMarkup, fixes);
            }
        }
        catch (e) {
            fixedFileText = "";
            newFileText = "";
        }
        results.results[fileToLint] = {
            errorsFromLinter: errorsFromLinter,
            errorsFromMarkup: errorsFromMarkup,
            fixesFromLinter: newFileText,
            fixesFromMarkup: fixedFileText,
            markupFromLinter: parse.createMarkupFromErrors(fileTextWithoutMarkup, errorsFromMarkup),
            markupFromMarkup: parse.createMarkupFromErrors(fileTextWithoutMarkup, errorsFromLinter),
        };
    };
    for (var _i = 0, filesToLint_1 = filesToLint; _i < filesToLint_1.length; _i++) {
        var fileToLint = filesToLint_1[_i];
        _loop_1(fileToLint);
    }
    return results;
}
exports.runTest = runTest;
function consoleTestResultsHandler(testResults) {
    var didAllTestsPass = true;
    for (var _i = 0, testResults_1 = testResults; _i < testResults_1.length; _i++) {
        var testResult = testResults_1[_i];
        if (!consoleTestResultHandler(testResult)) {
            didAllTestsPass = false;
        }
    }
    return didAllTestsPass;
}
exports.consoleTestResultsHandler = consoleTestResultsHandler;
function consoleTestResultHandler(testResult) {
    var didAllTestsPass = true;
    for (var _i = 0, _a = Object.keys(testResult.results); _i < _a.length; _i++) {
        var fileName = _a[_i];
        var results = testResult.results[fileName];
        process.stdout.write(fileName + ":");
        var markupDiffResults = diff.diffLines(results.markupFromMarkup, results.markupFromLinter);
        var fixesDiffResults = diff.diffLines(results.fixesFromLinter, results.fixesFromMarkup);
        var didMarkupTestPass = !markupDiffResults.some(function (diff) { return !!diff.added || !!diff.removed; });
        var didFixesTestPass = !fixesDiffResults.some(function (diff) { return !!diff.added || !!diff.removed; });
        /* tslint:disable:no-console */
        if (didMarkupTestPass && didFixesTestPass) {
            console.log(colors.green(" Passed"));
        }
        else {
            console.log(colors.red(" Failed!"));
            didAllTestsPass = false;
            if (!didMarkupTestPass) {
                displayDiffResults(markupDiffResults, MARKUP_FILE_EXTENSION);
            }
            if (!didFixesTestPass) {
                displayDiffResults(fixesDiffResults, FIXES_FILE_EXTENSION);
            }
        }
        /* tslint:enable:no-console */
    }
    return didAllTestsPass;
}
exports.consoleTestResultHandler = consoleTestResultHandler;
function displayDiffResults(diffResults, extension) {
    /* tslint:disable:no-console */
    console.log(colors.green("Expected (from " + extension + " file)"));
    console.log(colors.red("Actual (from TSLint)"));
    for (var _i = 0, diffResults_1 = diffResults; _i < diffResults_1.length; _i++) {
        var diffResult = diffResults_1[_i];
        var color = colors.grey;
        if (diffResult.added) {
            color = colors.green;
        }
        else if (diffResult.removed) {
            color = colors.red;
        }
        process.stdout.write(color(diffResult.value));
    }
    /* tslint:enable:no-console */
}
