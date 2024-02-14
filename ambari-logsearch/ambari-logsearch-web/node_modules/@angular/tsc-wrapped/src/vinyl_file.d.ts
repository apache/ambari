/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
export interface VinylFile extends Object {
    path: string;
    contents: Buffer;
}
export declare function isVinylFile(obj: any): obj is VinylFile;
