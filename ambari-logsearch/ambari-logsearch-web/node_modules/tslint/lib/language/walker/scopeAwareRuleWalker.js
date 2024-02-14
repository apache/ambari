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
var utils_1 = require("../utils");
var ruleWalker_1 = require("./ruleWalker");
var ScopeAwareRuleWalker = (function (_super) {
    __extends(ScopeAwareRuleWalker, _super);
    function ScopeAwareRuleWalker(sourceFile, options) {
        var _this = _super.call(this, sourceFile, options) || this;
        // initialize with global scope if file is not a module
        _this.scopeStack = ts.isExternalModule(sourceFile) ? [] : [_this.createScope(sourceFile)];
        return _this;
    }
    ScopeAwareRuleWalker.prototype.getCurrentScope = function () {
        return this.scopeStack[this.scopeStack.length - 1];
    };
    // get all scopes available at this depth
    ScopeAwareRuleWalker.prototype.getAllScopes = function () {
        return this.scopeStack;
    };
    ScopeAwareRuleWalker.prototype.getCurrentDepth = function () {
        return this.scopeStack.length;
    };
    // callback notifier when a scope begins
    ScopeAwareRuleWalker.prototype.onScopeStart = function () {
        return;
    };
    // callback notifier when a scope ends
    ScopeAwareRuleWalker.prototype.onScopeEnd = function () {
        return;
    };
    ScopeAwareRuleWalker.prototype.visitNode = function (node) {
        var isNewScope = this.isScopeBoundary(node);
        if (isNewScope) {
            this.scopeStack.push(this.createScope(node));
            this.onScopeStart();
        }
        _super.prototype.visitNode.call(this, node);
        if (isNewScope) {
            this.onScopeEnd();
            this.scopeStack.pop();
        }
    };
    ScopeAwareRuleWalker.prototype.isScopeBoundary = function (node) {
        return utils_1.isScopeBoundary(node);
    };
    return ScopeAwareRuleWalker;
}(ruleWalker_1.RuleWalker));
exports.ScopeAwareRuleWalker = ScopeAwareRuleWalker;
