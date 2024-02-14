/// <reference types="node" />
import { FileSystemTreeHost } from './filesystem';
export declare class NodeJsHost implements FileSystemTreeHost {
    private _root;
    constructor(_root: string);
    listDirectory(path: string): string[];
    isDirectory(path: string): boolean;
    readFile(path: string): Buffer;
    join(path1: string, path2: string): string;
}
