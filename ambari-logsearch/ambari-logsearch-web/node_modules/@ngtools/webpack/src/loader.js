"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const ts = require("typescript");
const plugin_1 = require("./plugin");
const refactor_1 = require("./refactor");
const loaderUtils = require('loader-utils');
const NormalModule = require('webpack/lib/NormalModule');
// This is a map of changes which need to be made
const changeMap = {
    platformBrowserDynamic: {
        name: 'platformBrowser',
        importLocation: '@angular/platform-browser'
    },
    platformDynamicServer: {
        name: 'platformServer',
        importLocation: '@angular/platform-server'
    }
};
function _getContentOfKeyLiteral(_source, node) {
    if (!node) {
        return null;
    }
    else if (node.kind == ts.SyntaxKind.Identifier) {
        return node.text;
    }
    else if (node.kind == ts.SyntaxKind.StringLiteral) {
        return node.text;
    }
    else {
        return null;
    }
}
function _angularImportsFromNode(node, _sourceFile) {
    const ms = node.moduleSpecifier;
    let modulePath = null;
    switch (ms.kind) {
        case ts.SyntaxKind.StringLiteral:
            modulePath = ms.text;
            break;
        default:
            return [];
    }
    if (!modulePath.startsWith('@angular/')) {
        return [];
    }
    if (node.importClause) {
        if (node.importClause.name) {
            // This is of the form `import Name from 'path'`. Ignore.
            return [];
        }
        else if (node.importClause.namedBindings) {
            const nb = node.importClause.namedBindings;
            if (nb.kind == ts.SyntaxKind.NamespaceImport) {
                // This is of the form `import * as name from 'path'`. Return `name.`.
                return [nb.name.text + '.'];
            }
            else {
                // This is of the form `import {a,b,c} from 'path'`
                const namedImports = nb;
                return namedImports.elements
                    .map((is) => is.propertyName ? is.propertyName.text : is.name.text);
            }
        }
    }
    else {
        // This is of the form `import 'path';`. Nothing to do.
        return [];
    }
    return [];
}
function _ctorParameterFromTypeReference(paramNode, angularImports, refactor) {
    let typeName = 'undefined';
    if (paramNode.type) {
        switch (paramNode.type.kind) {
            case ts.SyntaxKind.TypeReference:
                const type = paramNode.type;
                if (type.typeName) {
                    typeName = type.typeName.getText(refactor.sourceFile);
                }
                else {
                    typeName = type.getText(refactor.sourceFile);
                }
                break;
            case ts.SyntaxKind.AnyKeyword:
                typeName = 'undefined';
                break;
            default:
                typeName = 'null';
        }
    }
    const decorators = refactor.findAstNodes(paramNode, ts.SyntaxKind.Decorator);
    const decoratorStr = decorators
        .map(decorator => {
        const call = refactor.findFirstAstNode(decorator, ts.SyntaxKind.CallExpression);
        if (!call) {
            return null;
        }
        const fnName = call.expression.getText(refactor.sourceFile);
        const args = call.arguments.map(x => x.getText(refactor.sourceFile)).join(', ');
        if (angularImports.indexOf(fnName) === -1) {
            return null;
        }
        else {
            return [fnName, args];
        }
    })
        .filter(x => !!x)
        .map(([name, args]) => {
        if (args) {
            return `{ type: ${name}, args: [${args}] }`;
        }
        return `{ type: ${name} }`;
    })
        .join(', ');
    if (decorators.length > 0) {
        return `{ type: ${typeName}, decorators: [${decoratorStr}] }`;
    }
    return `{ type: ${typeName} }`;
}
function _addCtorParameters(classNode, angularImports, refactor) {
    // For every classes with constructors, output the ctorParameters function which contains a list
    // of injectable types.
    const ctor = refactor.findFirstAstNode(classNode, ts.SyntaxKind.Constructor);
    if (!ctor) {
        // A class can be missing a constructor, and that's _okay_.
        return;
    }
    const params = Array.from(ctor.parameters).map(paramNode => {
        return _ctorParameterFromTypeReference(paramNode, angularImports, refactor);
    });
    const ctorParametersDecl = `static ctorParameters() { return [ ${params.join(', ')} ]; }`;
    refactor.prependBefore(classNode.getLastToken(refactor.sourceFile), ctorParametersDecl);
}
function _removeDecorators(refactor) {
    const angularImports = refactor.findAstNodes(refactor.sourceFile, ts.SyntaxKind.ImportDeclaration)
        .map((node) => _angularImportsFromNode(node, refactor.sourceFile))
        .reduce((acc, current) => acc.concat(current), []);
    // Find all decorators.
    refactor.findAstNodes(refactor.sourceFile, ts.SyntaxKind.Decorator)
        .forEach(node => {
        // First, add decorators to classes to the classes array.
        if (node.parent) {
            const declarations = refactor.findAstNodes(node.parent, ts.SyntaxKind.ClassDeclaration, false, 1);
            if (declarations.length > 0) {
                _addCtorParameters(declarations[0], angularImports, refactor);
            }
        }
        refactor.findAstNodes(node, ts.SyntaxKind.CallExpression)
            .filter((node) => {
            const fnName = node.expression.getText(refactor.sourceFile);
            if (fnName.indexOf('.') != -1) {
                // Since this is `a.b`, see if it's the same namespace as a namespace import.
                return angularImports.indexOf(fnName.replace(/\..*$/, '') + '.') != -1;
            }
            else {
                return angularImports.indexOf(fnName) != -1;
            }
        })
            .forEach(() => refactor.removeNode(node));
    });
}
function _getNgFactoryPath(plugin, refactor) {
    // Calculate the base path.
    const basePath = path.normalize(plugin.basePath);
    const genDir = path.normalize(plugin.genDir);
    const dirName = path.normalize(path.dirname(refactor.fileName));
    const entryModule = plugin.entryModule;
    const entryModuleFileName = path.normalize(entryModule.path + '.ngfactory');
    const relativeEntryModulePath = path.relative(basePath, entryModuleFileName);
    const fullEntryModulePath = path.resolve(genDir, relativeEntryModulePath);
    const relativeNgFactoryPath = path.relative(dirName, fullEntryModulePath);
    return './' + relativeNgFactoryPath.replace(/\\/g, '/');
}
function _replacePlatform(refactor, bootstrapCall) {
    const platforms = refactor.findAstNodes(bootstrapCall, ts.SyntaxKind.CallExpression, true)
        .filter(call => {
        return call.expression.kind == ts.SyntaxKind.Identifier;
    })
        .filter(call => !!changeMap[call.expression.text]);
    platforms.forEach(call => {
        const platform = changeMap[call.expression.text];
        // Replace with mapped replacement
        refactor.replaceNode(call.expression, platform.name);
        // Add the appropriate import
        refactor.insertImport(platform.name, platform.importLocation);
    });
}
function _replaceBootstrapOrRender(refactor, call) {
    // If neither bootstrapModule or renderModule can't be found, bail out early.
    let replacementTarget;
    let identifier;
    if (call.getText().includes('bootstrapModule')) {
        if (call.expression.kind != ts.SyntaxKind.PropertyAccessExpression) {
            return;
        }
        replacementTarget = 'bootstrapModule';
        const access = call.expression;
        identifier = access.name;
        _replacePlatform(refactor, access);
    }
    else if (call.getText().includes('renderModule')) {
        if (call.expression.kind != ts.SyntaxKind.Identifier) {
            return;
        }
        replacementTarget = 'renderModule';
        identifier = call.expression;
        refactor.insertImport('renderModuleFactory', '@angular/platform-server');
    }
    if (identifier && identifier.text === replacementTarget) {
        refactor.replaceNode(identifier, replacementTarget + 'Factory');
    }
}
function _getCaller(node) {
    while (node.parent) {
        node = node.parent;
        if (node.kind === ts.SyntaxKind.CallExpression) {
            return node;
        }
    }
    return null;
}
function _replaceEntryModule(plugin, refactor) {
    const modules = refactor.findAstNodes(refactor.sourceFile, ts.SyntaxKind.Identifier, true)
        .filter(identifier => identifier.getText() === plugin.entryModule.className)
        .filter(identifier => identifier.parent &&
        (identifier.parent.kind === ts.SyntaxKind.CallExpression ||
            identifier.parent.kind === ts.SyntaxKind.PropertyAssignment))
        .filter(node => !!_getCaller(node));
    if (modules.length == 0) {
        return;
    }
    const factoryClassName = plugin.entryModule.className + 'NgFactory';
    refactor.insertImport(factoryClassName, _getNgFactoryPath(plugin, refactor));
    modules
        .forEach(reference => {
        refactor.replaceNode(reference, factoryClassName);
        const caller = _getCaller(reference);
        if (caller) {
            _replaceBootstrapOrRender(refactor, caller);
        }
    });
}
function _refactorBootstrap(plugin, refactor) {
    const genDir = path.normalize(plugin.genDir);
    const dirName = path.normalize(path.dirname(refactor.fileName));
    // Bail if in the generated directory
    if (dirName.startsWith(genDir)) {
        return;
    }
    _replaceEntryModule(plugin, refactor);
}
function removeModuleIdOnlyForTesting(refactor) {
    _removeModuleId(refactor);
}
exports.removeModuleIdOnlyForTesting = removeModuleIdOnlyForTesting;
function _removeModuleId(refactor) {
    const sourceFile = refactor.sourceFile;
    refactor.findAstNodes(sourceFile, ts.SyntaxKind.Decorator, true)
        .reduce((acc, node) => {
        return acc.concat(refactor.findAstNodes(node, ts.SyntaxKind.ObjectLiteralExpression, true));
    }, new Array())
        .filter((node) => {
        return node.properties.some(prop => {
            return prop.kind == ts.SyntaxKind.PropertyAssignment
                && _getContentOfKeyLiteral(sourceFile, prop.name) == 'moduleId';
        });
    })
        .forEach((node) => {
        const moduleIdProp = node.properties.filter((prop, _idx) => {
            return prop.kind == ts.SyntaxKind.PropertyAssignment
                && _getContentOfKeyLiteral(sourceFile, prop.name) == 'moduleId';
        })[0];
        // Get the trailing comma.
        const moduleIdCommaProp = moduleIdProp.parent
            ? moduleIdProp.parent.getChildAt(1).getChildren()[1] : null;
        refactor.removeNodes(moduleIdProp, moduleIdCommaProp);
    });
}
function _getResourceRequest(element, sourceFile) {
    if (element.kind == ts.SyntaxKind.StringLiteral) {
        const url = element.text;
        // If the URL does not start with ./ or ../, prepends ./ to it.
        return `'${/^\.?\.\//.test(url) ? '' : './'}${url}'`;
    }
    else {
        // if not string, just use expression directly
        return element.getFullText(sourceFile);
    }
}
function _replaceResources(refactor) {
    const sourceFile = refactor.sourceFile;
    _getResourceNodes(refactor)
        .forEach((node) => {
        const key = _getContentOfKeyLiteral(sourceFile, node.name);
        if (key == 'templateUrl') {
            refactor.replaceNode(node, `template: require(${_getResourceRequest(node.initializer, sourceFile)})`);
        }
        else if (key == 'styleUrls') {
            const arr = (refactor.findAstNodes(node, ts.SyntaxKind.ArrayLiteralExpression, false));
            if (!arr || arr.length == 0 || arr[0].elements.length == 0) {
                return;
            }
            const initializer = arr[0].elements.map((element) => {
                return _getResourceRequest(element, sourceFile);
            });
            refactor.replaceNode(node, `styles: [require(${initializer.join('), require(')})]`);
        }
    });
}
function _getResourceNodes(refactor) {
    const { sourceFile } = refactor;
    // Find all object literals.
    return refactor.findAstNodes(sourceFile, ts.SyntaxKind.ObjectLiteralExpression, true)
        .map(node => refactor.findAstNodes(node, ts.SyntaxKind.PropertyAssignment))
        .reduce((prev, curr) => curr ? prev.concat(curr) : prev, [])
        .filter((node) => {
        const key = _getContentOfKeyLiteral(sourceFile, node.name);
        if (!key) {
            // key is an expression, can't do anything.
            return false;
        }
        return key == 'templateUrl' || key == 'styleUrls';
    });
}
function _getResourcesUrls(refactor) {
    return _getResourceNodes(refactor)
        .reduce((acc, node) => {
        const key = _getContentOfKeyLiteral(refactor.sourceFile, node.name);
        if (key == 'templateUrl') {
            const url = node.initializer.text;
            if (url) {
                acc.push(url);
            }
        }
        else if (key == 'styleUrls') {
            const arr = (refactor.findAstNodes(node, ts.SyntaxKind.ArrayLiteralExpression, false));
            if (!arr || arr.length == 0 || arr[0].elements.length == 0) {
                return acc;
            }
            arr[0].elements.forEach((element) => {
                if (element.kind == ts.SyntaxKind.StringLiteral) {
                    const url = element.text;
                    if (url) {
                        acc.push(url);
                    }
                }
            });
        }
        return acc;
    }, []);
}
/**
 * Recursively calls diagnose on the plugins for all the reverse dependencies.
 * @private
 */
function _diagnoseDeps(reasons, plugin, checked) {
    reasons
        .filter(reason => reason && reason.module && reason.module instanceof NormalModule)
        .filter(reason => !checked.has(reason.module.resource))
        .forEach(reason => {
        checked.add(reason.module.resource);
        plugin.diagnose(reason.module.resource);
        _diagnoseDeps(reason.module.reasons, plugin, checked);
    });
}
function _getModuleExports(plugin, refactor) {
    const exports = refactor
        .findAstNodes(refactor.sourceFile, ts.SyntaxKind.ExportDeclaration, true);
    return exports
        .filter(node => {
        const identifiers = refactor.findAstNodes(node, ts.SyntaxKind.Identifier, false)
            .filter(node => node.getText() === plugin.entryModule.className);
        return identifiers.length > 0;
    });
}
exports._getModuleExports = _getModuleExports;
function _replaceExport(plugin, refactor) {
    if (!plugin.replaceExport) {
        return;
    }
    _getModuleExports(plugin, refactor)
        .forEach(node => {
        const factoryPath = _getNgFactoryPath(plugin, refactor);
        const factoryClassName = plugin.entryModule.className + 'NgFactory';
        const exportStatement = `export \{ ${factoryClassName} \} from '${factoryPath}'`;
        refactor.appendAfter(node, exportStatement);
    });
}
exports._replaceExport = _replaceExport;
function _exportModuleMap(plugin, refactor) {
    if (!plugin.replaceExport) {
        return;
    }
    const dirName = path.normalize(path.dirname(refactor.fileName));
    const classNameAppend = plugin.skipCodeGeneration ? '' : 'NgFactory';
    const modulePathAppend = plugin.skipCodeGeneration ? '' : '.ngfactory';
    _getModuleExports(plugin, refactor)
        .forEach(node => {
        const modules = Object.keys(plugin.discoveredLazyRoutes)
            .map((loadChildrenString) => {
            let [lazyRouteKey, moduleName] = loadChildrenString.split('#');
            if (!lazyRouteKey || !moduleName) {
                throw new Error(`${loadChildrenString} was not a proper loadChildren string`);
            }
            moduleName += classNameAppend;
            lazyRouteKey += modulePathAppend;
            const modulePath = plugin.lazyRoutes[lazyRouteKey];
            return {
                modulePath,
                moduleName,
                loadChildrenString
            };
        });
        modules.forEach((module, index) => {
            const relativePath = path.relative(dirName, module.modulePath).replace(/\\/g, '/');
            refactor.prependBefore(node, `import * as __lazy_${index}__ from './${relativePath}'`);
        });
        const jsonContent = modules
            .map((module, index) => `"${module.loadChildrenString}": __lazy_${index}__.${module.moduleName}`)
            .join();
        refactor.appendAfter(node, `export const LAZY_MODULE_MAP = {${jsonContent}};`);
    });
}
exports._exportModuleMap = _exportModuleMap;
// Super simple TS transpiler loader for testing / isolated usage. does not type check!
function ngcLoader(source) {
    const cb = this.async();
    const sourceFileName = this.resourcePath;
    const plugin = this._compilation._ngToolsWebpackPluginInstance;
    if (plugin) {
        // We must verify that AotPlugin is an instance of the right class.
        // Throw an error if it isn't, that often means multiple @ngtools/webpack installs.
        if (!(plugin instanceof plugin_1.AotPlugin)) {
            throw new Error('AotPlugin was detected but it was an instance of the wrong class.\n'
                + 'This likely means you have several @ngtools/webpack packages installed. '
                + 'You can check this with `npm ls @ngtools/webpack`, and then remove the extra copies.');
        }
        if (plugin.compilerHost.readFile(sourceFileName) == source) {
            // In the case where the source is the same as the one in compilerHost, we don't have
            // extra TS loaders and there's no need to do any trickery.
            source = null;
        }
        const refactor = new refactor_1.TypeScriptFileRefactor(sourceFileName, plugin.compilerHost, plugin.program, source);
        Promise.resolve()
            .then(() => {
            if (!plugin.skipCodeGeneration) {
                return Promise.resolve()
                    .then(() => _removeDecorators(refactor))
                    .then(() => _refactorBootstrap(plugin, refactor))
                    .then(() => _replaceExport(plugin, refactor))
                    .then(() => _exportModuleMap(plugin, refactor));
            }
            else {
                return Promise.resolve()
                    .then(() => _replaceResources(refactor))
                    .then(() => _removeModuleId(refactor))
                    .then(() => _exportModuleMap(plugin, refactor));
            }
        })
            .then(() => {
            if (plugin.typeCheck) {
                // Check all diagnostics from this and reverse dependencies also.
                if (!plugin.firstRun) {
                    _diagnoseDeps(this._module.reasons, plugin, new Set());
                }
                // We do this here because it will throw on error, resulting in rebuilding this file
                // the next time around if it changes.
                plugin.diagnose(sourceFileName);
            }
        })
            .then(() => {
            // Add resources as dependencies.
            _getResourcesUrls(refactor).forEach((url) => {
                this.addDependency(path.resolve(path.dirname(sourceFileName), url));
            });
        })
            .then(() => {
            if (source) {
                // We need to validate diagnostics. We ignore type checking though, to save time.
                const diagnostics = refactor.getDiagnostics(false);
                if (diagnostics.length) {
                    let message = '';
                    diagnostics.forEach(diagnostic => {
                        const messageText = ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n');
                        if (diagnostic.file) {
                            const position = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
                            const fileName = diagnostic.file.fileName;
                            const { line, character } = position;
                            message += `${fileName} (${line + 1},${character + 1}): ${messageText}\n`;
                        }
                        else {
                            message += `${messageText}\n`;
                        }
                    });
                    throw new Error(message);
                }
            }
            // Force a few compiler options to make sure we get the result we want.
            const compilerOptions = Object.assign({}, plugin.compilerOptions, {
                inlineSources: true,
                inlineSourceMap: false,
                sourceRoot: plugin.basePath
            });
            const result = refactor.transpile(compilerOptions);
            if (plugin.failedCompilation) {
                // Return an empty string to prevent extra loader errors (missing imports etc).
                // Plugin errors were already pushed to the compilation errors.
                cb(null, '');
            }
            else {
                cb(null, result.outputText, result.sourceMap);
            }
        })
            .catch(err => cb(err));
    }
    else {
        const options = loaderUtils.getOptions(this) || {};
        const tsConfigPath = options.tsConfigPath;
        if (tsConfigPath === undefined) {
            throw new Error('@ngtools/webpack is being used as a loader but no `tsConfigPath` option nor '
                + 'AotPlugin was detected. You must provide at least one of these.');
        }
        const tsConfig = ts.readConfigFile(tsConfigPath, ts.sys.readFile);
        if (tsConfig.error) {
            throw tsConfig.error;
        }
        const compilerOptions = tsConfig.config.compilerOptions;
        for (const key of Object.keys(options)) {
            if (key == 'tsConfigPath') {
                continue;
            }
            compilerOptions[key] = options[key];
        }
        const compilerHost = ts.createCompilerHost(compilerOptions);
        const refactor = new refactor_1.TypeScriptFileRefactor(sourceFileName, compilerHost);
        _replaceResources(refactor);
        const result = refactor.transpile(compilerOptions);
        // Webpack is going to take care of this.
        result.outputText = result.outputText.replace(/^\/\/# sourceMappingURL=[^\r\n]*/gm, '');
        cb(null, result.outputText, result.sourceMap);
    }
}
exports.ngcLoader = ngcLoader;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/loader.js.map