/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
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
var walkContext_1 = require("./walkContext");
var AbstractWalker = (function (_super) {
    __extends(AbstractWalker, _super);
    function AbstractWalker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    AbstractWalker.prototype.getSourceFile = function () {
        return this.sourceFile;
    };
    AbstractWalker.prototype.getFailures = function () {
        return this.failures;
    };
    return AbstractWalker;
}(walkContext_1.WalkContext));
exports.AbstractWalker = AbstractWalker;
