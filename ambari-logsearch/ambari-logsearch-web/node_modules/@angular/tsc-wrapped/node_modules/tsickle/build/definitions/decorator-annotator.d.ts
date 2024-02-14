/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SourceMapGenerator } from 'source-map';
import * as ts from 'typescript';
export declare const ANNOTATION_SUPPORT_CODE: string;
export declare function convertDecorators(typeChecker: ts.TypeChecker, sourceFile: ts.SourceFile): {
    output: string;
    diagnostics: ts.Diagnostic[];
    sourceMap: SourceMapGenerator;
};
