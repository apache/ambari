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
var utils_1 = require("../utils");
var OPTION_SPACE = "check-space";
var OPTION_LOWERCASE = "check-lowercase";
var OPTION_UPPERCASE = "check-uppercase";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new CommentWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "comment-format",
    description: "Enforces formatting rules for single-line comments.",
    rationale: "Helps maintain a consistent, readable style in your codebase.",
    optionsDescription: (_a = ["\n            Three arguments may be optionally provided:\n\n            * `\"check-space\"` requires that all single-line comments must begin with a space, as in `// comment`\n                * note that comments starting with `///` are also allowed, for things such as `///<reference>`\n            * `\"check-lowercase\"` requires that the first non-whitespace character of a comment must be lowercase, if applicable.\n            * `\"check-uppercase\"` requires that the first non-whitespace character of a comment must be uppercase, if applicable.\n            \n            Exceptions to `\"check-lowercase\"` or `\"check-uppercase\"` can be managed with object that may be passed as last argument.\n            \n            One of two options can be provided in this object:\n                \n                * `\"ignore-words\"`  - array of strings - words that will be ignored at the beginning of the comment.\n                * `\"ignore-pattern\"` - string - RegExp pattern that will be ignored at the beginning of the comment.\n            "], _a.raw = ["\n            Three arguments may be optionally provided:\n\n            * \\`\"check-space\"\\` requires that all single-line comments must begin with a space, as in \\`// comment\\`\n                * note that comments starting with \\`///\\` are also allowed, for things such as \\`///<reference>\\`\n            * \\`\"check-lowercase\"\\` requires that the first non-whitespace character of a comment must be lowercase, if applicable.\n            * \\`\"check-uppercase\"\\` requires that the first non-whitespace character of a comment must be uppercase, if applicable.\n            \n            Exceptions to \\`\"check-lowercase\"\\` or \\`\"check-uppercase\"\\` can be managed with object that may be passed as last argument.\n            \n            One of two options can be provided in this object:\n                \n                * \\`\"ignore-words\"\\`  - array of strings - words that will be ignored at the beginning of the comment.\n                * \\`\"ignore-pattern\"\\` - string - RegExp pattern that will be ignored at the beginning of the comment.\n            "], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            anyOf: [
                {
                    type: "string",
                    enum: [
                        "check-space",
                        "check-lowercase",
                        "check-uppercase",
                    ],
                },
                {
                    type: "object",
                    properties: {
                        "ignore-words": {
                            type: "array",
                            items: {
                                type: "string",
                            },
                        },
                        "ignore-pattern": {
                            type: "string",
                        },
                    },
                    minProperties: 1,
                    maxProperties: 1,
                },
            ],
        },
        minLength: 1,
        maxLength: 4,
    },
    optionExamples: [
        '[true, "check-space", "check-uppercase"]',
        '[true, "check-lowercase", {"ignore-words": ["TODO", "HACK"]}]',
        '[true, "check-lowercase", {"ignore-pattern": "STD\\w{2,3}\\b"}]',
    ],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.LOWERCASE_FAILURE = "comment must start with lowercase letter";
Rule.UPPERCASE_FAILURE = "comment must start with uppercase letter";
Rule.LEADING_SPACE_FAILURE = "comment must start with a space";
Rule.IGNORE_WORDS_FAILURE_FACTORY = function (words) { return " or the word(s): " + words.join(", "); };
Rule.IGNORE_PATTERN_FAILURE_FACTORY = function (pattern) { return " or its start must match the regex pattern \"" + pattern + "\""; };
exports.Rule = Rule;
var CommentWalker = (function (_super) {
    __extends(CommentWalker, _super);
    function CommentWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.failureIgnorePart = "";
        _this.exceptionsRegExp = _this.composeExceptionsRegExp();
        return _this;
    }
    CommentWalker.prototype.visitSourceFile = function (node) {
        var _this = this;
        utils.forEachComment(node, function (fullText, comment) {
            if (comment.kind === ts.SyntaxKind.SingleLineCommentTrivia) {
                var commentText = fullText.substring(comment.pos, comment.end);
                var startPosition = comment.pos + 2;
                var width = commentText.length - 2;
                if (_this.hasOption(OPTION_SPACE)) {
                    if (!startsWithSpace(commentText)) {
                        _this.addFailureAt(startPosition, width, Rule.LEADING_SPACE_FAILURE);
                    }
                }
                if (_this.hasOption(OPTION_LOWERCASE)) {
                    if (!startsWithLowercase(commentText) && !_this.startsWithException(commentText)) {
                        _this.addFailureAt(startPosition, width, Rule.LOWERCASE_FAILURE + _this.failureIgnorePart);
                    }
                }
                if (_this.hasOption(OPTION_UPPERCASE)) {
                    if (!startsWithUppercase(commentText) && !isEnableDisableFlag(commentText) && !_this.startsWithException(commentText)) {
                        _this.addFailureAt(startPosition, width, Rule.UPPERCASE_FAILURE + _this.failureIgnorePart);
                    }
                }
            }
        });
    };
    CommentWalker.prototype.startsWithException = function (commentText) {
        if (this.exceptionsRegExp == null) {
            return false;
        }
        return this.exceptionsRegExp.test(commentText);
    };
    CommentWalker.prototype.composeExceptionsRegExp = function () {
        var optionsList = this.getOptions();
        var exceptionsObject = optionsList[optionsList.length - 1];
        // early return if last element is string instead of exceptions object
        if (typeof exceptionsObject === "string" || !exceptionsObject) {
            return null;
        }
        if (exceptionsObject["ignore-pattern"]) {
            var ignorePattern = exceptionsObject["ignore-pattern"];
            this.failureIgnorePart = Rule.IGNORE_PATTERN_FAILURE_FACTORY(ignorePattern);
            // regex is "start of string"//"any amount of whitespace" followed by user provided ignore pattern
            return new RegExp("^//\\s*(" + ignorePattern + ")");
        }
        if (exceptionsObject["ignore-words"]) {
            var ignoreWords = exceptionsObject["ignore-words"];
            this.failureIgnorePart = Rule.IGNORE_WORDS_FAILURE_FACTORY(ignoreWords);
            // Converts all exceptions values to strings, trim whitespace, escapes RegExp special characters and combines into alternation
            var wordsPattern = ignoreWords
                .map(String)
                .map(function (str) { return str.trim(); })
                .map(utils_1.escapeRegExp)
                .join("|");
            // regex is "start of string"//"any amount of whitespace"("any word from ignore list") followed by non alphanumeric character
            return new RegExp("^//\\s*(" + wordsPattern + ")\\b");
        }
        return null;
    };
    return CommentWalker;
}(Lint.RuleWalker));
function startsWith(commentText, changeCase) {
    if (commentText.length <= 2) {
        return true; // comment is "//"? Technically not a violation.
    }
    // regex is "start of string"//"any amount of whitespace"("word character")
    var firstCharacterMatch = commentText.match(/^\/\/\s*(\w)/);
    if (firstCharacterMatch != null) {
        // the first group matched, i.e. the thing in the parens, is the first non-space character, if it's alphanumeric
        var firstCharacter = firstCharacterMatch[1];
        return firstCharacter === changeCase(firstCharacter);
    }
    else {
        // first character isn't alphanumeric/doesn't exist? Technically not a violation
        return true;
    }
}
function startsWithLowercase(commentText) {
    return startsWith(commentText, function (c) { return c.toLowerCase(); });
}
function startsWithUppercase(commentText) {
    return startsWith(commentText, function (c) { return c.toUpperCase(); });
}
function startsWithSpace(commentText) {
    if (commentText.length <= 2) {
        return true; // comment is "//"? Technically not a violation.
    }
    var commentBody = commentText.substring(2);
    // whitelist //#region and //#endregion
    if ((/^#(end)?region/).test(commentBody)) {
        return true;
    }
    // whitelist JetBrains IDEs' "//noinspection ..."
    if ((/^noinspection\s/).test(commentBody)) {
        return true;
    }
    var firstCharacter = commentBody.charAt(0);
    // three slashes (///) also works, to allow for ///<reference>
    return firstCharacter === " " || firstCharacter === "/";
}
function isEnableDisableFlag(commentText) {
    // regex is: start of string followed by "/*" or "//"
    // followed by any amount of whitespace followed by "tslint:"
    // followed by either "enable" or "disable"
    return /^(\/\*|\/\/)\s*tslint:(enable|disable)/.test(commentText);
}
var _a;
