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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.isEnabled = function () {
        var ruleArguments = this.getOptions().ruleArguments;
        if (_super.prototype.isEnabled.call(this)) {
            var option = ruleArguments[0];
            if (typeof option === "number" && option > 0) {
                return true;
            }
        }
        return false;
    };
    Rule.prototype.apply = function (sourceFile) {
        var ruleFailures = [];
        var ruleArguments = this.getOptions().ruleArguments;
        var lineLimit = ruleArguments[0];
        var lineCount = sourceFile.getLineStarts().length;
        var disabledIntervals = this.getOptions().disabledIntervals;
        if (lineCount > lineLimit && disabledIntervals.length === 0) {
            var errorString = Rule.FAILURE_STRING_FACTORY(lineCount, lineLimit);
            ruleFailures.push(new Lint.RuleFailure(sourceFile, 0, 1, errorString, this.getOptions().ruleName));
        }
        return ruleFailures;
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "max-file-line-count",
    description: "Requires files to remain under a certain number of lines",
    rationale: (_a = ["\n            Limiting the number of lines allowed in a file allows files to remain small, \n            single purpose, and maintainable."], _a.raw = ["\n            Limiting the number of lines allowed in a file allows files to remain small, \n            single purpose, and maintainable."], Lint.Utils.dedent(_a)),
    optionsDescription: "An integer indicating the maximum number of lines.",
    options: {
        type: "number",
        minimum: "1",
    },
    optionExamples: ["[true, 300]"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (lineCount, lineLimit) {
    var msg = "This file has " + lineCount + " lines, which exceeds the maximum of " + lineLimit + " lines allowed. ";
    msg += "Consider breaking this file up into smaller parts";
    return msg;
};
exports.Rule = Rule;
var _a;
