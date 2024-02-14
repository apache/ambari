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
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.FAILURE_STRING_FACTORY = function (typeName, messageAddition) {
        return "Don't use '" + typeName + "' as a type." +
            (messageAddition ? " " + messageAddition : "");
    };
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new BanTypeWalker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "ban-types",
    description: (_a = ["\n            Bans specific types from being used. Does not ban the\n            corresponding runtime objects from being used."], _a.raw = ["\n            Bans specific types from being used. Does not ban the\n            corresponding runtime objects from being used."], Lint.Utils.dedent(_a)),
    options: {
        type: "list",
        listType: {
            type: "array",
            items: { type: "string" },
            minLength: 1,
            maxLength: 2,
        },
    },
    optionsDescription: (_b = ["\n            A list of `[\"regex\", \"optional explanation here\"]`, which bans\n            types that match `regex`"], _b.raw = ["\n            A list of \\`[\"regex\", \"optional explanation here\"]\\`, which bans\n            types that match \\`regex\\`"], Lint.Utils.dedent(_b)),
    optionExamples: ["[true, [\"Object\", \"Use {} instead.\"], [\"String\"]]"],
    type: "typescript",
    typescriptOnly: true,
};
exports.Rule = Rule;
var BanTypeWalker = (function (_super) {
    __extends(BanTypeWalker, _super);
    function BanTypeWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.bans = options.ruleArguments;
        return _this;
    }
    BanTypeWalker.prototype.visitTypeReference = function (node) {
        var typeName = node.typeName.getText();
        var ban = this.bans.find(function (_a) {
            var bannedType = _a[0];
            return typeName.match("^" + bannedType + "$") != null;
        });
        if (ban) {
            this.addFailure(this.createFailure(node.getStart(), node.getWidth(), Rule.FAILURE_STRING_FACTORY(typeName, ban[1])));
        }
        _super.prototype.visitTypeReference.call(this, node);
    };
    return BanTypeWalker;
}(Lint.RuleWalker));
var _a, _b;
