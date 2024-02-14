/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SourceMapGenerator } from 'source-map';
import * as ts from 'typescript';
import { Options } from './tsickle_compiler_host';
export { convertDecorators } from './decorator-annotator';
export { processES5 } from './es5processor';
export { FileMap, ModulesManifest } from './modules_manifest';
export { Options, Pass, TsickleCompilerHost, TsickleHost } from './tsickle_compiler_host';
export interface Output {
    /** The TypeScript source with Closure annotations inserted. */
    output: string;
    /** Generated externs declarations, if any. */
    externs: string | null;
    /** Error messages, if any. */
    diagnostics: ts.Diagnostic[];
    /** A source map mapping back into the original sources. */
    sourceMap: SourceMapGenerator;
}
/**
 * The header to be used in generated externs.  This is not included in the
 * output of annotate() because annotate() works one file at a time, and
 * typically you create one externs file from the entire compilation unit.
 */
export declare const EXTERNS_HEADER: string;
/**
 * Symbols that are already declared as externs in Closure, that should
 * be avoided by tsickle's "declare ..." => externs.js conversion.
 */
export declare let closureExternsBlacklist: string[];
export declare function formatDiagnostics(diags: ts.Diagnostic[]): string;
export declare function isDtsFileName(fileName: string): boolean;
export declare function annotate(program: ts.Program, file: ts.SourceFile, pathToModuleName: (context: string, importPath: string) => string, options?: Options, host?: ts.ModuleResolutionHost, tsOpts?: ts.CompilerOptions): Output;
