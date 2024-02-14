import * as ts from 'typescript';
import { ModulesManifest } from './modules_manifest';
/**
 * Tsickle can perform 2 different precompilation transforms - decorator downleveling
 * and closurization.  Both require tsc to have already type checked their
 * input, so they can't both be run in one call to tsc. If you only want one of
 * the transforms, you can specify it in the constructor, if you want both, you'll
 * have to specify it by calling reconfigureForRun() with the appropriate Pass.
 */
export declare enum Pass {
    NONE = 0,
    DECORATOR_DOWNLEVEL = 1,
    CLOSURIZE = 2,
}
export interface Options {
    googmodule?: boolean;
    es5Mode?: boolean;
    prelude?: string;
    /**
     * If true, convert every type to the Closure {?} type, which means
     * "don't check types".
     */
    untyped?: boolean;
    /**
     * If provided a function that logs an internal warning.
     * These warnings are not actionable by an end user and should be hidden
     * by default.
     */
    logWarning?: (warning: ts.Diagnostic) => void;
    /** If provided, a set of paths whose types should always generate as {?}. */
    typeBlackListPaths?: Set<string>;
    /**
     * Convert shorthand "/index" imports to full path (include the "/index").
     * Annotation will be slower because every import must be resolved.
     */
    convertIndexImportShorthand?: boolean;
}
/**
 *  Provides hooks to customize TsickleCompilerHost's behavior for different
 *  compilation environments.
 */
export interface TsickleHost {
    /**
     * If true, tsickle and decorator downlevel processing will be skipped for
     * that file.
     */
    shouldSkipTsickleProcessing(fileName: string): boolean;
    /**
     * Takes a context (the current file) and the path of the file to import
     *  and generates a googmodule module name
     */
    pathToModuleName(context: string, importPath: string): string;
    /**
     * Tsickle treats warnings as errors, if true, ignore warnings.  This might be
     * useful for e.g. third party code.
     */
    shouldIgnoreWarningsForPath(filePath: string): boolean;
    /**
     * If we do googmodule processing, we polyfill module.id, since that's
     * part of ES6 modules.  This function determines what the module.id will be
     * for each file.
     */
    fileNameToModuleId(fileName: string): string;
}
/**
 * TsickleCompilerHost does tsickle processing of input files, including
 * closure type annotation processing, decorator downleveling and
 * require -> googmodule rewriting.
 */
export declare class TsickleCompilerHost implements ts.CompilerHost {
    private delegate;
    private tscOptions;
    private options;
    private environment;
    modulesManifest: ModulesManifest;
    /** Error messages produced by tsickle, if any. */
    diagnostics: ts.Diagnostic[];
    /** externs.js files produced by tsickle, if any. */
    externs: {
        [fileName: string]: string;
    };
    private sourceFileToPreexistingSourceMap;
    private preexistingSourceMaps;
    private decoratorDownlevelSourceMaps;
    private tsickleSourceMaps;
    private runConfiguration;
    constructor(delegate: ts.CompilerHost, tscOptions: ts.CompilerOptions, options: Options, environment: TsickleHost);
    /**
     * Tsickle can perform 2 kinds of precompilation source transforms - decorator
     * downleveling and closurization.  They can't be run in the same run of the
     * typescript compiler, because they both depend on type information that comes
     * from running the compiler.  We need to use the same compiler host to run both
     * so we have all the source map data when finally write out.  Thus if we want
     * to run both transforms, we call reconfigureForRun() between the calls to
     * ts.createProgram().
     */
    reconfigureForRun(oldProgram: ts.Program, pass: Pass): void;
    getSourceFile(fileName: string, languageVersion: ts.ScriptTarget, onError?: (message: string) => void): ts.SourceFile;
    writeFile(fileName: string, content: string, writeByteOrderMark: boolean, onError?: (message: string) => void, sourceFiles?: ts.SourceFile[]): void;
    getSourceMapKeyForPathAndName(outputFilePath: string, sourceFileName: string): string;
    getSourceMapKeyForSourceFile(sourceFile: ts.SourceFile): string;
    stripAndStoreExistingSourceMap(sourceFile: ts.SourceFile): ts.SourceFile;
    combineSourceMaps(filePath: string, tscSourceMapText: string): string;
    combineInlineSourceMaps(filePath: string, compiledJsWithInlineSourceMap: string): string;
    convertCommonJsToGoogModule(fileName: string, content: string): string;
    private downlevelDecorators(sourceFile, program, fileName, languageVersion);
    private closurize(sourceFile, program, fileName, languageVersion);
    /** Concatenate all generated externs definitions together into a string. */
    getGeneratedExterns(): string;
    fileExists(fileName: string): boolean;
    getCurrentDirectory(): string;
    useCaseSensitiveFileNames(): boolean;
    getNewLine(): string;
    getDirectories(path: string): string[];
    readFile(fileName: string): string;
    getDefaultLibFileName(options: ts.CompilerOptions): string;
    getCanonicalFileName(fileName: string): string;
    getCancellationToken: (() => ts.CancellationToken) | undefined;
    getDefaultLibLocation: (() => string) | undefined;
    resolveModuleNames: ((moduleNames: string[], containingFile: string) => ts.ResolvedModule[]) | undefined;
    resolveTypeReferenceDirectives: ((typeReferenceDirectiveNames: string[], containingFile: string) => ts.ResolvedTypeReferenceDirective[]) | undefined;
    getEnvironmentVariable: ((name: string) => string) | undefined;
    trace: ((s: string) => void) | undefined;
    directoryExists: ((directoryName: string) => boolean) | undefined;
    realpath: ((path: string) => string) | undefined;
}
