/// <reference types="node" />
import { FileSystemTreeHost } from './filesystem';
export declare class InMemoryFileSystemTreeHost implements FileSystemTreeHost {
    private _content;
    private _files;
    constructor(content: {
        [path: string]: string;
    });
    listDirectory(path: string): string[];
    isDirectory(path: string): boolean;
    readFile(path: string): Buffer;
    join(path1: string, path2: string): string & {
        __PRIVATE_SCHEMATIC_PATH: void;
    };
}
