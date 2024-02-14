import * as ts from 'typescript';
import * as tsickle from './tsickle';
/** Tsickle settings passed on the command line. */
export interface Settings {
    /** If provided, path to save externs to. */
    externsPath?: string;
    /** If provided, attempt to provide types rather than {?}. */
    isTyped?: boolean;
    /** If true, log internal debug warnings to the console. */
    verbose?: boolean;
}
export interface ClosureJSOptions {
    tsickleCompilerHostOptions: tsickle.Options;
    tsickleHost: tsickle.TsickleHost;
    files: Map<string, string>;
    tsicklePasses: tsickle.Pass[];
}
/**
 * Compiles TypeScript code into Closure-compiler-ready JS.
 * Doesn't write any files to disk; all JS content is returned in a map.
 */
export declare function toClosureJS(options: ts.CompilerOptions, fileNames: string[], settings: Settings, allDiagnostics: ts.Diagnostic[], partialClosureJSOptions?: Partial<ClosureJSOptions>): {
    jsFiles: Map<string, string>;
    externs: string;
} | null;
