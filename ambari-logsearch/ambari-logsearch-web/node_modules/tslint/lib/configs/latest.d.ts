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
export declare const rules: {
    "no-invalid-this": boolean;
    "no-angle-bracket-type-assertion": boolean;
    "only-arrow-functions": (string | boolean)[];
    "prefer-const": boolean;
    "callable-types": boolean;
    "interface-over-type-literal": boolean;
    "no-empty-interface": boolean;
    "no-string-throw": boolean;
    "import-spacing": boolean;
    "space-before-function-paren": (boolean | {
        "anonymous": string;
        "asyncArrow": string;
        "constructor": string;
        "method": string;
        "named": string;
    })[];
    "typeof-compare": boolean;
    "unified-signatures": boolean;
    "arrow-return-shorthand": boolean;
    "no-unnecessary-initializer": boolean;
    "no-misused-new": boolean;
    "ban-types": (boolean | string[])[];
    "no-duplicate-super": boolean;
};
declare const xtends = "tslint:recommended";
export { xtends as extends };
