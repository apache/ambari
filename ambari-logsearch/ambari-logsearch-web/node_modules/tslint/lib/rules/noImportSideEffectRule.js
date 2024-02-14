/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
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
var Lint = require("tslint");
var OPTION_IGNORE_MODULE = "ignore-module";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoImportSideEffectWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
Rule.metadata = {
    description: "Avoid import statements with side-effect.",
    optionExamples: ["true", "[true, { \"" + OPTION_IGNORE_MODULE + "\": \"(\\.html|\\.css)$\" }]"],
    options: {
        items: {
            properties: {
                "ignore-module": {
                    type: "string",
                },
            },
            type: "object",
        },
        maxLength: 1,
        minLength: 0,
        type: "array",
    },
    optionsDescription: (_a = ["\n            One argument may be optionally provided:\n\n            * `", "` allows to specify a regex and ignore modules which it matches."], _a.raw = ["\n            One argument may be optionally provided:\n\n            * \\`", "\\` allows to specify a regex and ignore modules which it matches."], Lint.Utils.dedent(_a, OPTION_IGNORE_MODULE)),
    rationale: "Imports with side effects may have behavior which is hard for static verification.",
    ruleName: "no-import-side-effect",
    type: "typescript",
    typescriptOnly: false,
};
Rule.FAILURE_STRING = "import with explicit side-effect";
exports.Rule = Rule;
var NoImportSideEffectWalker = (function (_super) {
    __extends(NoImportSideEffectWalker, _super);
    function NoImportSideEffectWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        var patternConfig = _this.getOptions().pop();
        _this.ignorePattern = patternConfig ? new RegExp(patternConfig[OPTION_IGNORE_MODULE]) : null;
        _this.scanner = ts.createScanner(ts.ScriptTarget.ES5, false, ts.LanguageVariant.Standard, sourceFile.text);
        return _this;
    }
    NoImportSideEffectWalker.prototype.visitImportDeclaration = function (node) {
        var importClause = node.importClause;
        if (importClause === undefined) {
            var specifier = node.moduleSpecifier.getText();
            if (this.ignorePattern === null || !this.ignorePattern.test(specifier.substring(1, specifier.length - 1))) {
                this.addFailureAtNode(node, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitImportDeclaration.call(this, node);
    };
    return NoImportSideEffectWalker;
}(Lint.SkippableTokenAwareRuleWalker));
var _a;
