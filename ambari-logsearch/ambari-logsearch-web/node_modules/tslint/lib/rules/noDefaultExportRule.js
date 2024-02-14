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
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NoDefaultExportWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-default-export",
    description: "Disallows default exports in ES6-style modules.",
    descriptionDetails: "Use named exports instead.",
    rationale: (_a = ["\n            Named imports/exports [promote clarity](https://github.com/palantir/tslint/issues/1182#issue-151780453).\n            In addition, current tooling differs on the correct way to handle default imports/exports.\n            Avoiding them all together can help avoid tooling bugs and conflicts."], _a.raw = ["\n            Named imports/exports [promote clarity](https://github.com/palantir/tslint/issues/1182#issue-151780453).\n            In addition, current tooling differs on the correct way to handle default imports/exports.\n            Avoiding them all together can help avoid tooling bugs and conflicts."], Lint.Utils.dedent(_a)),
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "maintainability",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Use of default exports is forbidden";
exports.Rule = Rule;
var NoDefaultExportWalker = (function (_super) {
    __extends(NoDefaultExportWalker, _super);
    function NoDefaultExportWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NoDefaultExportWalker.prototype.visitExportAssignment = function (node) {
        var exportMember = node.getChildAt(1);
        if (exportMember != null && exportMember.kind === ts.SyntaxKind.DefaultKeyword) {
            this.addFailureAtNode(exportMember, Rule.FAILURE_STRING);
        }
        _super.prototype.visitExportAssignment.call(this, node);
    };
    // inline class declaration and function declaration exports use modifiers
    NoDefaultExportWalker.prototype.visitNode = function (node) {
        if (node.kind === ts.SyntaxKind.DefaultKeyword && node.parent != null) {
            var nodes = node.parent.modifiers;
            if (nodes != null &&
                nodes.length === 2 &&
                nodes[0].kind === ts.SyntaxKind.ExportKeyword &&
                nodes[1].kind === ts.SyntaxKind.DefaultKeyword) {
                this.addFailureAtNode(nodes[1], Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitNode.call(this, node);
    };
    return NoDefaultExportWalker;
}(Lint.RuleWalker));
var _a;
