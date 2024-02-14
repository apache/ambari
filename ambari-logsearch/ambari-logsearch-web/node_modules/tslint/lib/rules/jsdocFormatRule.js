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
var utils = require("tsutils");
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new JsdocWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "jsdoc-format",
    description: "Enforces basic format rules for JSDoc comments.",
    descriptionDetails: (_a = ["\n            The following rules are enforced for JSDoc comments (comments starting with `/**`):\n\n            * each line contains an asterisk and asterisks must be aligned\n            * each asterisk must be followed by either a space or a newline (except for the first and the last)\n            * the only characters before the asterisk on each line must be whitespace characters\n            * one line comments must start with `/** ` and end with `*/`"], _a.raw = ["\n            The following rules are enforced for JSDoc comments (comments starting with \\`/**\\`):\n\n            * each line contains an asterisk and asterisks must be aligned\n            * each asterisk must be followed by either a space or a newline (except for the first and the last)\n            * the only characters before the asterisk on each line must be whitespace characters\n            * one line comments must start with \\`/** \\` and end with \\`*/\\`"], Lint.Utils.dedent(_a)),
    rationale: "Helps maintain a consistent, readable style for JSDoc comments.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.ALIGNMENT_FAILURE_STRING = "asterisks in jsdoc must be aligned";
Rule.FORMAT_FAILURE_STRING = "jsdoc is not formatted correctly on this line";
exports.Rule = Rule;
var JsdocWalker = (function (_super) {
    __extends(JsdocWalker, _super);
    function JsdocWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    JsdocWalker.prototype.visitSourceFile = function (node) {
        var _this = this;
        utils.forEachComment(node, function (fullText, comment) {
            if (comment.kind === ts.SyntaxKind.MultiLineCommentTrivia) {
                _this.findFailuresForJsdocComment(fullText.substring(comment.pos, comment.end), comment.pos);
            }
        });
    };
    JsdocWalker.prototype.findFailuresForJsdocComment = function (commentText, startingPosition) {
        var currentPosition = startingPosition;
        // the file may be different depending on the OS it was originally authored on
        // can't rely on require('os').EOL or process.platform as that is the execution env
        // regex is: split optionally on \r\n, but alwasy split on \n if no \r exists
        var lines = commentText.split(/\r?\n/);
        var firstLine = lines[0];
        var jsdocPosition = currentPosition;
        // regex is: start of string, followed by any amount of whitespace, followed by /** but not more than 2 **
        var isJsdocMatch = firstLine.match(/^\s*\/\*\*([^*]|$)/);
        if (isJsdocMatch != null) {
            if (lines.length === 1) {
                var firstLineMatch = firstLine.match(/^\s*\/\*\* (.* )?\*\/$/);
                if (firstLineMatch == null) {
                    this.addFailureAt(jsdocPosition, firstLine.length, Rule.FORMAT_FAILURE_STRING);
                }
                return;
            }
            var indexToMatch = firstLine.indexOf("**") + this.getLineAndCharacterOfPosition(currentPosition).character;
            // all lines but the first and last
            var otherLines = lines.splice(1, lines.length - 2);
            jsdocPosition += firstLine.length + 1; // + 1 for the splitted-out newline
            for (var _i = 0, otherLines_1 = otherLines; _i < otherLines_1.length; _i++) {
                var line = otherLines_1[_i];
                // regex is: start of string, followed by any amount of whitespace, followed by *,
                // followed by either a space or the end of the string
                var asteriskMatch = line.match(/^\s*\*( |$)/);
                if (asteriskMatch == null) {
                    this.addFailureAt(jsdocPosition, line.length, Rule.FORMAT_FAILURE_STRING);
                }
                var asteriskIndex = line.indexOf("*");
                if (asteriskIndex !== indexToMatch) {
                    this.addFailureAt(jsdocPosition, line.length, Rule.ALIGNMENT_FAILURE_STRING);
                }
                jsdocPosition += line.length + 1; // + 1 for the splitted-out newline
            }
            var lastLine = lines[lines.length - 1];
            // regex is: start of string, followed by any amount of whitespace, followed by */,
            // followed by the end of the string
            var endBlockCommentMatch = lastLine.match(/^\s*\*\/$/);
            if (endBlockCommentMatch == null) {
                this.addFailureAt(jsdocPosition, lastLine.length, Rule.FORMAT_FAILURE_STRING);
            }
            var lastAsteriskIndex = lastLine.indexOf("*");
            if (lastAsteriskIndex !== indexToMatch) {
                this.addFailureAt(jsdocPosition, lastLine.length, Rule.ALIGNMENT_FAILURE_STRING);
            }
        }
    };
    return JsdocWalker;
}(Lint.RuleWalker));
var _a;
