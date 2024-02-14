/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { BaseException } from '../exception/exception';
import { Action } from './action';
import { MergeStrategy, Tree, UpdateRecorder } from './interface';
export declare class CannotCreateFileException extends BaseException {
    constructor(path: string);
}
export declare class NullTree implements Tree {
    exists(_path: string): boolean;
    read(_path: string): null;
    get(_path: string): null;
    readonly files: string[];
    beginUpdate(path: string): never;
    commitUpdate(record: UpdateRecorder): never;
    copy(path: string, _to: string): never;
    delete(path: string): never;
    create(path: string, _content: Buffer | string): never;
    rename(path: string, _to: string): never;
    overwrite(path: string, _content: Buffer | string): never;
    apply(_action: Action, _strategy?: MergeStrategy): void;
    readonly actions: Action[];
}
