import { AngularCompilerOptions } from '@angular/tsc-wrapped';
import * as ts from 'typescript';
export interface NgTools_InternalApi_NG2_CodeGen_Options {
    basePath: string;
    compilerOptions: ts.CompilerOptions;
    program: ts.Program;
    host: ts.CompilerHost;
    angularCompilerOptions: AngularCompilerOptions;
    i18nFormat?: string;
    i18nFile?: string;
    locale?: string;
    missingTranslation?: string;
    readResource: (fileName: string) => Promise<string>;
}
export interface NgTools_InternalApi_NG2_ListLazyRoutes_Options {
    program: ts.Program;
    host: ts.CompilerHost;
    angularCompilerOptions: AngularCompilerOptions;
    entryModule: string;
}
export interface NgTools_InternalApi_NG_2_LazyRouteMap {
    [route: string]: string;
}
export interface NgTools_InternalApi_NG2_ExtractI18n_Options {
    basePath: string;
    compilerOptions: ts.CompilerOptions;
    program: ts.Program;
    host: ts.CompilerHost;
    angularCompilerOptions: AngularCompilerOptions;
    i18nFormat?: string;
    readResource: (fileName: string) => Promise<string>;
    locale?: string;
    outFile?: string;
}
/**
 * @internal
 * @private
 */
export declare class NgTools_InternalApi_NG_2 {
    /**
     * @internal
     * @private
     */
    static codeGen(options: NgTools_InternalApi_NG2_CodeGen_Options): Promise<any>;
    /**
     * @internal
     * @private
     */
    static listLazyRoutes(options: NgTools_InternalApi_NG2_ListLazyRoutes_Options): NgTools_InternalApi_NG_2_LazyRouteMap;
    /**
     * @internal
     * @private
     */
    static extractI18n(options: NgTools_InternalApi_NG2_ExtractI18n_Options): Promise<any>;
}
