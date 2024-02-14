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
var BANNED_KEYWORDS = ["any", "Number", "number", "String", "string", "Boolean", "boolean", "Undefined", "undefined"];
var OPTION_LEADING_UNDERSCORE = "allow-leading-underscore";
var OPTION_TRAILING_UNDERSCORE = "allow-trailing-underscore";
var OPTION_BAN_KEYWORDS = "ban-keywords";
var OPTION_CHECK_FORMAT = "check-format";
var OPTION_ALLOW_PASCAL_CASE = "allow-pascal-case";
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        var variableNameWalker = new VariableNameWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(variableNameWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "variable-name",
    description: "Checks variable names for various errors.",
    optionsDescription: (_a = ["\n            Five arguments may be optionally provided:\n\n            * `\"", "\"`: allows only camelCased or UPPER_CASED variable names\n              * `\"", "\"` allows underscores at the beginning (only has an effect if \"check-format\" specified)\n              * `\"", "\"` allows underscores at the end. (only has an effect if \"check-format\" specified)\n              * `\"", "\"` allows PascalCase in addition to camelCase.\n            * `\"", "\"`: disallows the use of certain TypeScript keywords (`any`, `Number`, `number`, `String`,\n            `string`, `Boolean`, `boolean`, `undefined`) as variable or parameter names."], _a.raw = ["\n            Five arguments may be optionally provided:\n\n            * \\`\"", "\"\\`: allows only camelCased or UPPER_CASED variable names\n              * \\`\"", "\"\\` allows underscores at the beginning (only has an effect if \"check-format\" specified)\n              * \\`\"", "\"\\` allows underscores at the end. (only has an effect if \"check-format\" specified)\n              * \\`\"", "\"\\` allows PascalCase in addition to camelCase.\n            * \\`\"", "\"\\`: disallows the use of certain TypeScript keywords (\\`any\\`, \\`Number\\`, \\`number\\`, \\`String\\`,\n            \\`string\\`, \\`Boolean\\`, \\`boolean\\`, \\`undefined\\`) as variable or parameter names."], Lint.Utils.dedent(_a, OPTION_CHECK_FORMAT, OPTION_LEADING_UNDERSCORE, OPTION_TRAILING_UNDERSCORE, OPTION_ALLOW_PASCAL_CASE, OPTION_BAN_KEYWORDS)),
    options: {
        type: "array",
        items: {
            type: "string",
            enum: [
                OPTION_CHECK_FORMAT,
                OPTION_LEADING_UNDERSCORE,
                OPTION_TRAILING_UNDERSCORE,
                OPTION_ALLOW_PASCAL_CASE,
                OPTION_BAN_KEYWORDS,
            ],
        },
        minLength: 0,
        maxLength: 5,
    },
    optionExamples: ['[true, "ban-keywords", "check-format", "allow-leading-underscore"]'],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FORMAT_FAILURE = "variable name must be in camelcase or uppercase";
Rule.KEYWORD_FAILURE = "variable name clashes with keyword/type";
exports.Rule = Rule;
var VariableNameWalker = (function (_super) {
    __extends(VariableNameWalker, _super);
    function VariableNameWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.shouldBanKeywords = _this.hasOption(OPTION_BAN_KEYWORDS);
        // check variable name formatting by default if no options are specified
        _this.shouldCheckFormat = !_this.shouldBanKeywords || _this.hasOption(OPTION_CHECK_FORMAT);
        return _this;
    }
    VariableNameWalker.prototype.visitBindingElement = function (node) {
        if (node.name.kind === ts.SyntaxKind.Identifier) {
            var identifier = node.name;
            this.handleVariableNameKeyword(identifier);
            // A destructuring pattern that does not rebind an expression is always an alias, e.g. `var {Foo} = ...;`.
            // Only check if the name is rebound (`var {Foo: bar} = ...;`).
            if (node.parent !== undefined && node.parent.kind !== ts.SyntaxKind.ObjectBindingPattern || node.propertyName) {
                this.handleVariableNameFormat(identifier, node.initializer);
            }
        }
        _super.prototype.visitBindingElement.call(this, node);
    };
    VariableNameWalker.prototype.visitParameterDeclaration = function (node) {
        if (node.name.kind === ts.SyntaxKind.Identifier) {
            var identifier = node.name;
            this.handleVariableNameFormat(identifier, undefined /* parameters may not alias */);
            this.handleVariableNameKeyword(identifier);
        }
        _super.prototype.visitParameterDeclaration.call(this, node);
    };
    VariableNameWalker.prototype.visitPropertyDeclaration = function (node) {
        if (node.name != null && node.name.kind === ts.SyntaxKind.Identifier) {
            var identifier = node.name;
            this.handleVariableNameFormat(identifier, node.initializer);
            // do not check property declarations for keywords, they are allowed to be keywords
        }
        _super.prototype.visitPropertyDeclaration.call(this, node);
    };
    VariableNameWalker.prototype.visitVariableDeclaration = function (node) {
        if (node.name.kind === ts.SyntaxKind.Identifier) {
            var identifier = node.name;
            this.handleVariableNameFormat(identifier, node.initializer);
            this.handleVariableNameKeyword(identifier);
        }
        _super.prototype.visitVariableDeclaration.call(this, node);
    };
    VariableNameWalker.prototype.visitVariableStatement = function (node) {
        // skip 'declare' keywords
        if (!Lint.hasModifier(node.modifiers, ts.SyntaxKind.DeclareKeyword)) {
            _super.prototype.visitVariableStatement.call(this, node);
        }
    };
    VariableNameWalker.prototype.isAlias = function (name, initializer) {
        if (initializer.kind === ts.SyntaxKind.PropertyAccessExpression) {
            return initializer.name.text === name.text;
        }
        else if (initializer.kind === ts.SyntaxKind.Identifier) {
            return initializer.text === name.text;
        }
        return false;
    };
    VariableNameWalker.prototype.handleVariableNameFormat = function (name, initializer) {
        var variableName = name.text;
        if (initializer && this.isAlias(name, initializer)) {
            return;
        }
        if (this.shouldCheckFormat && !this.isCamelCase(variableName) && !isUpperCase(variableName)) {
            this.addFailureAtNode(name, Rule.FORMAT_FAILURE);
        }
    };
    VariableNameWalker.prototype.handleVariableNameKeyword = function (name) {
        var variableName = name.text;
        if (this.shouldBanKeywords && BANNED_KEYWORDS.indexOf(variableName) !== -1) {
            this.addFailureAtNode(name, Rule.KEYWORD_FAILURE);
        }
    };
    VariableNameWalker.prototype.isCamelCase = function (name) {
        var firstCharacter = name.charAt(0);
        var lastCharacter = name.charAt(name.length - 1);
        var middle = name.substr(1, name.length - 2);
        if (name.length <= 0) {
            return true;
        }
        if (!this.hasOption(OPTION_LEADING_UNDERSCORE) && firstCharacter === "_") {
            return false;
        }
        if (!this.hasOption(OPTION_TRAILING_UNDERSCORE) && lastCharacter === "_") {
            return false;
        }
        if (!this.hasOption(OPTION_ALLOW_PASCAL_CASE) && !isLowerCase(firstCharacter)) {
            return false;
        }
        return middle.indexOf("_") === -1;
    };
    return VariableNameWalker;
}(Lint.RuleWalker));
function isLowerCase(name) {
    return name === name.toLowerCase();
}
function isUpperCase(name) {
    return name === name.toUpperCase();
}
var _a;
