/// <reference types="node" />
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/concat';
import 'rxjs/add/observable/empty';
import 'rxjs/add/observable/merge';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/reduce';
import { CreateFileAction } from '../tree/action';
import { UpdateBuffer } from '../utility/update-buffer';
import { SimpleSinkBase } from './sink';
export interface VirtualFileSystemSinkHost {
    write(path: string, content: Buffer): Observable<void>;
    delete(path: string): Observable<void>;
    exists(path: string): Observable<boolean>;
    rename(path: string, to: string): Observable<void>;
}
export declare abstract class VirtualFileSystemSink extends SimpleSinkBase {
    protected _host: VirtualFileSystemSinkHost;
    protected _force: boolean;
    protected _filesToDelete: Set<string>;
    protected _filesToRename: Set<[string, string]>;
    protected _filesToCreate: Map<string, UpdateBuffer>;
    protected _filesToUpdate: Map<string, UpdateBuffer>;
    constructor(_host: VirtualFileSystemSinkHost, _force?: boolean);
    protected _validateCreateAction(action: CreateFileAction): Observable<void>;
    protected _readFile(p: string): Observable<UpdateBuffer>;
    protected _validateFileExists(p: string): Observable<boolean>;
    protected _overwriteFile(path: string, content: Buffer): Observable<void>;
    protected _createFile(path: string, content: Buffer): Observable<void>;
    protected _renameFile(from: string, to: string): Observable<void>;
    protected _deleteFile(path: string): Observable<void>;
    _done(): Observable<void>;
}
