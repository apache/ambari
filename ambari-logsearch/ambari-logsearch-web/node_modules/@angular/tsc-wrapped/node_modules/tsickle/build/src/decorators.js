/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
"use strict";
var ts = require("typescript");
/**
 * Returns the declarations for the given decorator.
 */
function getDecoratorDeclarations(decorator, typeChecker) {
    // Walk down the expression to find the identifier of the decorator function.
    var node = decorator;
    while (node.kind !== ts.SyntaxKind.Identifier) {
        if (node.kind === ts.SyntaxKind.Decorator || node.kind === ts.SyntaxKind.CallExpression) {
            node = node.expression;
        }
        else {
            // We do not know how to handle this type of decorator.
            return [];
        }
    }
    var decSym = typeChecker.getSymbolAtLocation(node);
    if (decSym.flags & ts.SymbolFlags.Alias) {
        decSym = typeChecker.getAliasedSymbol(decSym);
    }
    return decSym.getDeclarations();
}
exports.getDecoratorDeclarations = getDecoratorDeclarations;
/**
 * Returns true if node has an exporting decorator  (i.e., a decorator with @ExportDecoratedItems
 * in its JSDoc).
 */
function hasExportingDecorator(node, typeChecker) {
    return node.decorators &&
        node.decorators.some(function (decorator) { return isExportingDecorator(decorator, typeChecker); });
}
exports.hasExportingDecorator = hasExportingDecorator;
/**
 * Returns true if the given decorator has an @ExportDecoratedItems directive in its JSDoc.
 */
function isExportingDecorator(decorator, typeChecker) {
    return getDecoratorDeclarations(decorator, typeChecker).some(function (declaration) {
        var range = ts.getLeadingCommentRanges(declaration.getFullText(), 0);
        if (!range) {
            return false;
        }
        for (var _i = 0, range_1 = range; _i < range_1.length; _i++) {
            var _a = range_1[_i], pos = _a.pos, end = _a.end;
            if (/@ExportDecoratedItems\b/.test(declaration.getFullText().substring(pos, end))) {
                return true;
            }
        }
        return false;
    });
}

//# sourceMappingURL=decorators.js.map
