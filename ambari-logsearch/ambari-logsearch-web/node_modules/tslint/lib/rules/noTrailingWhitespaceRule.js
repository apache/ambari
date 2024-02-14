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
var OPTION_IGNORE_COMMENTS = "ignore-comments";
var OPTION_IGNORE_JSDOC = "ignore-jsdoc";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var option = 0 /* None */;
        if (this.ruleArguments.indexOf(OPTION_IGNORE_COMMENTS) !== -1) {
            option = 1 /* Comments */;
        }
        else if (this.ruleArguments.indexOf(OPTION_IGNORE_JSDOC) !== -1) {
            option = 2 /* JsDoc */;
        }
        return this.applyWithFunction(sourceFile, walk, option);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-trailing-whitespace",
    description: "Disallows trailing whitespace at the end of a line.",
    rationale: "Keeps version control diffs clean as it prevents accidental whitespace from being committed.",
    optionsDescription: (_a = ["\n            Possible settings are:\n\n            * `\"", "\"`: Allows trailing whitespace in comments.\n            * `\"", "\"`: Allows trailing whitespace only in JSDoc comments."], _a.raw = ["\n            Possible settings are:\n\n            * \\`\"", "\"\\`: Allows trailing whitespace in comments.\n            * \\`\"", "\"\\`: Allows trailing whitespace only in JSDoc comments."], Lint.Utils.dedent(_a, OPTION_IGNORE_COMMENTS, OPTION_IGNORE_JSDOC)),
    hasFix: true,
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [OPTION_IGNORE_COMMENTS, OPTION_IGNORE_JSDOC],
        },
    },
    optionExamples: [
        "true",
        "[true, \"" + OPTION_IGNORE_COMMENTS + "\"]",
        "[true, \"" + OPTION_IGNORE_JSDOC + "\"]",
    ],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "trailing whitespace";
exports.Rule = Rule;
function walk(ctx) {
    var lastSeenWasWhitespace = false;
    var lastSeenWhitespacePosition = 0;
    utils.forEachTokenWithTrivia(ctx.sourceFile, function (fullText, kind, range) {
        if (kind === ts.SyntaxKind.NewLineTrivia || kind === ts.SyntaxKind.EndOfFileToken) {
            if (lastSeenWasWhitespace) {
                reportFailure(ctx, lastSeenWhitespacePosition, range.pos);
            }
            lastSeenWasWhitespace = false;
        }
        else if (kind === ts.SyntaxKind.WhitespaceTrivia) {
            lastSeenWasWhitespace = true;
            lastSeenWhitespacePosition = range.pos;
        }
        else {
            if (ctx.options !== 1 /* Comments */) {
                if (kind === ts.SyntaxKind.SingleLineCommentTrivia) {
                    var commentText = fullText.substring(range.pos + 2, range.end);
                    var match = /\s+$/.exec(commentText);
                    if (match !== null) {
                        reportFailure(ctx, range.end - match[0].length, range.end);
                    }
                }
                else if (kind === ts.SyntaxKind.MultiLineCommentTrivia &&
                    (ctx.options !== 2 /* JsDoc */ ||
                        fullText[range.pos + 2] !== "*" ||
                        fullText[range.pos + 3] === "*")) {
                    var startPos = range.pos + 2;
                    var commentText = fullText.substring(startPos, range.end - 2);
                    var lines = commentText.split("\n");
                    // we don't want to check the content of the last comment line, as it is always followed by */
                    var len = lines.length - 1;
                    for (var i = 0; i < len; ++i) {
                        var line = lines[i];
                        // remove carriage return at the end, it is does not account to trailing whitespace
                        if (line.endsWith("\r")) {
                            line = line.substr(0, line.length - 1);
                        }
                        var start = line.search(/\s+$/);
                        if (start !== -1) {
                            reportFailure(ctx, startPos + start, startPos + line.length);
                        }
                        startPos += lines[i].length + 1;
                    }
                }
            }
            lastSeenWasWhitespace = false;
        }
    });
}
function reportFailure(ctx, start, end) {
    ctx.addFailure(start, end, Rule.FAILURE_STRING, ctx.createFix(Lint.Replacement.deleteFromTo(start, end)));
}
var _a;
