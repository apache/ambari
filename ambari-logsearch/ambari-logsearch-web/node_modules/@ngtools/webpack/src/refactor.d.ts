import * as ts from 'typescript';
export interface TranspileOutput {
    outputText: string;
    sourceMap: any | null;
}
export declare class TypeScriptFileRefactor {
    private _program;
    private _fileName;
    private _sourceFile;
    private _sourceString;
    private _sourceText;
    private _changed;
    readonly fileName: string;
    readonly sourceFile: ts.SourceFile;
    readonly sourceText: any;
    constructor(fileName: string, _host: ts.CompilerHost, _program?: ts.Program, source?: string | null);
    /**
     * Collates the diagnostic messages for the current source file
     */
    getDiagnostics(typeCheck?: boolean): ts.Diagnostic[];
    /**
     * Find all nodes from the AST in the subtree of node of SyntaxKind kind.
     * @param node The root node to check, or null if the whole tree should be searched.
     * @param kind The kind of nodes to find.
     * @param recursive Whether to go in matched nodes to keep matching.
     * @param max The maximum number of items to return.
     * @return all nodes of kind, or [] if none is found
     */
    findAstNodes(node: ts.Node | null, kind: ts.SyntaxKind, recursive?: boolean, max?: number): ts.Node[];
    findFirstAstNode(node: ts.Node | null, kind: ts.SyntaxKind): ts.Node | null;
    appendAfter(node: ts.Node, text: string): void;
    append(node: ts.Node, text: string): void;
    prependBefore(node: ts.Node, text: string): void;
    insertImport(symbolName: string, modulePath: string): void;
    removeNode(node: ts.Node): void;
    removeNodes(...nodes: Array<ts.Node | null>): void;
    replaceNode(node: ts.Node, replacement: string): void;
    sourceMatch(re: RegExp): boolean;
    transpile(compilerOptions: ts.CompilerOptions): TranspileOutput;
}
