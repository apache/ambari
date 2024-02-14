/**
 * @license
 * Copyright 2015 Palantir Technologies, Inc.
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
var ruleWalker_1 = require("./ruleWalker");
var SkippableTokenAwareRuleWalker = (function (_super) {
    __extends(SkippableTokenAwareRuleWalker, _super);
    function SkippableTokenAwareRuleWalker() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.tokensToSkipStartEndMap = new Map();
        return _this;
    }
    SkippableTokenAwareRuleWalker.prototype.visitRegularExpressionLiteral = function (node) {
        this.addTokenToSkipFromNode(node);
        _super.prototype.visitRegularExpressionLiteral.call(this, node);
    };
    SkippableTokenAwareRuleWalker.prototype.visitIdentifier = function (node) {
        this.addTokenToSkipFromNode(node);
        _super.prototype.visitIdentifier.call(this, node);
    };
    SkippableTokenAwareRuleWalker.prototype.visitTemplateExpression = function (node) {
        this.addTokenToSkipFromNode(node);
        _super.prototype.visitTemplateExpression.call(this, node);
    };
    SkippableTokenAwareRuleWalker.prototype.addTokenToSkipFromNode = function (node) {
        var start = node.getStart();
        var end = node.getEnd();
        if (start < end) {
            // only add to the map nodes whose end comes after their start, to prevent infinite loops
            this.tokensToSkipStartEndMap.set(start, end);
        }
    };
    SkippableTokenAwareRuleWalker.prototype.getSkipEndFromStart = function (start) {
        return this.tokensToSkipStartEndMap.get(start);
    };
    return SkippableTokenAwareRuleWalker;
}(ruleWalker_1.RuleWalker));
exports.SkippableTokenAwareRuleWalker = SkippableTokenAwareRuleWalker;
