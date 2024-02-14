/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */

import {SourceMapGenerator} from 'source-map';
import * as ts from 'typescript';

import {getDecoratorDeclarations} from './decorators';
import {Rewriter} from './rewriter';
import {assertTypeChecked, TypeTranslator} from './type-translator';
import {toArray} from './util';

export const ANNOTATION_SUPPORT_CODE = `
interface DecoratorInvocation {
  type: Function;
  args?: any[];
}
`;

// ClassRewriter rewrites a single "class Foo {...}" declaration.
// It's its own object because we collect decorators on the class and the ctor
// separately for each class we encounter.
class ClassRewriter extends Rewriter {
  /** Decorators on the class itself. */
  decorators: ts.Decorator[];
  /** The constructor parameter list and decorators on each param. */
  ctorParameters: ([string | undefined, ts.Decorator[]|undefined]|null)[];
  /** Per-method decorators. */
  propDecorators: Map<string, ts.Decorator[]>;

  constructor(private typeChecker: ts.TypeChecker, sourceFile: ts.SourceFile) {
    super(sourceFile);
  }

  /**
   * Determines whether the given decorator should be re-written as an annotation.
   */
  private shouldLower(decorator: ts.Decorator) {
    for (let d of getDecoratorDeclarations(decorator, this.typeChecker)) {
      // Switch to the TS JSDoc parser in the future to avoid false positives here.
      // For example using '@Annotation' in a true comment.
      // However, a new TS API would be needed, track at
      // https://github.com/Microsoft/TypeScript/issues/7393.
      let commentNode: ts.Node = d;
      // Not handling PropertyAccess expressions here, because they are
      // filtered earlier.
      if (commentNode.kind === ts.SyntaxKind.VariableDeclaration) {
        if (!commentNode.parent) continue;
        commentNode = commentNode.parent;
      }
      // Go up one more level to VariableDeclarationStatement, where usually
      // the comment lives. If the declaration has an 'export', the
      // VDList.getFullText will not contain the comment.
      if (commentNode.kind === ts.SyntaxKind.VariableDeclarationList) {
        if (!commentNode.parent) continue;
        commentNode = commentNode.parent;
      }
      let range = ts.getLeadingCommentRanges(commentNode.getFullText(), 0);
      if (!range) continue;
      for (let {pos, end} of range) {
        let jsDocText = commentNode.getFullText().substring(pos, end);
        if (jsDocText.includes('@Annotation')) return true;
      }
    }
    return false;
  }

  private decoratorsToLower(n: ts.Node): ts.Decorator[] {
    if (n.decorators) {
      return n.decorators.filter((d) => this.shouldLower(d));
    }
    return [];
  }

  /**
   * process is the main entry point, rewriting a single class node.
   */
  process(node: ts.ClassDeclaration): {output: string, diagnostics: ts.Diagnostic[]} {
    if (node.decorators) {
      let toLower = this.decoratorsToLower(node);
      if (toLower.length > 0) this.decorators = toLower;
    }

    // Emit the class contents, but stop just before emitting the closing curly brace.
    // (This code is the same as Rewriter.writeNode except for the curly brace handling.)
    let pos = node.getFullStart();
    ts.forEachChild(node, child => {
      // This forEachChild handles emitting the text between each child, while child.visit
      // recursively emits the children themselves.
      this.writeRange(pos, child.getFullStart());
      this.visit(child);
      pos = child.getEnd();
    });

    // At this point, we've emitted up through the final child of the class, so all that
    // remains is the trailing whitespace and closing curly brace.
    // The final character owned by the class node should always be a '}',
    // or we somehow got the AST wrong and should report an error.
    // (Any whitespace or semicolon following the '}' will be part of the next Node.)
    if (this.file.text[node.getEnd() - 1] !== '}') {
      this.error(node, 'unexpected class terminator');
    }
    this.writeRange(pos, node.getEnd() - 1);
    this.emitMetadata();
    this.emit('}');
    return this.getOutput();
  }

  /**
   * gatherConstructor grabs the parameter list and decorators off the class
   * constructor, and emits nothing.
   */
  private gatherConstructor(ctor: ts.ConstructorDeclaration) {
    let ctorParameters: ([string | undefined, ts.Decorator[] | undefined]|null)[] = [];
    let hasDecoratedParam = false;
    for (let param of ctor.parameters) {
      let paramCtor: string|undefined;
      let decorators: ts.Decorator[]|undefined;
      if (param.decorators) {
        decorators = this.decoratorsToLower(param);
        hasDecoratedParam = decorators.length > 0;
      }
      if (param.type) {
        // param has a type provided, e.g. "foo: Bar".
        // Verify that "Bar" is a value (e.g. a constructor) and not just a type.
        let sym = this.typeChecker.getTypeAtLocation(param.type).getSymbol();
        if (sym && (sym.flags & ts.SymbolFlags.Value)) {
          paramCtor = new TypeTranslator(this.typeChecker, param.type).symbolToString(sym);
        }
      }
      if (paramCtor || decorators) {
        ctorParameters.push([paramCtor, decorators]);
      } else {
        ctorParameters.push(null);
      }
    }

    // Use the ctor parameter metadata only if the class or the ctor was decorated.
    if (this.decorators || hasDecoratedParam) {
      this.ctorParameters = ctorParameters;
    }
  }

  /**
   * gatherMethod grabs the decorators off a class method and emits nothing.
   */
  private gatherMethodOrProperty(method: ts.Declaration) {
    if (!method.decorators) return;
    if (!method.name || method.name.kind !== ts.SyntaxKind.Identifier) {
      // Method has a weird name, e.g.
      //   [Symbol.foo]() {...}
      this.error(method, 'cannot process decorators on strangely named method');
      return;
    }

    let name = (method.name as ts.Identifier).text;
    let decorators: ts.Decorator[] = this.decoratorsToLower(method);
    if (decorators.length === 0) return;
    if (!this.propDecorators) this.propDecorators = new Map<string, ts.Decorator[]>();
    this.propDecorators.set(name, decorators);
  }

  /**
   * maybeProcess is called by the traversal of the AST.
   * @return True if the node was handled, false to have the node emitted as normal.
   */
  protected maybeProcess(node: ts.Node): boolean {
    switch (node.kind) {
      case ts.SyntaxKind.ClassDeclaration:
        // Encountered a new class while processing this class; use a new separate
        // rewriter to gather+emit its metadata.
        let {output, diagnostics} =
            new ClassRewriter(this.typeChecker, this.file).process(node as ts.ClassDeclaration);
        this.diagnostics.push(...diagnostics);
        this.emit(output);
        return true;
      case ts.SyntaxKind.Constructor:
        this.gatherConstructor(node as ts.ConstructorDeclaration);
        return false;  // Proceed with ordinary emit of the ctor.
      case ts.SyntaxKind.PropertyDeclaration:
      case ts.SyntaxKind.SetAccessor:
      case ts.SyntaxKind.GetAccessor:
      case ts.SyntaxKind.MethodDeclaration:
        this.gatherMethodOrProperty(node as ts.Declaration);
        return false;  // Proceed with ordinary emit of the method.
      case ts.SyntaxKind.Decorator:
        if (this.shouldLower(node as ts.Decorator)) {
          // Return true to signal that this node should not be emitted,
          // but still emit the whitespace *before* the node.
          this.writeRange(node.getFullStart(), node.getStart());
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  /**
   * emitMetadata emits the various gathered metadata, as static fields.
   */
  private emitMetadata() {
    if (this.decorators) {
      this.emit(`static decorators: DecoratorInvocation[] = [\n`);
      for (let annotation of this.decorators) {
        this.emitDecorator(annotation);
        this.emit(',\n');
      }
      this.emit('];\n');
    }

    if (this.decorators || this.ctorParameters) {
      this.emit(`/** @nocollapse */\n`);
      // ctorParameters may contain forward references in the type: field, so wrap in a function
      // closure
      this.emit(
          `static ctorParameters: () => ({type: any, decorators?: DecoratorInvocation[]}|null)[] = () => [\n`);
      for (let param of this.ctorParameters || []) {
        if (!param) {
          this.emit('null,\n');
          continue;
        }
        let [ctor, decorators] = param;
        this.emit(`{type: ${ctor}, `);
        if (decorators) {
          this.emit('decorators: [');
          for (let decorator of decorators) {
            this.emitDecorator(decorator);
            this.emit(', ');
          }
          this.emit(']');
        }
        this.emit('},\n');
      }
      this.emit(`];\n`);
    }

    if (this.propDecorators) {
      this.emit('static propDecorators: {[key: string]: DecoratorInvocation[]} = {\n');
      for (let name of toArray(this.propDecorators.keys())) {
        this.emit(`'${name}': [`);

        for (let decorator of this.propDecorators.get(name)!) {
          this.emitDecorator(decorator);
          this.emit(',');
        }
        this.emit('],\n');
      }
      this.emit('};\n');
    }
  }

  private emitDecorator(decorator: ts.Decorator) {
    this.emit('{ type: ');
    let expr = decorator.expression;
    switch (expr.kind) {
      case ts.SyntaxKind.Identifier:
        // The decorator was a plain @Foo.
        this.visit(expr);
        break;
      case ts.SyntaxKind.CallExpression:
        // The decorator was a call, like @Foo(bar).
        let call = expr as ts.CallExpression;
        this.visit(call.expression);
        if (call.arguments.length) {
          this.emit(', args: [');
          for (let arg of call.arguments) {
            this.emit(arg.getText());
            this.emit(', ');
          }
          this.emit(']');
        }
        break;
      default:
        this.errorUnimplementedKind(expr, 'gathering metadata');
        this.emit('undefined');
    }
    this.emit(' }');
  }
}

class DecoratorRewriter extends Rewriter {
  constructor(private typeChecker: ts.TypeChecker, sourceFile: ts.SourceFile) {
    super(sourceFile);
  }

  process(): {output: string, diagnostics: ts.Diagnostic[], sourceMap: SourceMapGenerator} {
    this.visit(this.file);
    return this.getOutput();
  }

  protected maybeProcess(node: ts.Node): boolean {
    switch (node.kind) {
      case ts.SyntaxKind.ClassDeclaration:
        let {output, diagnostics} =
            new ClassRewriter(this.typeChecker, this.file).process(node as ts.ClassDeclaration);
        this.diagnostics.push(...diagnostics);
        this.emit(output);
        return true;
      default:
        return false;
    }
  }
}

export function convertDecorators(typeChecker: ts.TypeChecker, sourceFile: ts.SourceFile):
    {output: string, diagnostics: ts.Diagnostic[], sourceMap: SourceMapGenerator} {
  assertTypeChecked(sourceFile);
  return new DecoratorRewriter(typeChecker, sourceFile).process();
}
