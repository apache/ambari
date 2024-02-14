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
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../utils");
var walker_1 = require("../walker");
var AbstractRule = (function () {
    function AbstractRule(ruleName, value, disabledIntervals) {
        this.ruleName = ruleName;
        this.value = value;
        this.disabledIntervals = disabledIntervals;
        if (Array.isArray(value) && value.length > 1) {
            this.ruleArguments = value.slice(1);
        }
        else {
            this.ruleArguments = [];
        }
    }
    AbstractRule.isRuleEnabled = function (ruleConfigValue) {
        if (typeof ruleConfigValue === "boolean") {
            return ruleConfigValue;
        }
        if (Array.isArray(ruleConfigValue) && ruleConfigValue.length > 0) {
            return ruleConfigValue[0];
        }
        return false;
    };
    AbstractRule.prototype.getOptions = function () {
        return {
            disabledIntervals: this.disabledIntervals,
            ruleArguments: this.ruleArguments,
            ruleName: this.ruleName,
        };
    };
    AbstractRule.prototype.applyWithWalker = function (walker) {
        walker.walk(walker.getSourceFile());
        return this.filterFailures(walker.getFailures());
    };
    AbstractRule.prototype.isEnabled = function () {
        return AbstractRule.isRuleEnabled(this.value);
    };
    AbstractRule.prototype.applyWithFunction = function (sourceFile, walkFn, options) {
        var ctx = new walker_1.WalkContext(sourceFile, this.ruleName, options);
        walkFn(ctx);
        return this.filterFailures(ctx.failures);
    };
    AbstractRule.prototype.filterFailures = function (failures) {
        var result = [];
        var _loop_1 = function (failure) {
            // don't add failures for a rule if the failure intersects an interval where that rule is disabled
            if (!utils_1.doesIntersect(failure, this_1.disabledIntervals) && !result.some(function (f) { return f.equals(failure); })) {
                result.push(failure);
            }
        };
        var this_1 = this;
        for (var _i = 0, failures_1 = failures; _i < failures_1.length; _i++) {
            var failure = failures_1[_i];
            _loop_1(failure);
        }
        return result;
    };
    return AbstractRule;
}());
exports.AbstractRule = AbstractRule;
