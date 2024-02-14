import * as ts from 'typescript';
import AngularCompilerOptions from './options';
import { VinylFile } from './vinyl_file';
/**
 * Our interface to the TypeScript standard compiler.
 * If you write an Angular compiler plugin for another build tool,
 * you should implement a similar interface.
 */
export interface CompilerInterface {
    readConfiguration(project: string | VinylFile, basePath: string, existingOptions?: ts.CompilerOptions): {
        parsed: ts.ParsedCommandLine;
        ngOptions: AngularCompilerOptions;
    };
    typeCheck(compilerHost: ts.CompilerHost, program: ts.Program): void;
    emit(program: ts.Program): number;
}
export declare class UserError extends Error {
    private _nativeError;
    constructor(message: string);
    message: string;
    name: string;
    stack: any;
    toString(): string;
}
export declare function formatDiagnostics(diags: ts.Diagnostic[]): string;
export declare function check(diags: ts.Diagnostic[]): void;
export declare function validateAngularCompilerOptions(options: AngularCompilerOptions): ts.Diagnostic[];
export declare class Tsc implements CompilerInterface {
    private readFile;
    private readDirectory;
    private parseConfigHost;
    constructor(readFile?: (path: string, encoding?: string) => string, readDirectory?: (path: string, extensions?: string[], exclude?: string[], include?: string[]) => string[]);
    readConfiguration(project: string | VinylFile, basePath: string, existingOptions?: ts.CompilerOptions): {
        parsed: ts.ParsedCommandLine;
        ngOptions: any;
    };
    typeCheck(compilerHost: ts.CompilerHost, program: ts.Program): void;
    emit(program: ts.Program): number;
}
export declare const tsc: CompilerInterface;
