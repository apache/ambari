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
var BanRule = require("./banRule");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Rule.prototype.apply = function (sourceFile) {
        var options = this.getOptions();
        var consoleBanWalker = new BanRule.BanFunctionWalker(sourceFile, this.getOptions());
        for (var _i = 0, _a = options.ruleArguments; _i < _a.length; _i++) {
            var option = _a[_i];
            consoleBanWalker.addBannedFunction(["console", option]);
        }
        return this.applyWithWalker(consoleBanWalker);
    };
    return Rule;
}(BanRule.Rule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-console",
    description: "Bans the use of specified `console` methods.",
    rationale: "In general, \`console\` methods aren't appropriate for production code.",
    optionsDescription: "A list of method names to ban.",
    options: {
        type: "array",
        items: { type: "string" },
    },
    optionExamples: ["[true, \"log\", \"error\"]"],
    type: "functionality",
    typescriptOnly: false,
};
exports.Rule = Rule;
