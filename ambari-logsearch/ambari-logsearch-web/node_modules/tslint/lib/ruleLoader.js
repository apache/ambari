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
var fs = require("fs");
var path = require("path");
var configuration_1 = require("./configuration");
var error_1 = require("./error");
var abstractRule_1 = require("./language/rule/abstractRule");
var utils_1 = require("./utils");
var moduleDirectory = path.dirname(module.filename);
var CORE_RULES_DIRECTORY = path.resolve(moduleDirectory, ".", "rules");
var cachedRules = new Map(); // null indicates that the rule was not found
function loadRules(ruleConfiguration, enableDisableRuleMap, rulesDirectories, isJs) {
    var rules = [];
    var notFoundRules = [];
    var notAllowedInJsRules = [];
    for (var ruleName in ruleConfiguration) {
        if (ruleConfiguration.hasOwnProperty(ruleName)) {
            var ruleValue = ruleConfiguration[ruleName];
            if (abstractRule_1.AbstractRule.isRuleEnabled(ruleValue) || enableDisableRuleMap.hasOwnProperty(ruleName)) {
                var Rule = findRule(ruleName, rulesDirectories);
                if (Rule == null) {
                    notFoundRules.push(ruleName);
                }
                else {
                    if (isJs && Rule.metadata && Rule.metadata.typescriptOnly != null && Rule.metadata.typescriptOnly) {
                        notAllowedInJsRules.push(ruleName);
                    }
                    else {
                        var ruleSpecificList = (ruleName in enableDisableRuleMap ? enableDisableRuleMap[ruleName] : []);
                        var disabledIntervals = buildDisabledIntervalsFromSwitches(ruleSpecificList);
                        rules.push(new Rule(ruleName, ruleValue, disabledIntervals));
                        if (Rule.metadata && Rule.metadata.deprecationMessage) {
                            error_1.showWarningOnce(Rule.metadata.ruleName + " is deprecated. " + Rule.metadata.deprecationMessage);
                        }
                    }
                }
            }
        }
    }
    if (notFoundRules.length > 0) {
        var warning = (_a = ["\n            Could not find implementations for the following rules specified in the configuration:\n                ", "\n            Try upgrading TSLint and/or ensuring that you have all necessary custom rules installed.\n            If TSLint was recently upgraded, you may have old rules configured which need to be cleaned up.\n        "], _a.raw = ["\n            Could not find implementations for the following rules specified in the configuration:\n                ", "\n            Try upgrading TSLint and/or ensuring that you have all necessary custom rules installed.\n            If TSLint was recently upgraded, you may have old rules configured which need to be cleaned up.\n        "], utils_1.dedent(_a, notFoundRules.join("\n                ")));
        console.warn(warning);
    }
    if (notAllowedInJsRules.length > 0) {
        var warning = (_b = ["\n            Following rules specified in configuration couldn't be applied to .js or .jsx files:\n                ", "\n            Make sure to exclude them from \"jsRules\" section of your tslint.json.\n        "], _b.raw = ["\n            Following rules specified in configuration couldn't be applied to .js or .jsx files:\n                ", "\n            Make sure to exclude them from \"jsRules\" section of your tslint.json.\n        "], utils_1.dedent(_b, notAllowedInJsRules.join("\n                ")));
        console.warn(warning);
    }
    if (rules.length === 0) {
        console.warn("No valid rules have been specified");
    }
    return rules;
    var _a, _b;
}
exports.loadRules = loadRules;
function findRule(name, rulesDirectories) {
    var camelizedName = transformName(name);
    var Rule;
    // first check for core rules
    Rule = loadCachedRule(CORE_RULES_DIRECTORY, camelizedName);
    if (Rule == null) {
        // then check for rules within the first level of rulesDirectory
        for (var _i = 0, _a = utils_1.arrayify(rulesDirectories); _i < _a.length; _i++) {
            var dir = _a[_i];
            Rule = loadCachedRule(dir, camelizedName, true);
            if (Rule != null) {
                break;
            }
        }
    }
    return Rule;
}
exports.findRule = findRule;
function transformName(name) {
    // camelize strips out leading and trailing underscores and dashes, so make sure they aren't passed to camelize
    // the regex matches the groups (leading underscores and dashes)(other characters)(trailing underscores and dashes)
    var nameMatch = name.match(/^([-_]*)(.*?)([-_]*)$/);
    if (nameMatch == null) {
        return name + "Rule";
    }
    return nameMatch[1] + utils_1.camelize(nameMatch[2]) + nameMatch[3] + "Rule";
}
/**
 * @param directory - An absolute path to a directory of rules
 * @param ruleName - A name of a rule in filename format. ex) "someLintRule"
 */
function loadRule(directory, ruleName) {
    var fullPath = path.join(directory, ruleName);
    if (fs.existsSync(fullPath + ".js")) {
        var ruleModule = require(fullPath);
        if (ruleModule && ruleModule.Rule) {
            return ruleModule.Rule;
        }
    }
    return undefined;
}
function loadCachedRule(directory, ruleName, isCustomPath) {
    if (isCustomPath === void 0) { isCustomPath = false; }
    // use cached value if available
    var fullPath = path.join(directory, ruleName);
    var cachedRule = cachedRules.get(fullPath);
    if (cachedRule !== undefined) {
        return cachedRule;
    }
    // get absolute path
    var absolutePath = directory;
    if (isCustomPath) {
        absolutePath = configuration_1.getRelativePath(directory);
        if (absolutePath != null) {
            if (!fs.existsSync(absolutePath)) {
                throw new Error("Could not find custom rule directory: " + directory);
            }
        }
    }
    var Rule = null;
    if (absolutePath != null) {
        Rule = loadRule(absolutePath, ruleName);
    }
    cachedRules.set(fullPath, Rule);
    return Rule;
}
/**
 * creates disabled intervals for rule based on list of switchers for it
 * @param ruleSpecificList - contains all switchers for rule states sorted top-down and strictly alternating between enabled and disabled
 */
function buildDisabledIntervalsFromSwitches(ruleSpecificList) {
    var disabledIntervalList = [];
    // starting from second element in the list since first is always enabled in position 0;
    var i = 1;
    while (i < ruleSpecificList.length) {
        var startPosition = ruleSpecificList[i].position;
        // rule enabled state is always alternating therefore we can use position of next switch as end of disabled interval
        // set endPosition as Infinity in case when last switch for rule in a file is disabled
        var endPosition = ruleSpecificList[i + 1] ? ruleSpecificList[i + 1].position : Infinity;
        disabledIntervalList.push({
            endPosition: endPosition,
            startPosition: startPosition,
        });
        i += 2;
    }
    return disabledIntervalList;
}
