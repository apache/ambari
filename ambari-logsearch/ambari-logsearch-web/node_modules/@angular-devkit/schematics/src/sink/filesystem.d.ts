/// <reference types="node" />
import { Observable } from 'rxjs/Observable';
import { VirtualFileSystemSink, VirtualFileSystemSinkHost } from './virtual-filesystem';
export declare class FileSystemSinkHost implements VirtualFileSystemSinkHost {
    protected _root: string;
    constructor(_root: string);
    exists(path: string): Observable<boolean>;
    delete(path: string): Observable<void>;
    mkDir(path: string): void;
    write(path: string, content: Buffer): Observable<void>;
    read(path: string): Observable<Buffer>;
    rename(from: string, to: string): Observable<void>;
}
export declare class FileSystemSink extends VirtualFileSystemSink {
    protected _root: string;
    constructor(_root: string, force?: boolean);
}
