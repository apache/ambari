import * as ts from 'typescript';
import { ResolverPlugin, Tapable, NormalModuleFactory } from './webpack';
export interface Mapping {
    onlyModule: boolean;
    alias: string;
    aliasPattern: RegExp;
    target: string;
}
export interface PathsPluginOptions {
    nmf: NormalModuleFactory;
    tsConfigPath: string;
    compilerOptions?: ts.CompilerOptions;
    compilerHost?: ts.CompilerHost;
}
export declare class PathsPlugin implements Tapable {
    private _nmf;
    private _tsConfigPath;
    private _compilerOptions;
    private _host;
    source: string;
    target: string;
    private _mappings;
    private _absoluteBaseUrl;
    private static _loadOptionsFromTsConfig(tsConfigPath, host?);
    constructor(options: PathsPluginOptions);
    apply(resolver: ResolverPlugin): void;
}
