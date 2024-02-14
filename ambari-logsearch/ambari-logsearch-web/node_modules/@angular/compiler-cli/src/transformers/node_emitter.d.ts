/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { ParseSourceSpan, Statement } from '@angular/compiler';
import * as ts from 'typescript';
export interface Node {
    sourceSpan: ParseSourceSpan | null;
}
export declare class TypeScriptNodeEmitter {
    updateSourceFile(sourceFile: ts.SourceFile, stmts: Statement[], preamble?: string): [ts.SourceFile, Map<ts.Node, Node>];
}
