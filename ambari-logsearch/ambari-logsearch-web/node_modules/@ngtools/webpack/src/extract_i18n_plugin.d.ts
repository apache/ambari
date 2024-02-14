import { Tapable } from './webpack';
export interface ExtractI18nPluginOptions {
    tsConfigPath: string;
    basePath?: string;
    genDir?: string;
    i18nFormat?: string;
    locale?: string;
    outFile?: string;
    exclude?: string[];
}
export declare class ExtractI18nPlugin implements Tapable {
    private _resourceLoader;
    private _donePromise;
    private _compiler;
    private _compilation;
    private _tsConfigPath;
    private _basePath;
    private _genDir;
    private _rootFilePath;
    private _compilerOptions;
    private _angularCompilerOptions;
    private _compilerHost;
    private _program;
    private _i18nFormat?;
    private _locale?;
    private _outFile?;
    constructor(options: ExtractI18nPluginOptions);
    private _setupOptions(options);
    apply(compiler: any): void;
    private _make(compilation, cb);
}
