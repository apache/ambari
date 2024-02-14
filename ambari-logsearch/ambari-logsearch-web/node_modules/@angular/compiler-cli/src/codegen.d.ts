/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * Transform template html and css into executable code.
 * Intended to be used in a build step.
 */
import * as compiler from '@angular/compiler';
import { AngularCompilerOptions, NgcCliOptions } from '@angular/tsc-wrapped';
import * as ts from 'typescript';
import { CompilerHost, CompilerHostContext } from './compiler_host';
export declare class CodeGenerator {
    private options;
    private program;
    host: ts.CompilerHost;
    private compiler;
    private ngCompilerHost;
    constructor(options: AngularCompilerOptions, program: ts.Program, host: ts.CompilerHost, compiler: compiler.AotCompiler, ngCompilerHost: CompilerHost);
    codegen(): Promise<string[]>;
    codegenSync(): string[];
    private emit(analyzedModules);
    static create(options: AngularCompilerOptions, cliOptions: NgcCliOptions, program: ts.Program, tsCompilerHost: ts.CompilerHost, compilerHostContext?: CompilerHostContext, ngCompilerHost?: CompilerHost): CodeGenerator;
}
