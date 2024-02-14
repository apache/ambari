import ts = require('typescript');
import { Change } from './change';
/**
 * Find all nodes from the AST in the subtree of node of SyntaxKind kind.
 * @param node
 * @param kind
 * @param max The maximum number of items to return.
 * @return all nodes of kind, or [] if none is found
 */
export declare function findNodes(node: ts.Node, kind: ts.SyntaxKind, max?: number): ts.Node[];
export declare function removeAstNode(node: ts.Node): Change;
