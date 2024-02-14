/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * @whatItDoes Represents the version of Angular
 *
 * @stable
 */
export declare class Version {
    full: string;
    constructor(full: string);
    readonly major: string;
    readonly minor: string;
    readonly patch: string;
}
/**
 * @stable
 */
export declare const VERSION: Version;
