/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import * as ts from 'typescript';
/**
 * Returns the declarations for the given decorator.
 */
export declare function getDecoratorDeclarations(decorator: ts.Decorator, typeChecker: ts.TypeChecker): ts.Declaration[];
/**
 * Returns true if node has an exporting decorator  (i.e., a decorator with @ExportDecoratedItems
 * in its JSDoc).
 */
export declare function hasExportingDecorator(node: ts.Node, typeChecker: ts.TypeChecker): boolean | undefined;
