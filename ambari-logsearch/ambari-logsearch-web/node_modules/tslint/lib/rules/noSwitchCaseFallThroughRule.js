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
        return this.applyWithWalker(new NoSwitchCaseFallThroughWalker(sourceFile, this.ruleName, undefined));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-switch-case-fall-through",
    description: "Disallows falling through case statements.",
    descriptionDetails: (_a = ["\n            For example, the following is not allowed:\n\n            ```ts\n            switch(foo) {\n                case 1:\n                    someFunc(foo);\n                case 2:\n                    someOtherFunc(foo);\n            }\n            ```\n\n            However, fall through is allowed when case statements are consecutive or\n            a magic `/* falls through */` comment is present. The following is valid:\n\n            ```ts\n            switch(foo) {\n                case 1:\n                    someFunc(foo);\n                    /* falls through */\n                case 2:\n                case 3:\n                    someOtherFunc(foo);\n            }\n            ```"], _a.raw = ["\n            For example, the following is not allowed:\n\n            \\`\\`\\`ts\n            switch(foo) {\n                case 1:\n                    someFunc(foo);\n                case 2:\n                    someOtherFunc(foo);\n            }\n            \\`\\`\\`\n\n            However, fall through is allowed when case statements are consecutive or\n            a magic \\`/* falls through */\\` comment is present. The following is valid:\n\n            \\`\\`\\`ts\n            switch(foo) {\n                case 1:\n                    someFunc(foo);\n                    /* falls through */\n                case 2:\n                case 3:\n                    someOtherFunc(foo);\n            }\n            \\`\\`\\`"], Lint.Utils.dedent(_a)),
    rationale: "Fall though in switch statements is often unintentional and a bug.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_PART = "expected a 'break' before ";
exports.Rule = Rule;
var NoSwitchCaseFallThroughWalker = (function (_super) {
    __extends(NoSwitchCaseFallThroughWalker, _super);
    function NoSwitchCaseFallThroughWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoSwitchCaseFallThroughWalker.prototype.walk = function (sourceFile) {
        var _this = this;
        var cb = function (node) {
            if (node.kind === ts.SyntaxKind.SwitchStatement) {
                _this.visitSwitchStatement(node);
            }
            return ts.forEachChild(node, cb);
        };
        return ts.forEachChild(sourceFile, cb);
    };
    NoSwitchCaseFallThroughWalker.prototype.visitSwitchStatement = function (node) {
        var clauses = node.caseBlock.clauses;
        var len = clauses.length - 1; // last clause doesn't need to be checked
        for (var i = 0; i < len; ++i) {
            if (clauses[i].statements.length !== 0 &&
                // TODO type assertion can be removed with typescript 2.2
                !utils.endsControlFlow(clauses[i]) &&
                !this.isFallThroughAllowed(clauses[i])) {
                this.reportError(clauses[i + 1]);
            }
        }
    };
    NoSwitchCaseFallThroughWalker.prototype.isFallThroughAllowed = function (clause) {
        var sourceFileText = this.sourceFile.text;
        var comments = ts.getLeadingCommentRanges(sourceFileText, clause.end);
        if (comments === undefined) {
            return false;
        }
        for (var _i = 0, comments_1 = comments; _i < comments_1.length; _i++) {
            var comment = comments_1[_i];
            var commentText = void 0;
            if (comment.kind === ts.SyntaxKind.MultiLineCommentTrivia) {
                commentText = sourceFileText.substring(comment.pos + 2, comment.end - 2);
            }
            else {
                commentText = sourceFileText.substring(comment.pos + 2, comment.end);
            }
            if (commentText.trim() === "falls through") {
                return true;
            }
        }
        return false;
    };
    NoSwitchCaseFallThroughWalker.prototype.reportError = function (clause) {
        var keyword = clause.kind === ts.SyntaxKind.CaseClause ? "case" : "default";
        this.addFailureAt(clause.getStart(this.sourceFile), keyword.length, Rule.FAILURE_STRING_PART + "'" + keyword + "'");
    };
    return NoSwitchCaseFallThroughWalker;
}(Lint.AbstractWalker));
exports.NoSwitchCaseFallThroughWalker = NoSwitchCaseFallThroughWalker;
var _a;
