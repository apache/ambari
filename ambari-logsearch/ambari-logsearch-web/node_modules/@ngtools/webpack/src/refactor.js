"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// TODO: move this in its own package.
const path = require("path");
const ts = require("typescript");
const source_map_1 = require("source-map");
const MagicString = require('magic-string');
function resolve(filePath, _host, program) {
    if (path.isAbsolute(filePath)) {
        return filePath;
    }
    const compilerOptions = program.getCompilerOptions();
    const basePath = compilerOptions.baseUrl || compilerOptions.rootDir;
    if (!basePath) {
        throw new Error(`Trying to resolve '${filePath}' without a basePath.`);
    }
    return path.join(basePath, filePath);
}
class TypeScriptFileRefactor {
    constructor(fileName, _host, _program, source) {
        this._program = _program;
        this._changed = false;
        fileName = resolve(fileName, _host, _program).replace(/\\/g, '/');
        this._fileName = fileName;
        if (_program) {
            if (source) {
                this._sourceFile = ts.createSourceFile(fileName, source, ts.ScriptTarget.Latest, true);
            }
            else {
                this._sourceFile = _program.getSourceFile(fileName);
            }
        }
        if (!this._sourceFile) {
            this._sourceFile = ts.createSourceFile(fileName, source || _host.readFile(fileName), ts.ScriptTarget.Latest, true);
        }
        this._sourceText = this._sourceFile.getFullText(this._sourceFile);
        this._sourceString = new MagicString(this._sourceText);
    }
    get fileName() { return this._fileName; }
    get sourceFile() { return this._sourceFile; }
    get sourceText() { return this._sourceString.toString(); }
    /**
     * Collates the diagnostic messages for the current source file
     */
    getDiagnostics(typeCheck = true) {
        if (!this._program) {
            return [];
        }
        let diagnostics = [];
        // only concat the declaration diagnostics if the tsconfig config sets it to true.
        if (this._program.getCompilerOptions().declaration == true) {
            diagnostics = diagnostics.concat(this._program.getDeclarationDiagnostics(this._sourceFile));
        }
        diagnostics = diagnostics.concat(this._program.getSyntacticDiagnostics(this._sourceFile), typeCheck ? this._program.getSemanticDiagnostics(this._sourceFile) : []);
        return diagnostics;
    }
    /**
     * Find all nodes from the AST in the subtree of node of SyntaxKind kind.
     * @param node The root node to check, or null if the whole tree should be searched.
     * @param kind The kind of nodes to find.
     * @param recursive Whether to go in matched nodes to keep matching.
     * @param max The maximum number of items to return.
     * @return all nodes of kind, or [] if none is found
     */
    findAstNodes(node, kind, recursive = false, max = Infinity) {
        if (max == 0) {
            return [];
        }
        if (!node) {
            node = this._sourceFile;
        }
        let arr = [];
        if (node.kind === kind) {
            // If we're not recursively looking for children, stop here.
            if (!recursive) {
                return [node];
            }
            arr.push(node);
            max--;
        }
        if (max > 0) {
            for (const child of node.getChildren(this._sourceFile)) {
                this.findAstNodes(child, kind, recursive, max)
                    .forEach((node) => {
                    if (max > 0) {
                        arr.push(node);
                    }
                    max--;
                });
                if (max <= 0) {
                    break;
                }
            }
        }
        return arr;
    }
    findFirstAstNode(node, kind) {
        return this.findAstNodes(node, kind, false, 1)[0] || null;
    }
    appendAfter(node, text) {
        this._sourceString.appendRight(node.getEnd(), text);
    }
    append(node, text) {
        this._sourceString.appendLeft(node.getEnd(), text);
    }
    prependBefore(node, text) {
        this._sourceString.appendLeft(node.getStart(), text);
    }
    insertImport(symbolName, modulePath) {
        // Find all imports.
        const allImports = this.findAstNodes(this._sourceFile, ts.SyntaxKind.ImportDeclaration);
        const maybeImports = allImports
            .filter((node) => {
            // Filter all imports that do not match the modulePath.
            return node.moduleSpecifier.kind == ts.SyntaxKind.StringLiteral
                && node.moduleSpecifier.text == modulePath;
        })
            .filter((node) => {
            // Remove import statements that are either `import 'XYZ'` or `import * as X from 'XYZ'`.
            const clause = node.importClause;
            if (!clause || clause.name || !clause.namedBindings) {
                return false;
            }
            return clause.namedBindings.kind == ts.SyntaxKind.NamedImports;
        })
            .map((node) => {
            // Return the `{ ... }` list of the named import.
            return node.importClause.namedBindings;
        });
        if (maybeImports.length) {
            // There's an `import {A, B, C} from 'modulePath'`.
            // Find if it's in either imports. If so, just return; nothing to do.
            const hasImportAlready = maybeImports.some((node) => {
                return node.elements.some((element) => {
                    return element.name.text == symbolName;
                });
            });
            if (hasImportAlready) {
                return;
            }
            // Just pick the first one and insert at the end of its identifier list.
            this.appendAfter(maybeImports[0].elements[maybeImports[0].elements.length - 1], `, ${symbolName}`);
        }
        else {
            // Find the last import and insert after.
            this.appendAfter(allImports[allImports.length - 1], `import {${symbolName}} from '${modulePath}';`);
        }
    }
    removeNode(node) {
        this._sourceString.remove(node.getStart(this._sourceFile), node.getEnd());
        this._changed = true;
    }
    removeNodes(...nodes) {
        nodes.forEach(node => node && this.removeNode(node));
    }
    replaceNode(node, replacement) {
        let replaceSymbolName = node.kind === ts.SyntaxKind.Identifier;
        this._sourceString.overwrite(node.getStart(this._sourceFile), node.getEnd(), replacement, { storeName: replaceSymbolName });
        this._changed = true;
    }
    sourceMatch(re) {
        return this._sourceText.match(re) !== null;
    }
    transpile(compilerOptions) {
        const source = this.sourceText;
        const result = ts.transpileModule(source, {
            compilerOptions: Object.assign({}, compilerOptions, {
                sourceMap: true,
                inlineSources: false,
                inlineSourceMap: false,
                sourceRoot: ''
            }),
            fileName: this._fileName
        });
        if (result.sourceMapText) {
            const sourceMapJson = JSON.parse(result.sourceMapText);
            sourceMapJson.sources = [this._fileName];
            const consumer = new source_map_1.SourceMapConsumer(sourceMapJson);
            const map = source_map_1.SourceMapGenerator.fromSourceMap(consumer);
            if (this._changed) {
                const sourceMap = this._sourceString.generateMap({
                    file: path.basename(this._fileName.replace(/\.ts$/, '.js')),
                    source: this._fileName,
                    hires: true,
                });
                map.applySourceMap(new source_map_1.SourceMapConsumer(sourceMap), this._fileName);
            }
            const sourceMap = map.toJSON();
            const fileName = process.platform.startsWith('win')
                ? this._fileName.replace(/\//g, '\\')
                : this._fileName;
            sourceMap.sources = [fileName];
            sourceMap.file = path.basename(fileName, '.ts') + '.js';
            sourceMap.sourcesContent = [this._sourceText];
            return { outputText: result.outputText, sourceMap };
        }
        else {
            return {
                outputText: result.outputText,
                sourceMap: null
            };
        }
    }
}
exports.TypeScriptFileRefactor = TypeScriptFileRefactor;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/refactor.js.map