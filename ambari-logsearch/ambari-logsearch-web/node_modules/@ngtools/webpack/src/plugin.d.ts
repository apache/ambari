import * as ts from 'typescript';
import { WebpackCompilerHost } from './compiler_host';
import { Tapable } from './webpack';
import { LazyRouteMap } from './lazy_routes';
/**
 * Option Constants
 */
export interface AotPluginOptions {
    tsConfigPath: string;
    basePath?: string;
    entryModule?: string;
    mainPath?: string;
    typeChecking?: boolean;
    skipCodeGeneration?: boolean;
    replaceExport?: boolean;
    hostOverrideFileSystem?: {
        [path: string]: string;
    };
    hostReplacementPaths?: {
        [path: string]: string;
    };
    i18nFile?: string;
    i18nFormat?: string;
    locale?: string;
    missingTranslation?: string;
    exclude?: string | string[];
    compilerOptions?: ts.CompilerOptions;
}
export declare class AotPlugin implements Tapable {
    private _options;
    private _compilerOptions;
    private _angularCompilerOptions;
    private _program;
    private _rootFilePath;
    private _compilerHost;
    private _resourceLoader;
    private _discoveredLazyRoutes;
    private _lazyRoutes;
    private _tsConfigPath;
    private _entryModule;
    private _donePromise;
    private _compiler;
    private _compilation;
    private _failedCompilation;
    private _typeCheck;
    private _skipCodeGeneration;
    private _replaceExport;
    private _basePath;
    private _genDir;
    private _i18nFile?;
    private _i18nFormat?;
    private _locale?;
    private _missingTranslation?;
    private _diagnoseFiles;
    private _firstRun;
    constructor(options: AotPluginOptions);
    readonly options: AotPluginOptions;
    readonly basePath: string;
    readonly compilation: any;
    readonly compilerHost: WebpackCompilerHost;
    readonly compilerOptions: ts.CompilerOptions;
    readonly done: Promise<void>;
    readonly failedCompilation: boolean;
    readonly entryModule: {
        path: string;
        className: string;
    };
    readonly genDir: string;
    readonly program: ts.Program;
    readonly skipCodeGeneration: boolean;
    readonly replaceExport: boolean;
    readonly typeCheck: boolean;
    readonly i18nFile: string;
    readonly i18nFormat: string;
    readonly locale: string;
    readonly missingTranslation: string;
    readonly firstRun: boolean;
    readonly lazyRoutes: LazyRouteMap;
    readonly discoveredLazyRoutes: LazyRouteMap;
    private _setupOptions(options);
    private _findLazyRoutesInAst();
    private _getLazyRoutesFromNgtools();
    apply(compiler: any): void;
    private _translateSourceMap(sourceText, fileName, {line, character});
    diagnose(fileName: string): void;
    private _make(compilation, cb);
}
