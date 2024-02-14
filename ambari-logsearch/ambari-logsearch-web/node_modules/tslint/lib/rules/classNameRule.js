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
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new NameWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "class-name",
    description: "Enforces PascalCased class and interface names.",
    rationale: "Makes it easy to differentiate classes from regular variables at a glance.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "Class name must be in pascal case";
exports.Rule = Rule;
var NameWalker = (function (_super) {
    __extends(NameWalker, _super);
    function NameWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NameWalker.prototype.visitClassDeclaration = function (node) {
        // classes declared as default exports will be unnamed
        if (node.name != null) {
            var className = node.name.getText();
            if (!this.isPascalCased(className)) {
                this.addFailureAtNode(node.name, Rule.FAILURE_STRING);
            }
        }
        _super.prototype.visitClassDeclaration.call(this, node);
    };
    NameWalker.prototype.visitInterfaceDeclaration = function (node) {
        var interfaceName = node.name.getText();
        if (!this.isPascalCased(interfaceName)) {
            this.addFailureAtNode(node.name, Rule.FAILURE_STRING);
        }
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    NameWalker.prototype.isPascalCased = function (name) {
        if (name.length <= 0) {
            return true;
        }
        var firstCharacter = name.charAt(0);
        return ((firstCharacter === firstCharacter.toUpperCase()) && name.indexOf("_") === -1);
    };
    return NameWalker;
}(Lint.RuleWalker));
