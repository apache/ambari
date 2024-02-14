"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path_1 = require("path");
const ts = require("typescript");
const refactor_1 = require("./refactor");
function _getContentOfKeyLiteral(_source, node) {
    if (node.kind == ts.SyntaxKind.Identifier) {
        return node.text;
    }
    else if (node.kind == ts.SyntaxKind.StringLiteral) {
        return node.text;
    }
    else {
        return null;
    }
}
function findLazyRoutes(filePath, program, host) {
    const refactor = new refactor_1.TypeScriptFileRefactor(filePath, host, program);
    return refactor
        .findAstNodes(null, ts.SyntaxKind.ObjectLiteralExpression, true)
        .map((node) => {
        return refactor.findAstNodes(node, ts.SyntaxKind.PropertyAssignment, false);
    })
        .reduce((acc, props) => {
        return acc.concat(props.filter(literal => {
            return _getContentOfKeyLiteral(refactor.sourceFile, literal.name) == 'loadChildren';
        }));
    }, [])
        .filter((node) => node.initializer.kind == ts.SyntaxKind.StringLiteral)
        .map((node) => node.initializer.text)
        .map((routePath) => {
        const moduleName = routePath.split('#')[0];
        const resolvedModuleName = moduleName[0] == '.'
            ? {
                resolvedModule: { resolvedFileName: path_1.join(path_1.dirname(filePath), moduleName) + '.ts' }
            }
            : ts.resolveModuleName(moduleName, filePath, program.getCompilerOptions(), host);
        if (resolvedModuleName.resolvedModule
            && resolvedModuleName.resolvedModule.resolvedFileName
            && host.fileExists(resolvedModuleName.resolvedModule.resolvedFileName)) {
            return [routePath, resolvedModuleName.resolvedModule.resolvedFileName];
        }
        else {
            return [routePath, null];
        }
    })
        .reduce((acc, [routePath, resolvedModuleName]) => {
        acc[routePath] = resolvedModuleName;
        return acc;
    }, {});
}
exports.findLazyRoutes = findLazyRoutes;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/lazy_routes.js.map