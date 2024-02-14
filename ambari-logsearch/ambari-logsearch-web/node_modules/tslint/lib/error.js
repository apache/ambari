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
var shownWarnings = new Set();
/**
 * Used to exit the program and display a friendly message without the callstack.
 */
var FatalError = (function (_super) {
    __extends(FatalError, _super);
    function FatalError(message, innerError) {
        var _this = _super.call(this, message) || this;
        _this.message = message;
        _this.innerError = innerError;
        _this.name = FatalError.NAME;
        _this.stack = new Error().stack;
        return _this;
    }
    return FatalError;
}(Error));
FatalError.NAME = "FatalError";
exports.FatalError = FatalError;
function isError(possibleError) {
    return possibleError != null && possibleError.message !== undefined;
}
exports.isError = isError;
function showWarningOnce(message) {
    if (!shownWarnings.has(message)) {
        console.warn(message);
        shownWarnings.add(message);
    }
}
exports.showWarningOnce = showWarningOnce;
