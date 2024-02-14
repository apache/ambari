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
        return this.applyWithWalker(new MaxClassesPerFileWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "max-classes-per-file",
    description: (_a = ["\n            A file may not contain more than the specified number of classes"], _a.raw = ["\n            A file may not contain more than the specified number of classes"], Lint.Utils.dedent(_a)),
    rationale: (_b = ["\n            Ensures that files have a single responsibility so that that classes each exist in their own files"], _b.raw = ["\n            Ensures that files have a single responsibility so that that classes each exist in their own files"], Lint.Utils.dedent(_b)),
    optionsDescription: (_c = ["\n            The one required argument is an integer indicating the maximum number of classes that can appear in a file."], _c.raw = ["\n            The one required argument is an integer indicating the maximum number of classes that can appear in a file."], Lint.Utils.dedent(_c)),
    options: {
        type: "array",
        items: [
            {
                type: "number",
                minimum: 1,
            },
        ],
        additionalItems: false,
        minLength: 1,
        maxLength: 2,
    },
    optionExamples: ["[true, 1]", "[true, 5]"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_FACTORY = function (maxCount) {
    var maxClassWord = maxCount === 1 ? "class per file is" : "classes per file are";
    return "A maximum of " + maxCount + " " + maxClassWord + " allowed";
};
exports.Rule = Rule;
var MaxClassesPerFileWalker = (function (_super) {
    __extends(MaxClassesPerFileWalker, _super);
    function MaxClassesPerFileWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.classCount = 0;
        if (options.ruleArguments[0] === undefined
            || isNaN(options.ruleArguments[0])
            || options.ruleArguments[0] < 1) {
            _this.maxClassCount = 1;
        }
        else {
            _this.maxClassCount = options.ruleArguments[0];
        }
        return _this;
    }
    MaxClassesPerFileWalker.prototype.visitClassDeclaration = function (node) {
        this.increaseClassCount(node);
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    MaxClassesPerFileWalker.prototype.visitClassExpression = function (node) {
        this.increaseClassCount(node);
        _super.prototype.visitClassExpression.call(this, node);
    };
    MaxClassesPerFileWalker.prototype.increaseClassCount = function (node) {
        this.classCount++;
        if (this.classCount > this.maxClassCount) {
            var msg = Rule.FAILURE_STRING_FACTORY(this.maxClassCount);
            this.addFailureAtNode(node, msg);
        }
    };
    return MaxClassesPerFileWalker;
}(Lint.RuleWalker));
var _a, _b, _c;
