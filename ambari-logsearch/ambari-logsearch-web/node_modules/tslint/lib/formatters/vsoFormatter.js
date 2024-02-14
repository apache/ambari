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
var abstractFormatter_1 = require("../language/formatter/abstractFormatter");
var Utils = require("../utils");
var Formatter = (function (_super) {
    __extends(Formatter, _super);
    function Formatter() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Formatter.prototype.format = function (failures) {
        var outputLines = failures.map(function (failure) {
            var fileName = failure.getFileName();
            var failureString = failure.getFailure();
            var lineAndCharacter = failure.getStartPosition().getLineAndCharacter();
            var line = lineAndCharacter.line + 1;
            var character = lineAndCharacter.character + 1;
            var code = (failure.getRuleName ? failure.getRuleName() : "");
            var properties = "sourcepath=" + fileName + ";linenumber=" + line + ";columnnumber=" + character + ";code=" + code + ";";
            return "##vso[task.logissue type=warning;" + properties + "]" + failureString;
        });
        return outputLines.join("\n") + "\n";
    };
    return Formatter;
}(abstractFormatter_1.AbstractFormatter));
/* tslint:disable:object-literal-sort-keys */
Formatter.metadata = {
    formatterName: "vso",
    description: "Formats output as VSO/TFS logging commands.",
    descriptionDetails: (_a = ["\n            Integrates with Visual Studio Online and Team Foundation Server by outputting errors\n            as 'warning' logging commands."], _a.raw = ["\n            Integrates with Visual Studio Online and Team Foundation Server by outputting errors\n            as 'warning' logging commands."], Utils.dedent(_a)),
    sample: "##vso[task.logissue type=warning;sourcepath=myFile.ts;linenumber=1;columnnumber=14;code=semicolon;]Missing semicolon",
    consumer: "machine",
};
exports.Formatter = Formatter;
var _a;
