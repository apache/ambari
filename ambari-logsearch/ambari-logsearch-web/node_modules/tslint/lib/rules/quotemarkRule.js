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
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING = function (actual, expected) {
        return actual + " should be " + expected;
    };
    Rule.prototype.isEnabled = function () {
        if (_super.prototype.isEnabled.call(this)) {
            var ruleArguments = this.getOptions().ruleArguments;
            var quoteMarkString = ruleArguments[0];
            return (quoteMarkString === "single" || quoteMarkString === "double");
        }
        return false;
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new QuotemarkWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "quotemark",
    description: "Requires single or double quotes for string literals.",
    hasFix: true,
    optionsDescription: (_a = ["\n            Five arguments may be optionally provided:\n\n            * `\"single\"` enforces single quotes.\n            * `\"double\"` enforces double quotes.\n            * `\"jsx-single\"` enforces single quotes for JSX attributes.\n            * `\"jsx-double\"` enforces double quotes for JSX attributes.\n            * `\"avoid-escape\"` allows you to use the \"other\" quotemark in cases where escaping would normally be required.\n            For example, `[true, \"double\", \"avoid-escape\"]` would not report a failure on the string literal `'Hello \"World\"'`."], _a.raw = ["\n            Five arguments may be optionally provided:\n\n            * \\`\"single\"\\` enforces single quotes.\n            * \\`\"double\"\\` enforces double quotes.\n            * \\`\"jsx-single\"\\` enforces single quotes for JSX attributes.\n            * \\`\"jsx-double\"\\` enforces double quotes for JSX attributes.\n            * \\`\"avoid-escape\"\\` allows you to use the \"other\" quotemark in cases where escaping would normally be required.\n            For example, \\`[true, \"double\", \"avoid-escape\"]\\` would not report a failure on the string literal \\`'Hello \"World\"'\\`."], Lint.Utils.dedent(_a)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: ["single", "double", "jsx-single", "jsx-double", "avoid-escape"],
        },
        minLength: 0,
        maxLength: 5,
    },
    optionExamples: ['[true, "single", "avoid-escape"]', '[true, "single", "jsx-double"]'],
    type: "style",
    typescriptOnly: false,
};
exports.Rule = Rule;
var QuotemarkWalker = (function (_super) {
    __extends(QuotemarkWalker, _super);
    function QuotemarkWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.quoteMark = _this.hasOption("single") ? "'" : '"';
        _this.jsxQuoteMark = _this.hasOption("jsx-single") ? "'" : _this.hasOption("jsx-double") ? '"' : _this.quoteMark;
        _this.avoidEscape = _this.hasOption("avoid-escape");
        return _this;
    }
    QuotemarkWalker.prototype.visitStringLiteral = function (node) {
        var expectedQuoteMark = node.parent.kind === ts.SyntaxKind.JsxAttribute ? this.jsxQuoteMark : this.quoteMark;
        var text = node.getText();
        var actualQuoteMark = text[0];
        if (actualQuoteMark !== expectedQuoteMark && !(this.avoidEscape && node.text.includes(expectedQuoteMark))) {
            var escapedText = text.slice(1, -1).replace(new RegExp(expectedQuoteMark, "g"), "\\" + expectedQuoteMark);
            var newText = expectedQuoteMark + escapedText + expectedQuoteMark;
            this.addFailureAtNode(node, Rule.FAILURE_STRING(actualQuoteMark, expectedQuoteMark), this.createFix(this.createReplacement(node.getStart(), node.getWidth(), newText)));
        }
    };
    return QuotemarkWalker;
}(Lint.RuleWalker));
var _a;
