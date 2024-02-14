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
var Formatter = (function (_super) {
    __extends(Formatter, _super);
    function Formatter() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /* tslint:enable:object-literal-sort-keys */
    Formatter.prototype.format = function (failures) {
        if (failures.length === 0) {
            return "";
        }
        var files = [];
        var currentFile;
        for (var _i = 0, failures_1 = failures; _i < failures_1.length; _i++) {
            var failure = failures_1[_i];
            var fileName = failure.getFileName();
            if (fileName !== currentFile) {
                files.push(fileName);
                currentFile = fileName;
            }
        }
        return files.join("\n") + "\n";
    };
    return Formatter;
}(abstractFormatter_1.AbstractFormatter));
/* tslint:disable:object-literal-sort-keys */
Formatter.metadata = {
    formatterName: "filesList",
    description: "Lists files containing lint errors.",
    sample: "directory/myFile.ts",
    consumer: "machine",
};
exports.Formatter = Formatter;
