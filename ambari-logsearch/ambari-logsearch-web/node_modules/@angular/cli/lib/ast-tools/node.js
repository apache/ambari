"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const change_1 = require("./change");
/**
 * Find all nodes from the AST in the subtree of node of SyntaxKind kind.
 * @param node
 * @param kind
 * @param max The maximum number of items to return.
 * @return all nodes of kind, or [] if none is found
 */
function findNodes(node, kind, max = Infinity) {
    if (!node || max == 0) {
        return [];
    }
    let arr = [];
    if (node.kind === kind) {
        arr.push(node);
        max--;
    }
    if (max > 0) {
        for (const child of node.getChildren()) {
            findNodes(child, kind, max).forEach(node => {
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
exports.findNodes = findNodes;
function removeAstNode(node) {
    const source = node.getSourceFile();
    return new change_1.RemoveChange(source.path, node.getStart(source), node.getFullText(source));
}
exports.removeAstNode = removeAstNode;
//# sourceMappingURL=/users/hansl/sources/angular-cli/lib/ast-tools/node.js.map