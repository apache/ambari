/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */

import {SourceMapGenerator} from 'source-map';
import * as ts from 'typescript';

/**
 * A Rewriter manages iterating through a ts.SourceFile, copying input
 * to output while letting the subclass potentially alter some nodes
 * along the way by implementing maybeProcess().
 */
export abstract class Rewriter {
  private output: string[] = [];
  /** Errors found while examining the code. */
  protected diagnostics: ts.Diagnostic[] = [];
  /** The source map that's generated while rewriting this file. */
  private sourceMap: SourceMapGenerator;
  /** Current position in the output. */
  private position = {line: 1, column: 1};
  /**
   * The current level of recursion through TypeScript Nodes.  Used in formatting internal debug
   * print statements.
   */
  private indent: number = 0;

  constructor(protected file: ts.SourceFile) {
    this.sourceMap = new SourceMapGenerator({file: file.fileName});
    this.sourceMap.addMapping({
      original: this.position,
      generated: this.position,
      source: file.fileName,
    });
  }

  getOutput(): {output: string, diagnostics: ts.Diagnostic[], sourceMap: SourceMapGenerator} {
    if (this.indent !== 0) {
      throw new Error('visit() failed to track nesting');
    }
    return {
      output: this.output.join(''),
      diagnostics: this.diagnostics,
      sourceMap: this.sourceMap,
    };
  }

  /**
   * visit traverses a Node, recursively writing all nodes not handled by this.maybeProcess.
   */
  visit(node: ts.Node) {
    // this.logWithIndent('node: ' + ts.SyntaxKind[node.kind]);
    this.indent++;
    if (!this.maybeProcess(node)) {
      this.writeNode(node);
    }
    this.indent--;
  }

  /**
   * maybeProcess lets subclasses optionally processes a node.
   *
   * @return True if the node has been handled and doesn't need to be traversed;
   *    false to have the node written and its children recursively visited.
   */
  protected maybeProcess(node: ts.Node): boolean {
    return false;
  }

  /** writeNode writes a ts.Node, calling this.visit() on its children. */
  writeNode(node: ts.Node, skipComments = false) {
    let pos = node.getFullStart();
    if (skipComments) {
      // To skip comments, we skip all whitespace/comments preceding
      // the node.  But if there was anything skipped we should emit
      // a newline in its place so that the node remains separated
      // from the previous node.  TODO: don't skip anything here if
      // there wasn't any comment.
      if (node.getFullStart() < node.getStart()) {
        this.emit('\n');
      }
      pos = node.getStart();
    }
    ts.forEachChild(node, child => {
      this.writeRange(pos, child.getFullStart());
      this.visit(child);
      pos = child.getEnd();
    });
    this.writeRange(pos, node.getEnd());
  }

  // Write a span of the input file as expressed by absolute offsets.
  // These offsets are found in attributes like node.getFullStart() and
  // node.getEnd().
  writeRange(from: number, to: number) {
    // getSourceFile().getText() is wrong here because it has the text of
    // the SourceFile node of the AST, which doesn't contain the comments
    // preceding that node.  Semantically these ranges are just offsets
    // into the original source file text, so slice from that.
    let text = this.file.text.slice(from, to);
    if (text) {
      // Add a source mapping. writeRange(from, to) always corresponds to
      // original source code, so add a mapping at the current location that
      // points back to the location at `from`. The additional code generated
      // by tsickle will then be considered part of the last mapped code
      // section preceding it. That's arguably incorrect (e.g. for the fake
      // methods defining properties), but is good enough for stack traces.
      const pos = this.file.getLineAndCharacterOfPosition(from);
      this.sourceMap.addMapping({
        original: {line: pos.line + 1, column: pos.character + 1},
        generated: this.position,
        source: this.file.fileName,
      });
      this.emit(text);
    }
  }

  emit(str: string) {
    this.output.push(str);
    for (const c of str) {
      this.position.column++;
      if (c === '\n') {
        this.position.line++;
        this.position.column = 1;
      }
    }
  }

  /** Removes comment metacharacters from a string, to make it safe to embed in a comment. */
  escapeForComment(str: string): string {
    return str.replace(/\/\*/g, '__').replace(/\*\//g, '__');
  }

  /* tslint:disable: no-unused-variable */
  logWithIndent(message: string) {
    /* tslint:enable: no-unused-variable */
    let prefix = new Array(this.indent + 1).join('| ');
    console.log(prefix + message);
  }

  /**
   * Produces a compiler error that references the Node's kind.  This is useful for the "else"
   * branch of code that is attempting to handle all possible input Node types, to ensure all cases
   * covered.
   */
  errorUnimplementedKind(node: ts.Node, where: string) {
    this.error(node, `${ts.SyntaxKind[node.kind]} not implemented in ${where}`);
  }

  error(node: ts.Node, messageText: string) {
    this.diagnostics.push({
      file: this.file,
      start: node.getStart(),
      length: node.getEnd() - node.getStart(),
      messageText: messageText,
      category: ts.DiagnosticCategory.Error,
      code: 0,
    });
  }
}

/** Returns the string contents of a ts.Identifier. */
export function getIdentifierText(identifier: ts.Identifier): string {
  // NOTE: the 'text' property on an Identifier may be escaped if it starts
  // with '__', so just use getText().
  return identifier.getText();
}

/**
 * Converts an escaped TypeScript name into the original source name.
 * Prefer getIdentifierText() instead if possible.
 */
export function unescapeName(name: string): string {
  // See the private function unescapeIdentifier in TypeScript's utilities.ts.
  if (name.match(/^___/)) return name.substr(1);
  return name;
}
