import * as ts from 'typescript';
export declare function getChildOfKind(node: ts.Node, kind: ts.SyntaxKind, sourceFile?: ts.SourceFile): ts.Node | undefined;
export declare function isTokenKind(kind: ts.SyntaxKind): boolean;
export declare function isNodeKind(kind: ts.SyntaxKind): boolean;
export declare function isAssignmentKind(kind: ts.SyntaxKind): boolean;
export declare function isTypeNodeKind(kind: ts.SyntaxKind): boolean;
export declare function isJsDocKind(kind: ts.SyntaxKind): boolean;
export declare function isThisParameter(parameter: ts.ParameterDeclaration): boolean;
export declare function hasModifier(modifiers: ts.Modifier[] | undefined, ...kinds: Array<ts.Modifier['kind']>): boolean;
export declare function isParameterProperty(node: ts.ParameterDeclaration): boolean;
export declare function hasAccessModifier(node: ts.ClassElement | ts.ParameterDeclaration): boolean;
export declare const isNodeFlagSet: (node: ts.Node, flag: ts.NodeFlags) => boolean;
export declare const isTypeFlagSet: (type: ts.Type, flag: ts.TypeFlags) => boolean;
export declare const isSymbolFlagSet: (symbol: ts.Symbol, flag: ts.SymbolFlags) => boolean;
export declare function isObjectFlagSet(objectType: ts.ObjectType, flag: ts.ObjectFlags): boolean;
export declare function isModfierFlagSet(node: ts.Node, flag: ts.ModifierFlags): boolean;
export declare function getPreviousStatement(statement: ts.Statement): ts.Statement | undefined;
export declare function getNextStatement(statement: ts.Statement): ts.Statement | undefined;
export declare function getPreviousToken(node: ts.Node, sourceFile?: ts.SourceFile): ts.Node | undefined;
export declare function getNextToken(node: ts.Node, sourceFile?: ts.SourceFile): ts.Node | undefined;
export declare function getPropertyName(propertyName: ts.PropertyName): string | undefined;
export declare function forEachDestructuringIdentifier<T>(pattern: ts.BindingPattern, fn: (element: ts.BindingElement & {
    name: ts.Identifier;
}) => T): T | undefined;
export declare function forEachDeclaredVariable<T>(declarationList: ts.VariableDeclarationList, cb: (element: ts.VariableLikeDeclaration & {
    name: ts.Identifier;
}) => T): T | undefined;
export declare const enum VariableDeclarationKind {
    Var = 0,
    Let = 1,
    Const = 2,
}
export declare function getVariableDeclarationKind(declarationList: ts.VariableDeclarationList): VariableDeclarationKind;
export declare function isBlockScopedVariableDeclarationList(declarationList: ts.VariableDeclarationList): boolean;
export declare function isBlockScopedVariableDeclaration(declaration: ts.VariableDeclaration): boolean;
export declare const enum ScopeBoundary {
    None = 0,
    Function = 1,
    Block = 2,
}
export declare function isScopeBoundary(node: ts.Node): ScopeBoundary;
export declare function isFunctionScopeBoundary(node: ts.Node): boolean;
export declare function isBlockScopeBoundary(node: ts.Node): boolean;
export declare function hasOwnThisReference(node: ts.Node): boolean;
export declare function isFunctionWithBody(node: ts.Node): node is ts.FunctionLikeDeclaration;
export declare function forEachToken(node: ts.Node, cb: (node: ts.Node) => void, sourceFile?: ts.SourceFile): void;
export declare type ForEachTokenCallback = (fullText: string, kind: ts.SyntaxKind, range: ts.TextRange, parent: ts.Node) => void;
export declare function forEachTokenWithTrivia(node: ts.Node, cb: ForEachTokenCallback, sourceFile?: ts.SourceFile): void;
export declare type ForEachCommentCallback = (fullText: string, comment: ts.CommentRange) => void;
export declare function forEachComment(node: ts.Node, cb: ForEachCommentCallback, sourceFile?: ts.SourceFile): void;
export declare function endsControlFlow(statement: ts.Statement | ts.BlockLike): boolean;
export interface LineRange extends ts.TextRange {
    contentLength: number;
}
export declare function getLineRanges(sourceFile: ts.SourceFile): LineRange[];
export declare function isValidIdentifier(text: string): boolean;
export declare function isValidPropertyAccess(text: string): boolean;
export declare function isValidPropertyName(text: string): boolean;
export declare function isValidNumericLiteral(text: string): boolean;
export declare function isSameLine(sourceFile: ts.SourceFile, pos1: number, pos2: number): boolean;
