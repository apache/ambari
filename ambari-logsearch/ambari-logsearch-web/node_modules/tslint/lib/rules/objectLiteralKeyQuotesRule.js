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
        var objectLiteralKeyQuotesWalker = new ObjectLiteralKeyQuotesWalker(sourceFile, this.getOptions());
        return this.applyWithWalker(objectLiteralKeyQuotesWalker);
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "object-literal-key-quotes",
    description: "Enforces consistent object literal property quote style.",
    descriptionDetails: (_a = ["\n            Object literal property names can be defined in two ways: using literals or using strings.\n            For example, these two objects are equivalent:\n\n            var object1 = {\n                property: true\n            };\n\n            var object2 = {\n                \"property\": true\n            };\n\n            In many cases, it doesn\u2019t matter if you choose to use an identifier instead of a string\n            or vice-versa. Even so, you might decide to enforce a consistent style in your code.\n\n            This rules lets you enforce consistent quoting of property names. Either they should always\n            be quoted (default behavior) or quoted only as needed (\"as-needed\")."], _a.raw = ["\n            Object literal property names can be defined in two ways: using literals or using strings.\n            For example, these two objects are equivalent:\n\n            var object1 = {\n                property: true\n            };\n\n            var object2 = {\n                \"property\": true\n            };\n\n            In many cases, it doesn\u2019t matter if you choose to use an identifier instead of a string\n            or vice-versa. Even so, you might decide to enforce a consistent style in your code.\n\n            This rules lets you enforce consistent quoting of property names. Either they should always\n            be quoted (default behavior) or quoted only as needed (\"as-needed\")."], Lint.Utils.dedent(_a)),
    hasFix: true,
    optionsDescription: (_b = ["\n            Possible settings are:\n\n            * `\"always\"`: Property names should always be quoted. (This is the default.)\n            * `\"as-needed\"`: Only property names which require quotes may be quoted (e.g. those with spaces in them).\n            * `\"consistent\"`: Property names should either all be quoted or unquoted.\n            * `\"consistent-as-needed\"`: If any property name requires quotes, then all properties must be quoted. Otherwise, no\n            property names may be quoted.\n\n            For ES6, computed property names (`{[name]: value}`) and methods (`{foo() {}}`) never need\n            to be quoted."], _b.raw = ["\n            Possible settings are:\n\n            * \\`\"always\"\\`: Property names should always be quoted. (This is the default.)\n            * \\`\"as-needed\"\\`: Only property names which require quotes may be quoted (e.g. those with spaces in them).\n            * \\`\"consistent\"\\`: Property names should either all be quoted or unquoted.\n            * \\`\"consistent-as-needed\"\\`: If any property name requires quotes, then all properties must be quoted. Otherwise, no\n            property names may be quoted.\n\n            For ES6, computed property names (\\`{[name]: value}\\`) and methods (\\`{foo() {}}\\`) never need\n            to be quoted."], Lint.Utils.dedent(_b)),
    options: {
        type: "string",
        enum: ["always", "as-needed", "consistent", "consistent-as-needed"],
    },
    optionExamples: ["[true, \"as-needed\"]", "[true, \"always\"]"],
    type: "style",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.INCONSISTENT_PROPERTY = "All property names in this object literal must be consistently quoted or unquoted.";
Rule.UNNEEDED_QUOTES = function (name) {
    return "Unnecessarily quoted property '" + name + "' found.";
};
Rule.UNQUOTED_PROPERTY = function (name) {
    return "Unquoted property '" + name + "' found.";
};
exports.Rule = Rule;
var ObjectLiteralKeyQuotesWalker = (function (_super) {
    __extends(ObjectLiteralKeyQuotesWalker, _super);
    function ObjectLiteralKeyQuotesWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.mode = _this.getOptions()[0] || "always";
        return _this;
    }
    ObjectLiteralKeyQuotesWalker.prototype.visitObjectLiteralExpression = function (node) {
        var properties = node.properties.filter(function (_a) {
            var kind = _a.kind;
            return kind !== ts.SyntaxKind.ShorthandPropertyAssignment && kind !== ts.SyntaxKind.SpreadAssignment;
        });
        switch (this.mode) {
            case "always":
                this.allMustHaveQuotes(properties);
                break;
            case "as-needed":
                this.noneMayHaveQuotes(properties);
                break;
            case "consistent":
                if (quotesAreInconsistent(properties)) {
                    // No fix -- don't know if they would want to add quotes or remove them.
                    this.addFailureAt(node.getStart(), 1, Rule.INCONSISTENT_PROPERTY);
                }
                break;
            case "consistent-as-needed":
                if (properties.some(function (_a) {
                    var name = _a.name;
                    return name !== undefined
                        && name.kind === ts.SyntaxKind.StringLiteral && propertyNeedsQuotes(name.text);
                })) {
                    this.allMustHaveQuotes(properties);
                }
                else {
                    this.noneMayHaveQuotes(properties, true);
                }
                break;
            default:
                break;
        }
        _super.prototype.visitObjectLiteralExpression.call(this, node);
    };
    ObjectLiteralKeyQuotesWalker.prototype.allMustHaveQuotes = function (properties) {
        for (var _i = 0, properties_1 = properties; _i < properties_1.length; _i++) {
            var name = properties_1[_i].name;
            if (name !== undefined && name.kind !== ts.SyntaxKind.StringLiteral && name.kind !== ts.SyntaxKind.ComputedPropertyName) {
                var fix = this.createFix(this.appendText(name.getStart(), '"'), this.appendText(name.getEnd(), '"'));
                this.addFailureAtNode(name, Rule.UNQUOTED_PROPERTY(name.getText()), fix);
            }
        }
    };
    ObjectLiteralKeyQuotesWalker.prototype.noneMayHaveQuotes = function (properties, noneNeedQuotes) {
        for (var _i = 0, properties_2 = properties; _i < properties_2.length; _i++) {
            var name = properties_2[_i].name;
            if (name !== undefined && name.kind === ts.SyntaxKind.StringLiteral && (noneNeedQuotes || !propertyNeedsQuotes(name.text))) {
                var fix = this.createFix(this.deleteText(name.getStart(), 1), this.deleteText(name.getEnd() - 1, 1));
                this.addFailureAtNode(name, Rule.UNNEEDED_QUOTES(name.text), fix);
            }
        }
    };
    return ObjectLiteralKeyQuotesWalker;
}(Lint.RuleWalker));
function quotesAreInconsistent(properties) {
    var propertiesAreQuoted; // inferred on first (non-computed) property
    for (var _i = 0, properties_3 = properties; _i < properties_3.length; _i++) {
        var name = properties_3[_i].name;
        if (name === undefined || name.kind === ts.SyntaxKind.ComputedPropertyName) {
            continue;
        }
        var thisOneIsQuoted = name.kind === ts.SyntaxKind.StringLiteral;
        if (propertiesAreQuoted === undefined) {
            propertiesAreQuoted = thisOneIsQuoted;
        }
        else if (propertiesAreQuoted !== thisOneIsQuoted) {
            return true;
        }
    }
    return false;
}
function propertyNeedsQuotes(property) {
    return !IDENTIFIER_NAME_REGEX.test(property) && (Number(property).toString() !== property || property.startsWith("-"));
}
// This is simplistic. See https://mothereff.in/js-properties for the gorey details.
var IDENTIFIER_NAME_REGEX = /^(?:[\$A-Z_a-z])+$/;
var _a, _b;
