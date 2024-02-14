/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import * as ts from 'typescript';
export declare function toArray<T>(iterator: Iterator<T>): T[];
/**
 * Constructs a new ts.CompilerHost that overlays sources in substituteSource
 * over another ts.CompilerHost.
 *
 * @param substituteSource A map of source file name -> overlay source text.
 */
export declare function createSourceReplacingCompilerHost(substituteSource: Map<string, string>, delegate: ts.CompilerHost): ts.CompilerHost;
/**
 * Constructs a new ts.CompilerHost that overlays sources in substituteSource
 * over another ts.CompilerHost.
 *
 * @param outputFiles map to fill with source file name -> output text.
 */
export declare function createOutputRetainingCompilerHost(outputFiles: Map<string, string>, delegate: ts.CompilerHost): ts.CompilerHost;
