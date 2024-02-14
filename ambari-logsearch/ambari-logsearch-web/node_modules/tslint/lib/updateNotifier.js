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
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("./utils");
var updateNotifier = require("update-notifier");
function updateNotifierCheck() {
    try {
        var pkg = require("../package.json");
        // Check every 3 days for a new version
        var cacheTime = 1000 * 60 * 60 * 24 * 3;
        var changeLogUrl = "https://github.com/palantir/tslint/blob/master/CHANGELOG.md";
        var notifier = updateNotifier({
            pkg: pkg,
            updateCheckInterval: cacheTime,
        });
        if (notifier.notify && notifier.update) {
            notifier.notify({
                message: (_a = ["\n                    TSLint update available v", " \u2192 v", ".\n                    See ", ""], _a.raw = ["\n                    TSLint update available v", " \u2192 v", ".\n                    See ", ""], utils_1.dedent(_a, notifier.update.current, notifier.update.latest, changeLogUrl)),
            });
        }
    }
    catch (error) {
        // ignore error
    }
    var _a;
}
exports.updateNotifierCheck = updateNotifierCheck;
;
