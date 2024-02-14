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
var OPTION_ALWAYS = "always-prefix";
var OPTION_NEVER = "never-prefix";
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
    ruleName: "interface-name",
    description: "Requires interface names to begin with a capital 'I'",
    rationale: "Makes it easy to differentiate interfaces from regular classes at a glance.",
    optionsDescription: (_a = ["\n            One of the following two options must be provided:\n\n            * `\"", "\"` requires interface names to start with an \"I\"\n            * `\"", "\"` requires interface names to not have an \"I\" prefix"], _a.raw = ["\n            One of the following two options must be provided:\n\n            * \\`\"", "\"\\` requires interface names to start with an \"I\"\n            * \\`\"", "\"\\` requires interface names to not have an \"I\" prefix"], Lint.Utils.dedent(_a, OPTION_ALWAYS, OPTION_NEVER)),
    options: {
        type: "string",
        enum: [OPTION_ALWAYS, OPTION_NEVER],
    },
    optionExamples: ["[true, \"" + OPTION_ALWAYS + "\"]", "[true, \"" + OPTION_NEVER + "\"]"],
    type: "style",
    typescriptOnly: true,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING = "interface name must start with a capitalized I";
Rule.FAILURE_STRING_NO_PREFIX = "interface name must not have an \"I\" prefix";
exports.Rule = Rule;
var NameWalker = (function (_super) {
    __extends(NameWalker, _super);
    function NameWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    NameWalker.prototype.visitInterfaceDeclaration = function (node) {
        var interfaceName = node.name.text;
        var always = this.hasOption(OPTION_ALWAYS) || (this.getOptions() && this.getOptions().length === 0);
        if (always) {
            if (!this.startsWithI(interfaceName)) {
                this.addFailureAtNode(node.name, Rule.FAILURE_STRING);
            }
        }
        else if (this.hasOption(OPTION_NEVER)) {
            if (this.hasPrefixI(interfaceName)) {
                this.addFailureAtNode(node.name, Rule.FAILURE_STRING_NO_PREFIX);
            }
        }
        _super.prototype.visitInterfaceDeclaration.call(this, node);
    };
    NameWalker.prototype.startsWithI = function (name) {
        if (name.length <= 0) {
            return true;
        }
        var firstCharacter = name.charAt(0);
        return (firstCharacter === "I");
    };
    NameWalker.prototype.hasPrefixI = function (name) {
        if (name.length <= 0) {
            return true;
        }
        var firstCharacter = name.charAt(0);
        if (firstCharacter !== "I") {
            return false;
        }
        var secondCharacter = name.charAt(1);
        if (secondCharacter === "") {
            return false;
        }
        else if (secondCharacter !== secondCharacter.toUpperCase()) {
            return false;
        }
        if (name.indexOf("IDB") === 0) {
            // IndexedDB
            return false;
        }
        return true;
    };
    return NameWalker;
}(Lint.RuleWalker));
var _a;
