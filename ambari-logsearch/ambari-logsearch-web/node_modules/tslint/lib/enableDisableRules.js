/**
 * @license
 * Copyright 2014 Palantir Technologies, Inc.
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
var utils = require("tsutils");
var ts = require("typescript");
var abstractRule_1 = require("./language/rule/abstractRule");
var EnableDisableRulesWalker = (function () {
    function EnableDisableRulesWalker(sourceFile, rules) {
        this.sourceFile = sourceFile;
        this.enableDisableRuleMap = {};
        this.enabledRules = [];
        if (rules) {
            for (var _i = 0, _a = Object.keys(rules); _i < _a.length; _i++) {
                var rule = _a[_i];
                if (abstractRule_1.AbstractRule.isRuleEnabled(rules[rule])) {
                    this.enabledRules.push(rule);
                    this.enableDisableRuleMap[rule] = [{
                            isEnabled: true,
                            position: 0,
                        }];
                }
            }
        }
    }
    EnableDisableRulesWalker.prototype.getEnableDisableRuleMap = function () {
        var _this = this;
        utils.forEachComment(this.sourceFile, function (fullText, comment) {
            var commentText = comment.kind === ts.SyntaxKind.SingleLineCommentTrivia
                ? fullText.substring(comment.pos + 2, comment.end)
                : fullText.substring(comment.pos + 2, comment.end - 2);
            return _this.handleComment(commentText, comment);
        });
        return this.enableDisableRuleMap;
    };
    EnableDisableRulesWalker.prototype.getStartOfLinePosition = function (position, lineOffset) {
        if (lineOffset === void 0) { lineOffset = 0; }
        var line = ts.getLineAndCharacterOfPosition(this.sourceFile, position).line + lineOffset;
        var lineStarts = this.sourceFile.getLineStarts();
        if (line >= lineStarts.length) {
            // next line ends with eof or there is no next line
            // undefined switches the rule until the end and avoids an extra array entry
            return undefined;
        }
        return lineStarts[line];
    };
    EnableDisableRulesWalker.prototype.switchRuleState = function (ruleName, isEnabled, start, end) {
        var ruleStateMap = this.enableDisableRuleMap[ruleName];
        if (ruleStateMap === undefined ||
            isEnabled === ruleStateMap[ruleStateMap.length - 1].isEnabled // no need to add switch points if there is no change
        ) {
            return;
        }
        ruleStateMap.push({
            isEnabled: isEnabled,
            position: start,
        });
        if (end) {
            // we only get here when rule state changes therefore we can safely use opposite state
            ruleStateMap.push({
                isEnabled: !isEnabled,
                position: end,
            });
        }
    };
    EnableDisableRulesWalker.prototype.handleComment = function (commentText, range) {
        // regex is: start of string followed by any amount of whitespace
        // followed by tslint and colon
        // followed by either "enable" or "disable"
        // followed optionally by -line or -next-line
        // followed by either colon, whitespace or end of string
        var match = /^\s*tslint:(enable|disable)(?:-(line|next-line))?(:|\s|$)/.exec(commentText);
        if (match !== null) {
            // remove everything matched by the previous regex to get only the specified rules
            // split at whitespaces
            // filter empty items coming from whitespaces at start, at end or empty list
            var rulesList = commentText.substr(match[0].length)
                .split(/\s+/)
                .filter(function (rule) { return !!rule; });
            if (rulesList.length === 0 && match[3] === ":") {
                // nothing to do here: an explicit separator was specified but no rules to switch
                return;
            }
            if (rulesList.length === 0 ||
                rulesList.indexOf("all") !== -1) {
                // if list is empty we default to all enabled rules
                // if `all` is specified we ignore the other rules and take all enabled rules
                rulesList = this.enabledRules;
            }
            this.handleTslintLineSwitch(rulesList, match[1] === "enable", match[2], range);
        }
    };
    EnableDisableRulesWalker.prototype.handleTslintLineSwitch = function (rules, isEnabled, modifier, range) {
        var start;
        var end;
        if (modifier === "line") {
            // start at the beginning of the line where comment starts
            start = this.getStartOfLinePosition(range.pos);
            // end at the beginning of the line following the comment
            end = this.getStartOfLinePosition(range.end, 1);
        }
        else if (modifier === "next-line") {
            // start at the beginning of the line following the comment
            start = this.getStartOfLinePosition(range.end, 1);
            if (start === undefined) {
                // no need to switch anything, there is no next line
                return;
            }
            // end at the beginning of the line following the next line
            end = this.getStartOfLinePosition(range.end, 2);
        }
        else {
            // switch rule for the rest of the file
            // start at the current position, but skip end position
            start = range.pos;
            end = undefined;
        }
        for (var _i = 0, rules_1 = rules; _i < rules_1.length; _i++) {
            var ruleToSwitch = rules_1[_i];
            this.switchRuleState(ruleToSwitch, isEnabled, start, end);
        }
    };
    return EnableDisableRulesWalker;
}());
exports.EnableDisableRulesWalker = EnableDisableRulesWalker;
