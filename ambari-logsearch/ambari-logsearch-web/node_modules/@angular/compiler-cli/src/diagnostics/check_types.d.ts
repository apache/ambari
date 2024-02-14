/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { AotCompilerHost, AotCompilerOptions, GeneratedFile, NgAnalyzedModules } from '@angular/compiler';
import * as ts from 'typescript';
import { Diagnostic } from '../transformers/api';
export declare class TypeChecker {
    private program;
    private tsOptions;
    private compilerHost;
    private aotCompilerHost;
    private aotOptions;
    private _analyzedModules;
    private _generatedFiles;
    private _aotCompiler;
    private _reflector;
    private _factories;
    private _factoryNames;
    private _diagnosticProgram;
    private _diagnosticsByFile;
    private _currentCancellationToken;
    private _partial;
    constructor(program: ts.Program, tsOptions: ts.CompilerOptions, compilerHost: ts.CompilerHost, aotCompilerHost: AotCompilerHost, aotOptions: AotCompilerOptions, _analyzedModules?: NgAnalyzedModules, _generatedFiles?: GeneratedFile[]);
    getDiagnostics(fileName?: string, cancellationToken?: ts.CancellationToken): Diagnostic[];
    readonly partialResults: boolean;
    private readonly analyzedModules;
    private readonly diagnosticsByFileName;
    private readonly diagnosticProgram;
    private readonly generatedFiles;
    private readonly aotCompiler;
    private readonly reflector;
    private readonly factories;
    private readonly factoryNames;
    private createCompilerAndReflector();
    private createDiagnosticProgram();
    private createFactories();
    private createDiagnosticsByFile();
    private sourceSpanOf(source, start, length);
}
