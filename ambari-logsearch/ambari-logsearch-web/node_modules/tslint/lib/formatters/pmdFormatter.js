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
var Utils = require("../utils");
var Formatter = (function (_super) {
    __extends(Formatter, _super);
    function Formatter() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Formatter.prototype.format = function (failures) {
        var output = "<pmd version=\"tslint\">";
        for (var _i = 0, failures_1 = failures; _i < failures_1.length; _i++) {
            var failure = failures_1[_i];
            var failureString = failure.getFailure()
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/'/g, "&#39;")
                .replace(/"/g, "&quot;");
            var lineAndCharacter = failure.getStartPosition().getLineAndCharacter();
            output += "<file name=\"" + failure.getFileName();
            output += "\"><violation begincolumn=\"" + (lineAndCharacter.character + 1);
            output += "\" beginline=\"" + (lineAndCharacter.line + 1);
            output += "\" priority=\"1\"";
            output += " rule=\"" + failureString + "\"> </violation></file>";
        }
        output += "</pmd>";
        return output;
    };
    return Formatter;
}(abstractFormatter_1.AbstractFormatter));
/* tslint:disable:object-literal-sort-keys */
Formatter.metadata = {
    formatterName: "pmd",
    description: "Formats errors as through they were PMD output.",
    descriptionDetails: "Imitates the XML output from PMD. All errors have a priority of 1.",
    sample: (_a = ["\n        <pmd version=\"tslint\">\n            <file name=\"myFile.ts\">\n                <violation begincolumn=\"14\" beginline=\"1\" priority=\"1\" rule=\"Missing semicolon\"></violation>\n            </file>\n        </pmd>"], _a.raw = ["\n        <pmd version=\"tslint\">\n            <file name=\"myFile.ts\">\n                <violation begincolumn=\"14\" beginline=\"1\" priority=\"1\" rule=\"Missing semicolon\"></violation>\n            </file>\n        </pmd>"], Utils.dedent(_a)),
    consumer: "machine",
};
exports.Formatter = Formatter;
var _a;
