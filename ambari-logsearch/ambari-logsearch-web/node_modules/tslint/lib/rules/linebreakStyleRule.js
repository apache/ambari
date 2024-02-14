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
var ts = require("typescript");
var Lint = require("../index");
var OPTION_LINEBREAK_STYLE_CRLF = "CRLF";
var OPTION_LINEBREAK_STYLE_LF = "LF";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var failures = [];
        var scanner = ts.createScanner(sourceFile.languageVersion, false, sourceFile.languageVariant, sourceFile.getFullText());
        var ruleArguments = this.getOptions().ruleArguments;
        var linebreakStyle = ruleArguments.length > 0 ? ruleArguments[0] : OPTION_LINEBREAK_STYLE_LF;
        var expectLF = linebreakStyle === OPTION_LINEBREAK_STYLE_CRLF;
        var expectedEOL = expectLF ? "\r\n" : "\n";
        var failureString = expectLF ? Rule.FAILURE_STRINGS.CRLF : Rule.FAILURE_STRINGS.LF;
        for (var token = scanner.scan(); token !== ts.SyntaxKind.EndOfFileToken; token = scanner.scan()) {
            if (token === ts.SyntaxKind.NewLineTrivia) {
                var text = scanner.getTokenText();
                if (text !== expectedEOL) {
                    failures.push(this.createFailure(sourceFile, scanner, failureString));
                }
            }
        }
        return failures;
    };
    Rule.prototype.createFailure = function (sourceFile, scanner, failure) {
        // get the start of the current line
        var start = sourceFile.getPositionOfLineAndCharacter(sourceFile.getLineAndCharacterOfPosition(scanner.getStartPos()).line, 0);
        // since line endings are not visible, we simply end at the beginning of
        // the line ending, which happens to be the start of the token.
        var end = scanner.getStartPos();
        return new Lint.RuleFailure(sourceFile, start, end, failure, this.getOptions().ruleName);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "linebreak-style",
    description: "Enforces a consistent linebreak style.",
    optionsDescription: (_a = ["\n            One of the following options must be provided:\n\n            * `\"", "\"` requires LF (`\\n`) linebreaks\n            * `\"", "\"` requires CRLF (`\\r\\n`) linebreaks"], _a.raw = ["\n            One of the following options must be provided:\n\n            * \\`\"", "\"\\` requires LF (\\`\\\\n\\`) linebreaks\n            * \\`\"", "\"\\` requires CRLF (\\`\\\\r\\\\n\\`) linebreaks"], Lint.Utils.dedent(_a, OPTION_LINEBREAK_STYLE_LF, OPTION_LINEBREAK_STYLE_CRLF)),
    options: {
        type: "string",
        enum: [OPTION_LINEBREAK_STYLE_LF, OPTION_LINEBREAK_STYLE_CRLF],
    },
    optionExamples: ["[true, \"" + OPTION_LINEBREAK_STYLE_LF + "\"]", "[true, \"" + OPTION_LINEBREAK_STYLE_CRLF + "\"]"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRINGS = {
    CRLF: "Expected linebreak to be '" + OPTION_LINEBREAK_STYLE_CRLF + "'",
    LF: "Expected linebreak to be '" + OPTION_LINEBREAK_STYLE_LF + "'",
};
exports.Rule = Rule;
var _a;
