/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SchematicPath } from '../utility/path';
import { FileEntry } from './interface';
export declare class SimpleFileEntry implements FileEntry {
    private _path;
    private _content;
    constructor(_path: SchematicPath, _content: Buffer);
    readonly path: SchematicPath;
    readonly content: Buffer;
}
export declare class LazyFileEntry implements FileEntry {
    private _path;
    private _load;
    private _content;
    constructor(_path: SchematicPath, _load: (path?: SchematicPath) => Buffer);
    readonly path: SchematicPath;
    readonly content: Buffer;
}
