/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { BaseException } from '../exception/exception';
export declare type SchematicPath = string & {
    __PRIVATE_SCHEMATIC_PATH: void;
};
export declare class InvalidPathException extends BaseException {
    constructor(path: string);
}
export declare function relativePath(from: SchematicPath, to: SchematicPath): SchematicPath;
export declare function normalizePath(path: string): SchematicPath;
