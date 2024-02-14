import { BaseError } from 'make-error';
import * as TS from 'typescript';
export interface TSCommon {
    version: typeof TS.version;
    sys: typeof TS.sys;
    ScriptSnapshot: typeof TS.ScriptSnapshot;
    displayPartsToString: typeof TS.displayPartsToString;
    createLanguageService: typeof TS.createLanguageService;
    getDefaultLibFilePath: typeof TS.getDefaultLibFilePath;
    getPreEmitDiagnostics: typeof TS.getPreEmitDiagnostics;
    flattenDiagnosticMessageText: typeof TS.flattenDiagnosticMessageText;
    transpileModule: typeof TS.transpileModule;
    findConfigFile(path: string, fileExists?: (path: string) => boolean): string;
    readConfigFile(path: string, readFile?: (path: string) => string): {
        config?: any;
        error?: TS.Diagnostic;
    };
    parseJsonConfigFileContent?(json: any, host: any, basePath: string, existingOptions: any, configFileName: string): any;
    parseConfigFile?(json: any, host: any, basePath: string): any;
}
export declare const VERSION: any;
export interface Options {
    fast?: boolean | null;
    lazy?: boolean | null;
    cache?: boolean | null;
    cacheDirectory?: string;
    compiler?: string;
    project?: boolean | string;
    ignore?: boolean | string | string[];
    ignoreWarnings?: number | string | Array<number | string>;
    disableWarnings?: boolean | null;
    getFile?: (fileName: string) => string;
    fileExists?: (fileName: string) => boolean;
    compilerOptions?: any;
}
export interface TypeInfo {
    name: string;
    comment: string;
}
export declare function split(value: string | undefined): string[] | undefined;
export declare function parse(value: string | undefined): any;
export declare function slash(value: string): string;
export interface Register {
    cwd: string;
    extensions: string[];
    compile(code: string, fileName: string, lineOffset?: number): string;
    getTypeInfo(fileName: string, position: number): TypeInfo;
}
export declare function register(options?: Options): () => Register;
export declare function fileExists(fileName: string): boolean;
export declare function getDirectories(path: string): string[];
export declare function directoryExists(path: string): boolean;
export declare function getFile(fileName: string): string;
export declare function formatDiagnostics(diagnostics: TS.Diagnostic[], cwd: string, ts: TSCommon, lineOffset: number): TSDiagnostic[];
export interface TSDiagnostic {
    message: string;
    code: number;
}
export declare function formatDiagnostic(diagnostic: TS.Diagnostic, cwd: string, ts: TSCommon, lineOffset: number): TSDiagnostic;
export declare class TSError extends BaseError {
    diagnostics: TSDiagnostic[];
    name: string;
    constructor(diagnostics: TSDiagnostic[]);
}
