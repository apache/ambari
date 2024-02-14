/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SourceMapGenerator } from 'source-map';
import * as ts from 'typescript';
/**
 * A Rewriter manages iterating through a ts.SourceFile, copying input
 * to output while letting the subclass potentially alter some nodes
 * along the way by implementing maybeProcess().
 */
export declare abstract class Rewriter {
    protected file: ts.SourceFile;
    private output;
    /** Errors found while examining the code. */
    protected diagnostics: ts.Diagnostic[];
    /** The source map that's generated while rewriting this file. */
    private sourceMap;
    /** Current position in the output. */
    private position;
    /**
     * The current level of recursion through TypeScript Nodes.  Used in formatting internal debug
     * print statements.
     */
    private indent;
    constructor(file: ts.SourceFile);
    getOutput(): {
        output: string;
        diagnostics: ts.Diagnostic[];
        sourceMap: SourceMapGenerator;
    };
    /**
     * visit traverses a Node, recursively writing all nodes not handled by this.maybeProcess.
     */
    visit(node: ts.Node): void;
    /**
     * maybeProcess lets subclasses optionally processes a node.
     *
     * @return True if the node has been handled and doesn't need to be traversed;
     *    false to have the node written and its children recursively visited.
     */
    protected maybeProcess(node: ts.Node): boolean;
    /** writeNode writes a ts.Node, calling this.visit() on its children. */
    writeNode(node: ts.Node, skipComments?: boolean): void;
    writeRange(from: number, to: number): void;
    emit(str: string): void;
    /** Removes comment metacharacters from a string, to make it safe to embed in a comment. */
    escapeForComment(str: string): string;
    logWithIndent(message: string): void;
    /**
     * Produces a compiler error that references the Node's kind.  This is useful for the "else"
     * branch of code that is attempting to handle all possible input Node types, to ensure all cases
     * covered.
     */
    errorUnimplementedKind(node: ts.Node, where: string): void;
    error(node: ts.Node, messageText: string): void;
}
/** Returns the string contents of a ts.Identifier. */
export declare function getIdentifierText(identifier: ts.Identifier): string;
/**
 * Converts an escaped TypeScript name into the original source name.
 * Prefer getIdentifierText() instead if possible.
 */
export declare function unescapeName(name: string): string;
