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
var abstractFormatter_1 = require("../language/formatter/abstractFormatter");
var colors = require("colors");
var Utils = require("../utils");
var Formatter = (function (_super) {
    __extends(Formatter, _super);
    function Formatter() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Formatter.prototype.format = function (failures) {
        if (typeof failures[0] === "undefined") {
            return "\n";
        }
        var outputLines = [];
        var positionMaxSize = this.getPositionMaxSize(failures);
        var ruleMaxSize = this.getRuleMaxSize(failures);
        var currentFile;
        for (var _i = 0, failures_1 = failures; _i < failures_1.length; _i++) {
            var failure = failures_1[_i];
            var fileName = failure.getFileName();
            // Output the name of each file once
            if (currentFile !== fileName) {
                outputLines.push("");
                outputLines.push(fileName);
                currentFile = fileName;
            }
            var failureString = failure.getFailure();
            failureString = colors.yellow(failureString);
            // Rule
            var ruleName = failure.getRuleName();
            ruleName = this.pad(ruleName, ruleMaxSize);
            ruleName = colors.grey(ruleName);
            // Lines
            var lineAndCharacter = failure.getStartPosition().getLineAndCharacter();
            var positionTuple = lineAndCharacter.line + 1 + ":" + (lineAndCharacter.character + 1);
            positionTuple = this.pad(positionTuple, positionMaxSize);
            positionTuple = colors.red(positionTuple);
            // Ouput
            var output = positionTuple + "  " + ruleName + "  " + failureString;
            outputLines.push(output);
        }
        // Removes initial blank line
        if (outputLines[0] === "") {
            outputLines.shift();
        }
        return outputLines.join("\n") + "\n\n";
    };
    Formatter.prototype.pad = function (str, len) {
        var padder = Array(len + 1).join(" ");
        return (str + padder).substring(0, padder.length);
    };
    Formatter.prototype.getPositionMaxSize = function (failures) {
        var positionMaxSize = 0;
        for (var _i = 0, failures_2 = failures; _i < failures_2.length; _i++) {
            var failure = failures_2[_i];
            var lineAndCharacter = failure.getStartPosition().getLineAndCharacter();
            var positionSize = (lineAndCharacter.line + 1 + ":" + (lineAndCharacter.character + 1)).length;
            if (positionSize > positionMaxSize) {
                positionMaxSize = positionSize;
            }
        }
        return positionMaxSize;
    };
    Formatter.prototype.getRuleMaxSize = function (failures) {
        var ruleMaxSize = 0;
        for (var _i = 0, failures_3 = failures; _i < failures_3.length; _i++) {
            var failure = failures_3[_i];
            var ruleSize = failure.getRuleName().length;
            if (ruleSize > ruleMaxSize) {
                ruleMaxSize = ruleSize;
            }
        }
        return ruleMaxSize;
    };
    return Formatter;
}(abstractFormatter_1.AbstractFormatter));
/* tslint:disable:object-literal-sort-keys */
Formatter.metadata = {
    formatterName: "stylish",
    description: "Human-readable formatter which creates stylish messages.",
    descriptionDetails: (_a = ["\n            The output matches that produced by eslint's stylish formatter. Its readability\n            enhanced through spacing and colouring"], _a.raw = ["\n            The output matches that produced by eslint's stylish formatter. Its readability\n            enhanced through spacing and colouring"], Utils.dedent(_a)),
    sample: (_b = ["\n        myFile.ts\n        1:14  semicolon  Missing semicolon"], _b.raw = ["\n        myFile.ts\n        1:14  semicolon  Missing semicolon"], Utils.dedent(_b)),
    consumer: "human",
};
exports.Formatter = Formatter;
var _a, _b;
