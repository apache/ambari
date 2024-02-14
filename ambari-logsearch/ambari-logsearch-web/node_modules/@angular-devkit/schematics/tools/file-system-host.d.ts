/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { FileSystemTreeHost } from '@angular-devkit/schematics';
export declare class FileSystemHost implements FileSystemTreeHost {
    private _root;
    constructor(_root: string);
    listDirectory(path: string): string[];
    isDirectory(path: string): boolean;
    readFile(path: string): Buffer;
    join(path1: string, path2: string): string;
}
