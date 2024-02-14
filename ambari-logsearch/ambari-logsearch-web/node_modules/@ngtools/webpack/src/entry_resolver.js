"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const path_1 = require("path");
const ts = require("typescript");
const refactor_1 = require("./refactor");
function _recursiveSymbolExportLookup(refactor, symbolName, host, program) {
    // Check this file.
    const hasSymbol = refactor.findAstNodes(null, ts.SyntaxKind.ClassDeclaration)
        .some((cd) => {
        return cd.name != undefined && cd.name.text == symbolName;
    });
    if (hasSymbol) {
        return refactor.fileName;
    }
    // We found the bootstrap variable, now we just need to get where it's imported.
    const exports = refactor.findAstNodes(null, ts.SyntaxKind.ExportDeclaration)
        .map(node => node);
    for (const decl of exports) {
        if (!decl.moduleSpecifier || decl.moduleSpecifier.kind !== ts.SyntaxKind.StringLiteral) {
            continue;
        }
        const modulePath = decl.moduleSpecifier.text;
        const resolvedModule = ts.resolveModuleName(modulePath, refactor.fileName, program.getCompilerOptions(), host);
        if (!resolvedModule.resolvedModule || !resolvedModule.resolvedModule.resolvedFileName) {
            return null;
        }
        const module = resolvedModule.resolvedModule.resolvedFileName;
        if (!decl.exportClause) {
            const moduleRefactor = new refactor_1.TypeScriptFileRefactor(module, host, program);
            const maybeModule = _recursiveSymbolExportLookup(moduleRefactor, symbolName, host, program);
            if (maybeModule) {
                return maybeModule;
            }
            continue;
        }
        const binding = decl.exportClause;
        for (const specifier of binding.elements) {
            if (specifier.name.text == symbolName) {
                // If it's a directory, load its index and recursively lookup.
                if (fs.statSync(module).isDirectory()) {
                    const indexModule = path_1.join(module, 'index.ts');
                    if (fs.existsSync(indexModule)) {
                        const indexRefactor = new refactor_1.TypeScriptFileRefactor(indexModule, host, program);
                        const maybeModule = _recursiveSymbolExportLookup(indexRefactor, symbolName, host, program);
                        if (maybeModule) {
                            return maybeModule;
                        }
                    }
                }
                // Create the source and verify that the symbol is at least a class.
                const source = new refactor_1.TypeScriptFileRefactor(module, host, program);
                const hasSymbol = source.findAstNodes(null, ts.SyntaxKind.ClassDeclaration)
                    .some((cd) => {
                    return cd.name != undefined && cd.name.text == symbolName;
                });
                if (hasSymbol) {
                    return module;
                }
            }
        }
    }
    return null;
}
function _symbolImportLookup(refactor, symbolName, host, program) {
    // We found the bootstrap variable, now we just need to get where it's imported.
    const imports = refactor.findAstNodes(null, ts.SyntaxKind.ImportDeclaration)
        .map(node => node);
    for (const decl of imports) {
        if (!decl.importClause || !decl.moduleSpecifier) {
            continue;
        }
        if (decl.moduleSpecifier.kind !== ts.SyntaxKind.StringLiteral) {
            continue;
        }
        const resolvedModule = ts.resolveModuleName(decl.moduleSpecifier.text, refactor.fileName, program.getCompilerOptions(), host);
        if (!resolvedModule.resolvedModule || !resolvedModule.resolvedModule.resolvedFileName) {
            continue;
        }
        const module = resolvedModule.resolvedModule.resolvedFileName;
        if (decl.importClause.namedBindings
            && decl.importClause.namedBindings.kind == ts.SyntaxKind.NamespaceImport) {
            const binding = decl.importClause.namedBindings;
            if (binding.name.text == symbolName) {
                // This is a default export.
                return module;
            }
        }
        else if (decl.importClause.namedBindings
            && decl.importClause.namedBindings.kind == ts.SyntaxKind.NamedImports) {
            const binding = decl.importClause.namedBindings;
            for (const specifier of binding.elements) {
                if (specifier.name.text == symbolName) {
                    // Create the source and recursively lookup the import.
                    const source = new refactor_1.TypeScriptFileRefactor(module, host, program);
                    const maybeModule = _recursiveSymbolExportLookup(source, symbolName, host, program);
                    if (maybeModule) {
                        return maybeModule;
                    }
                }
            }
        }
    }
    return null;
}
function resolveEntryModuleFromMain(mainPath, host, program) {
    const source = new refactor_1.TypeScriptFileRefactor(mainPath, host, program);
    const bootstrap = source.findAstNodes(source.sourceFile, ts.SyntaxKind.CallExpression, true)
        .map(node => node)
        .filter(call => {
        const access = call.expression;
        return access.kind == ts.SyntaxKind.PropertyAccessExpression
            && access.name.kind == ts.SyntaxKind.Identifier
            && (access.name.text == 'bootstrapModule'
                || access.name.text == 'bootstrapModuleFactory');
    })
        .map(node => node.arguments[0])
        .filter(node => node.kind == ts.SyntaxKind.Identifier);
    if (bootstrap.length != 1) {
        throw new Error('Tried to find bootstrap code, but could not. Specify either '
            + 'statically analyzable bootstrap code or pass in an entryModule '
            + 'to the plugins options.');
    }
    const bootstrapSymbolName = bootstrap[0].text;
    const module = _symbolImportLookup(source, bootstrapSymbolName, host, program);
    if (module) {
        return `${module.replace(/\.ts$/, '')}#${bootstrapSymbolName}`;
    }
    // shrug... something bad happened and we couldn't find the import statement.
    throw new Error('Tried to find bootstrap code, but could not. Specify either '
        + 'statically analyzable bootstrap code or pass in an entryModule '
        + 'to the plugins options.');
}
exports.resolveEntryModuleFromMain = resolveEntryModuleFromMain;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/entry_resolver.js.map