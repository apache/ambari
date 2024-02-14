/*
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
var lines_1 = require("./lines");
var lintError_1 = require("./lintError");
/**
 * Takes the full text of a .lint file and returns the contents of the file
 * with all error markup removed
 */
function removeErrorMarkup(text) {
    var textWithMarkup = text.split("\n");
    var lines = textWithMarkup.map(lines_1.parseLine);
    var codeText = lines.filter(function (line) { return (line instanceof lines_1.CodeLine); }).map(function (line) { return line.contents; });
    return codeText.join("\n");
}
exports.removeErrorMarkup = removeErrorMarkup;
/* tslint:disable:object-literal-sort-keys */
/**
 * Takes the full text of a .lint file and returns an array of LintErrors
 * corresponding to the error markup in the file.
 */
function parseErrorsFromMarkup(text) {
    var textWithMarkup = text.split("\n");
    var lines = textWithMarkup.map(lines_1.parseLine);
    if (lines.length > 0 && !(lines[0] instanceof lines_1.CodeLine)) {
        throw lintError_1.lintSyntaxError("text cannot start with an error mark line.");
    }
    var messageSubstitutionLines = lines.filter(function (l) { return l instanceof lines_1.MessageSubstitutionLine; });
    var messageSubstitutions = new Map(messageSubstitutionLines.map(function (_a) {
        var key = _a.key, message = _a.message;
        return [key, message];
    }));
    // errorLineForCodeLine[5] contains all the ErrorLine objects associated with the 5th line of code, for example
    var errorLinesForCodeLines = createCodeLineNoToErrorsMap(lines);
    var lintErrors = [];
    function addError(errorLine, errorStartPos, lineNo) {
        lintErrors.push({
            startPos: errorStartPos,
            endPos: { line: lineNo, col: errorLine.endCol },
            message: messageSubstitutions.get(errorLine.message) || errorLine.message,
        });
    }
    // for each line of code...
    errorLinesForCodeLines.forEach(function (errorLinesForLineOfCode, lineNo) {
        // for each error marking on that line...
        while (errorLinesForLineOfCode.length > 0) {
            var errorLine = errorLinesForLineOfCode.shift();
            var errorStartPos = { line: lineNo, col: errorLine.startCol };
            // if the error starts and ends on this line, add it now to list of errors
            if (errorLine instanceof lines_1.EndErrorLine) {
                addError(errorLine, errorStartPos, lineNo);
                // if the error is the start of a multiline error
            }
            else if (errorLine instanceof lines_1.MultilineErrorLine) {
                // iterate through the MultilineErrorLines until we get to an EndErrorLine
                for (var nextLineNo = lineNo + 1;; ++nextLineNo) {
                    if (!isValidErrorMarkupContinuation(errorLinesForCodeLines, nextLineNo)) {
                        throw lintError_1.lintSyntaxError("Error mark starting at " + errorStartPos.line + ":" + errorStartPos.col + " does not end correctly.");
                    }
                    else {
                        var nextErrorLine = errorLinesForCodeLines[nextLineNo].shift();
                        // if end of multiline error, add it it list of errors
                        if (nextErrorLine instanceof lines_1.EndErrorLine) {
                            addError(nextErrorLine, errorStartPos, nextLineNo);
                            break;
                        }
                    }
                }
            }
        }
    });
    lintErrors.sort(lintError_1.errorComparator);
    return lintErrors;
}
exports.parseErrorsFromMarkup = parseErrorsFromMarkup;
function createMarkupFromErrors(code, lintErrors) {
    lintErrors.sort(lintError_1.errorComparator);
    var codeText = code.split("\n");
    var errorLinesForCodeText = codeText.map(function () { return []; });
    for (var _i = 0, lintErrors_1 = lintErrors; _i < lintErrors_1.length; _i++) {
        var error = lintErrors_1[_i];
        var startPos = error.startPos, endPos = error.endPos, message = error.message;
        if (startPos.line === endPos.line) {
            // single line error
            errorLinesForCodeText[startPos.line].push(new lines_1.EndErrorLine(startPos.col, endPos.col, message));
        }
        else {
            // multiline error
            errorLinesForCodeText[startPos.line].push(new lines_1.MultilineErrorLine(startPos.col));
            for (var lineNo = startPos.line + 1; lineNo < endPos.line; ++lineNo) {
                errorLinesForCodeText[lineNo].push(new lines_1.MultilineErrorLine(0));
            }
            errorLinesForCodeText[endPos.line].push(new lines_1.EndErrorLine(0, endPos.col, message));
        }
    }
    var finalText = combineCodeTextAndErrorLines(codeText, errorLinesForCodeText);
    return finalText.join("\n");
}
exports.createMarkupFromErrors = createMarkupFromErrors;
/* tslint:enable:object-literal-sort-keys */
function combineCodeTextAndErrorLines(codeText, errorLinesForCodeText) {
    return codeText.reduce(function (resultText, code, i) {
        resultText.push(code);
        var errorPrintLines = errorLinesForCodeText[i].map(function (line) { return lines_1.printLine(line, code); }).filter(function (line) { return line !== null; });
        resultText.push.apply(resultText, errorPrintLines);
        return resultText;
    }, []);
}
function createCodeLineNoToErrorsMap(lines) {
    var errorLinesForCodeLine = [];
    for (var _i = 0, lines_2 = lines; _i < lines_2.length; _i++) {
        var line = lines_2[_i];
        if (line instanceof lines_1.CodeLine) {
            errorLinesForCodeLine.push([]);
        }
        else if (line instanceof lines_1.ErrorLine) {
            errorLinesForCodeLine[errorLinesForCodeLine.length - 1].push(line);
        }
    }
    return errorLinesForCodeLine;
}
function isValidErrorMarkupContinuation(errorLinesForCodeLines, lineNo) {
    return lineNo < errorLinesForCodeLines.length
        && errorLinesForCodeLines[lineNo].length !== 0
        && errorLinesForCodeLines[lineNo][0].startCol === 0;
}
