/// <reference types="node" />
import { Stats } from 'fs';
import { InputFileSystem, Callback } from './webpack';
import { WebpackCompilerHost } from './compiler_host';
export declare class VirtualFileSystemDecorator implements InputFileSystem {
    private _inputFileSystem;
    private _webpackCompilerHost;
    constructor(_inputFileSystem: InputFileSystem, _webpackCompilerHost: WebpackCompilerHost);
    private _readFileSync(path);
    private _statSync(path);
    private _readDirSync(path);
    stat(path: string, callback: Callback<any>): void;
    readdir(path: string, callback: Callback<any>): void;
    readFile(path: string, callback: Callback<any>): void;
    readJson(path: string, callback: Callback<any>): void;
    readlink(path: string, callback: Callback<any>): void;
    statSync(path: string): Stats;
    readdirSync(path: string): string[];
    readFileSync(path: string): string;
    readJsonSync(path: string): string;
    readlinkSync(path: string): string;
    purge(changes?: string[] | string): void;
}
