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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var walker = new FileHeaderWalker(sourceFile, this.getOptions());
        var options = this.getOptions().ruleArguments;
        walker.setRegexp(new RegExp(options[0].toString()));
        return this.applyWithWalker(walker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "file-header",
    description: "Enforces a certain header comment for all files, matched by a regular expression.",
    optionsDescription: "Regular expression to match the header.",
    options: {
        type: "string",
    },
    optionExamples: ['[true, "Copyright \\\\d{4}"]'],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "missing file header";
exports.Rule = Rule;
var FileHeaderWalker = (function (_super) {
    __extends(FileHeaderWalker, _super);
    function FileHeaderWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        // match a single line or multi line comment with leading whitespace
        // the wildcard dot does not match new lines - we can use [\s\S] instead
        _this.commentRegexp = /^\s*(\/\/(.*?)|\/\*([\s\S]*?)\*\/)/;
        return _this;
    }
    FileHeaderWalker.prototype.setRegexp = function (headerRegexp) {
        this.headerRegexp = headerRegexp;
    };
    FileHeaderWalker.prototype.visitSourceFile = function (node) {
        if (this.headerRegexp) {
            var text = node.getFullText();
            var offset = 0;
            // ignore shebang if it exists
            if (text.indexOf("#!") === 0) {
                offset = text.indexOf("\n") + 1;
                text = text.substring(offset);
            }
            // check for a comment
            var match = text.match(this.commentRegexp);
            if (!match) {
                this.addFailureAt(offset, 0, Rule.FAILURE_STRING);
            }
            else {
                // either the third or fourth capture group contains the comment contents
                var comment = match[2] ? match[2] : match[3];
                if (comment !== undefined && comment.search(this.headerRegexp) < 0) {
                    this.addFailureAt(offset, 0, Rule.FAILURE_STRING);
                }
            }
        }
    };
    return FileHeaderWalker;
}(Lint.RuleWalker));
