/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import 'reflect-metadata';
import * as ts from 'typescript';
export declare function readConfiguration(project: string, basePath: string, existingOptions?: ts.CompilerOptions): {
    parsed: ts.ParsedCommandLine;
    ngOptions: any;
};
export declare function performCompilation(basePath: string, files: string[], options: ts.CompilerOptions, ngOptions: any, consoleError?: (s: string) => void, tsCompilerHost?: ts.CompilerHost): 0 | 1;
export declare function main(args: string[], consoleError?: (s: string) => void): number;
