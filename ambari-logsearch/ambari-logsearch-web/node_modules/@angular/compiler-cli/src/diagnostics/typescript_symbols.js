"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var ts = require("typescript");
var symbols_1 = require("./symbols");
// In TypeScript 2.1 these flags moved
// These helpers work for both 2.0 and 2.1.
var isPrivate = ts.ModifierFlags ?
    (function (node) {
        return !!(ts.getCombinedModifierFlags(node) & ts.ModifierFlags.Private);
    }) :
    (function (node) { return !!(node.flags & ts.NodeFlags.Private); });
var isReferenceType = ts.ObjectFlags ?
    (function (type) {
        return !!(type.flags & ts.TypeFlags.Object &&
            type.objectFlags & ts.ObjectFlags.Reference);
    }) :
    (function (type) { return !!(type.flags & ts.TypeFlags.Reference); });
function getSymbolQuery(program, checker, source, fetchPipes) {
    return new TypeScriptSymbolQuery(program, checker, source, fetchPipes);
}
exports.getSymbolQuery = getSymbolQuery;
function getClassMembers(program, checker, staticSymbol) {
    var declaration = getClassFromStaticSymbol(program, staticSymbol);
    if (declaration) {
        var type = checker.getTypeAtLocation(declaration);
        var node = program.getSourceFile(staticSymbol.filePath);
        return new TypeWrapper(type, { node: node, program: program, checker: checker }).members();
    }
}
exports.getClassMembers = getClassMembers;
function getClassMembersFromDeclaration(program, checker, source, declaration) {
    var type = checker.getTypeAtLocation(declaration);
    return new TypeWrapper(type, { node: source, program: program, checker: checker }).members();
}
exports.getClassMembersFromDeclaration = getClassMembersFromDeclaration;
function getClassFromStaticSymbol(program, type) {
    var source = program.getSourceFile(type.filePath);
    if (source) {
        return ts.forEachChild(source, function (child) {
            if (child.kind === ts.SyntaxKind.ClassDeclaration) {
                var classDeclaration = child;
                if (classDeclaration.name != null && classDeclaration.name.text === type.name) {
                    return classDeclaration;
                }
            }
        });
    }
    return undefined;
}
exports.getClassFromStaticSymbol = getClassFromStaticSymbol;
function getPipesTable(source, program, checker, pipes) {
    return new PipesTable(pipes, { program: program, checker: checker, node: source });
}
exports.getPipesTable = getPipesTable;
var TypeScriptSymbolQuery = (function () {
    function TypeScriptSymbolQuery(program, checker, source, fetchPipes) {
        this.program = program;
        this.checker = checker;
        this.source = source;
        this.fetchPipes = fetchPipes;
        this.typeCache = new Map();
    }
    TypeScriptSymbolQuery.prototype.getTypeKind = function (symbol) { return typeKindOf(this.getTsTypeOf(symbol)); };
    TypeScriptSymbolQuery.prototype.getBuiltinType = function (kind) {
        var result = this.typeCache.get(kind);
        if (!result) {
            var type = getBuiltinTypeFromTs(kind, { checker: this.checker, node: this.source, program: this.program });
            result =
                new TypeWrapper(type, { program: this.program, checker: this.checker, node: this.source });
            this.typeCache.set(kind, result);
        }
        return result;
    };
    TypeScriptSymbolQuery.prototype.getTypeUnion = function () {
        var types = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            types[_i] = arguments[_i];
        }
        // No API exists so return any if the types are not all the same type.
        var result = undefined;
        if (types.length) {
            result = types[0];
            for (var i = 1; i < types.length; i++) {
                if (types[i] != result) {
                    result = undefined;
                    break;
                }
            }
        }
        return result || this.getBuiltinType(symbols_1.BuiltinType.Any);
    };
    TypeScriptSymbolQuery.prototype.getArrayType = function (type) { return this.getBuiltinType(symbols_1.BuiltinType.Any); };
    TypeScriptSymbolQuery.prototype.getElementType = function (type) {
        if (type instanceof TypeWrapper) {
            var elementType = getTypeParameterOf(type.tsType, 'Array');
            if (elementType) {
                return new TypeWrapper(elementType, type.context);
            }
        }
    };
    TypeScriptSymbolQuery.prototype.getNonNullableType = function (symbol) {
        if (symbol instanceof TypeWrapper && (typeof this.checker.getNonNullableType == 'function')) {
            var tsType = symbol.tsType;
            var nonNullableType = this.checker.getNonNullableType(tsType);
            if (nonNullableType != tsType) {
                return new TypeWrapper(nonNullableType, symbol.context);
            }
            else if (nonNullableType == tsType) {
                return symbol;
            }
        }
        return this.getBuiltinType(symbols_1.BuiltinType.Any);
    };
    TypeScriptSymbolQuery.prototype.getPipes = function () {
        var result = this.pipesCache;
        if (!result) {
            result = this.pipesCache = this.fetchPipes();
        }
        return result;
    };
    TypeScriptSymbolQuery.prototype.getTemplateContext = function (type) {
        var context = { node: this.source, program: this.program, checker: this.checker };
        var typeSymbol = findClassSymbolInContext(type, context);
        if (typeSymbol) {
            var contextType = this.getTemplateRefContextType(typeSymbol);
            if (contextType)
                return new SymbolWrapper(contextType, context).members();
        }
    };
    TypeScriptSymbolQuery.prototype.getTypeSymbol = function (type) {
        var context = { node: this.source, program: this.program, checker: this.checker };
        var typeSymbol = findClassSymbolInContext(type, context);
        return typeSymbol && new SymbolWrapper(typeSymbol, context);
    };
    TypeScriptSymbolQuery.prototype.createSymbolTable = function (symbols) {
        var result = new MapSymbolTable();
        result.addAll(symbols.map(function (s) { return new DeclaredSymbol(s); }));
        return result;
    };
    TypeScriptSymbolQuery.prototype.mergeSymbolTable = function (symbolTables) {
        var result = new MapSymbolTable();
        for (var _i = 0, symbolTables_1 = symbolTables; _i < symbolTables_1.length; _i++) {
            var symbolTable = symbolTables_1[_i];
            result.addAll(symbolTable.values());
        }
        return result;
    };
    TypeScriptSymbolQuery.prototype.getSpanAt = function (line, column) {
        return spanAt(this.source, line, column);
    };
    TypeScriptSymbolQuery.prototype.getTemplateRefContextType = function (typeSymbol) {
        var type = this.checker.getTypeOfSymbolAtLocation(typeSymbol, this.source);
        var constructor = type.symbol && type.symbol.members &&
            getFromSymbolTable(type.symbol.members, '__constructor');
        if (constructor) {
            var constructorDeclaration = constructor.declarations[0];
            for (var _i = 0, _a = constructorDeclaration.parameters; _i < _a.length; _i++) {
                var parameter = _a[_i];
                var type_1 = this.checker.getTypeAtLocation(parameter.type);
                if (type_1.symbol.name == 'TemplateRef' && isReferenceType(type_1)) {
                    var typeReference = type_1;
                    if (typeReference.typeArguments.length === 1) {
                        return typeReference.typeArguments[0].symbol;
                    }
                }
            }
        }
    };
    TypeScriptSymbolQuery.prototype.getTsTypeOf = function (symbol) {
        var type = this.getTypeWrapper(symbol);
        return type && type.tsType;
    };
    TypeScriptSymbolQuery.prototype.getTypeWrapper = function (symbol) {
        var type = undefined;
        if (symbol instanceof TypeWrapper) {
            type = symbol;
        }
        else if (symbol.type instanceof TypeWrapper) {
            type = symbol.type;
        }
        return type;
    };
    return TypeScriptSymbolQuery;
}());
function typeCallable(type) {
    var signatures = type.getCallSignatures();
    return signatures && signatures.length != 0;
}
function signaturesOf(type, context) {
    return type.getCallSignatures().map(function (s) { return new SignatureWrapper(s, context); });
}
function selectSignature(type, context, types) {
    // TODO: Do a better job of selecting the right signature.
    var signatures = type.getCallSignatures();
    return signatures.length ? new SignatureWrapper(signatures[0], context) : undefined;
}
var TypeWrapper = (function () {
    function TypeWrapper(tsType, context) {
        this.tsType = tsType;
        this.context = context;
        if (!tsType) {
            throw Error('Internal: null type');
        }
    }
    Object.defineProperty(TypeWrapper.prototype, "name", {
        get: function () {
            var symbol = this.tsType.symbol;
            return (symbol && symbol.name) || '<anonymous>';
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "kind", {
        get: function () { return 'type'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "language", {
        get: function () { return 'typescript'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "type", {
        get: function () { return undefined; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "container", {
        get: function () { return undefined; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "public", {
        get: function () { return true; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "callable", {
        get: function () { return typeCallable(this.tsType); },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "nullable", {
        get: function () {
            return this.context.checker.getNonNullableType(this.tsType) != this.tsType;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeWrapper.prototype, "definition", {
        get: function () { return definitionFromTsSymbol(this.tsType.getSymbol()); },
        enumerable: true,
        configurable: true
    });
    TypeWrapper.prototype.members = function () {
        return new SymbolTableWrapper(this.tsType.getProperties(), this.context);
    };
    TypeWrapper.prototype.signatures = function () { return signaturesOf(this.tsType, this.context); };
    TypeWrapper.prototype.selectSignature = function (types) {
        return selectSignature(this.tsType, this.context, types);
    };
    TypeWrapper.prototype.indexed = function (argument) { return undefined; };
    return TypeWrapper;
}());
var SymbolWrapper = (function () {
    function SymbolWrapper(symbol, context) {
        this.context = context;
        this.symbol = symbol && context && (symbol.flags & ts.SymbolFlags.Alias) ?
            context.checker.getAliasedSymbol(symbol) :
            symbol;
    }
    Object.defineProperty(SymbolWrapper.prototype, "name", {
        get: function () { return this.symbol.name; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "kind", {
        get: function () { return this.callable ? 'method' : 'property'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "language", {
        get: function () { return 'typescript'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "type", {
        get: function () { return new TypeWrapper(this.tsType, this.context); },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "container", {
        get: function () { return getContainerOf(this.symbol, this.context); },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "public", {
        get: function () {
            // Symbols that are not explicitly made private are public.
            return !isSymbolPrivate(this.symbol);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "callable", {
        get: function () { return typeCallable(this.tsType); },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "nullable", {
        get: function () { return false; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SymbolWrapper.prototype, "definition", {
        get: function () { return definitionFromTsSymbol(this.symbol); },
        enumerable: true,
        configurable: true
    });
    SymbolWrapper.prototype.members = function () {
        if (!this._members) {
            if ((this.symbol.flags & (ts.SymbolFlags.Class | ts.SymbolFlags.Interface)) != 0) {
                var declaredType = this.context.checker.getDeclaredTypeOfSymbol(this.symbol);
                var typeWrapper = new TypeWrapper(declaredType, this.context);
                this._members = typeWrapper.members();
            }
            else {
                this._members = new SymbolTableWrapper(this.symbol.members, this.context);
            }
        }
        return this._members;
    };
    SymbolWrapper.prototype.signatures = function () { return signaturesOf(this.tsType, this.context); };
    SymbolWrapper.prototype.selectSignature = function (types) {
        return selectSignature(this.tsType, this.context, types);
    };
    SymbolWrapper.prototype.indexed = function (argument) { return undefined; };
    Object.defineProperty(SymbolWrapper.prototype, "tsType", {
        get: function () {
            var type = this._tsType;
            if (!type) {
                type = this._tsType =
                    this.context.checker.getTypeOfSymbolAtLocation(this.symbol, this.context.node);
            }
            return type;
        },
        enumerable: true,
        configurable: true
    });
    return SymbolWrapper;
}());
var DeclaredSymbol = (function () {
    function DeclaredSymbol(declaration) {
        this.declaration = declaration;
    }
    Object.defineProperty(DeclaredSymbol.prototype, "name", {
        get: function () { return this.declaration.name; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "kind", {
        get: function () { return this.declaration.kind; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "language", {
        get: function () { return 'ng-template'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "container", {
        get: function () { return undefined; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "type", {
        get: function () { return this.declaration.type; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "callable", {
        get: function () { return this.declaration.type.callable; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "nullable", {
        get: function () { return false; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "public", {
        get: function () { return true; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(DeclaredSymbol.prototype, "definition", {
        get: function () { return this.declaration.definition; },
        enumerable: true,
        configurable: true
    });
    DeclaredSymbol.prototype.members = function () { return this.declaration.type.members(); };
    DeclaredSymbol.prototype.signatures = function () { return this.declaration.type.signatures(); };
    DeclaredSymbol.prototype.selectSignature = function (types) {
        return this.declaration.type.selectSignature(types);
    };
    DeclaredSymbol.prototype.indexed = function (argument) { return undefined; };
    return DeclaredSymbol;
}());
var SignatureWrapper = (function () {
    function SignatureWrapper(signature, context) {
        this.signature = signature;
        this.context = context;
    }
    Object.defineProperty(SignatureWrapper.prototype, "arguments", {
        get: function () {
            return new SymbolTableWrapper(this.signature.getParameters(), this.context);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SignatureWrapper.prototype, "result", {
        get: function () { return new TypeWrapper(this.signature.getReturnType(), this.context); },
        enumerable: true,
        configurable: true
    });
    return SignatureWrapper;
}());
var SignatureResultOverride = (function () {
    function SignatureResultOverride(signature, resultType) {
        this.signature = signature;
        this.resultType = resultType;
    }
    Object.defineProperty(SignatureResultOverride.prototype, "arguments", {
        get: function () { return this.signature.arguments; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SignatureResultOverride.prototype, "result", {
        get: function () { return this.resultType; },
        enumerable: true,
        configurable: true
    });
    return SignatureResultOverride;
}());
var toSymbolTable = isTypescriptVersion('2.2') ?
    (function (symbols) {
        var result = new Map();
        for (var _i = 0, symbols_2 = symbols; _i < symbols_2.length; _i++) {
            var symbol = symbols_2[_i];
            result.set(symbol.name, symbol);
        }
        return result;
    }) :
    (function (symbols) {
        var result = {};
        for (var _i = 0, symbols_3 = symbols; _i < symbols_3.length; _i++) {
            var symbol = symbols_3[_i];
            result[symbol.name] = symbol;
        }
        return result;
    });
function toSymbols(symbolTable) {
    if (!symbolTable)
        return [];
    var table = symbolTable;
    if (typeof table.values === 'function') {
        return Array.from(table.values());
    }
    var result = [];
    var own = typeof table.hasOwnProperty === 'function' ?
        function (name) { return table.hasOwnProperty(name); } :
        function (name) { return !!table[name]; };
    for (var name_1 in table) {
        if (own(name_1)) {
            result.push(table[name_1]);
        }
    }
    return result;
}
var SymbolTableWrapper = (function () {
    function SymbolTableWrapper(symbols, context) {
        this.context = context;
        symbols = symbols || [];
        if (Array.isArray(symbols)) {
            this.symbols = symbols;
            this.symbolTable = toSymbolTable(symbols);
        }
        else {
            this.symbols = toSymbols(symbols);
            this.symbolTable = symbols;
        }
    }
    Object.defineProperty(SymbolTableWrapper.prototype, "size", {
        get: function () { return this.symbols.length; },
        enumerable: true,
        configurable: true
    });
    SymbolTableWrapper.prototype.get = function (key) {
        var symbol = getFromSymbolTable(this.symbolTable, key);
        return symbol ? new SymbolWrapper(symbol, this.context) : undefined;
    };
    SymbolTableWrapper.prototype.has = function (key) {
        var table = this.symbolTable;
        return (typeof table.has === 'function') ? table.has(key) : table[key] != null;
    };
    SymbolTableWrapper.prototype.values = function () {
        var _this = this;
        return this.symbols.map(function (s) { return new SymbolWrapper(s, _this.context); });
    };
    return SymbolTableWrapper;
}());
var MapSymbolTable = (function () {
    function MapSymbolTable() {
        this.map = new Map();
        this._values = [];
    }
    Object.defineProperty(MapSymbolTable.prototype, "size", {
        get: function () { return this.map.size; },
        enumerable: true,
        configurable: true
    });
    MapSymbolTable.prototype.get = function (key) { return this.map.get(key); };
    MapSymbolTable.prototype.add = function (symbol) {
        if (this.map.has(symbol.name)) {
            var previous = this.map.get(symbol.name);
            this._values[this._values.indexOf(previous)] = symbol;
        }
        this.map.set(symbol.name, symbol);
        this._values.push(symbol);
    };
    MapSymbolTable.prototype.addAll = function (symbols) {
        for (var _i = 0, symbols_4 = symbols; _i < symbols_4.length; _i++) {
            var symbol = symbols_4[_i];
            this.add(symbol);
        }
    };
    MapSymbolTable.prototype.has = function (key) { return this.map.has(key); };
    MapSymbolTable.prototype.values = function () {
        // Switch to this.map.values once iterables are supported by the target language.
        return this._values;
    };
    return MapSymbolTable;
}());
var PipesTable = (function () {
    function PipesTable(pipes, context) {
        this.pipes = pipes;
        this.context = context;
    }
    Object.defineProperty(PipesTable.prototype, "size", {
        get: function () { return this.pipes.length; },
        enumerable: true,
        configurable: true
    });
    PipesTable.prototype.get = function (key) {
        var pipe = this.pipes.find(function (pipe) { return pipe.name == key; });
        if (pipe) {
            return new PipeSymbol(pipe, this.context);
        }
    };
    PipesTable.prototype.has = function (key) { return this.pipes.find(function (pipe) { return pipe.name == key; }) != null; };
    PipesTable.prototype.values = function () {
        var _this = this;
        return this.pipes.map(function (pipe) { return new PipeSymbol(pipe, _this.context); });
    };
    return PipesTable;
}());
var PipeSymbol = (function () {
    function PipeSymbol(pipe, context) {
        this.pipe = pipe;
        this.context = context;
    }
    Object.defineProperty(PipeSymbol.prototype, "name", {
        get: function () { return this.pipe.name; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "kind", {
        get: function () { return 'pipe'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "language", {
        get: function () { return 'typescript'; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "type", {
        get: function () { return new TypeWrapper(this.tsType, this.context); },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "container", {
        get: function () { return undefined; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "callable", {
        get: function () { return true; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "nullable", {
        get: function () { return false; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "public", {
        get: function () { return true; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PipeSymbol.prototype, "definition", {
        get: function () { return definitionFromTsSymbol(this.tsType.getSymbol()); },
        enumerable: true,
        configurable: true
    });
    PipeSymbol.prototype.members = function () { return EmptyTable.instance; };
    PipeSymbol.prototype.signatures = function () { return signaturesOf(this.tsType, this.context); };
    PipeSymbol.prototype.selectSignature = function (types) {
        var signature = selectSignature(this.tsType, this.context, types);
        if (types.length == 1) {
            var parameterType = types[0];
            if (parameterType instanceof TypeWrapper) {
                var resultType = undefined;
                switch (this.name) {
                    case 'async':
                        switch (parameterType.name) {
                            case 'Observable':
                            case 'Promise':
                            case 'EventEmitter':
                                resultType = getTypeParameterOf(parameterType.tsType, parameterType.name);
                                break;
                            default:
                                resultType = getBuiltinTypeFromTs(symbols_1.BuiltinType.Any, this.context);
                                break;
                        }
                        break;
                    case 'slice':
                        resultType = getTypeParameterOf(parameterType.tsType, 'Array');
                        break;
                }
                if (resultType) {
                    signature = new SignatureResultOverride(signature, new TypeWrapper(resultType, parameterType.context));
                }
            }
        }
        return signature;
    };
    PipeSymbol.prototype.indexed = function (argument) { return undefined; };
    Object.defineProperty(PipeSymbol.prototype, "tsType", {
        get: function () {
            var type = this._tsType;
            if (!type) {
                var classSymbol = this.findClassSymbol(this.pipe.type.reference);
                if (classSymbol) {
                    type = this._tsType = this.findTransformMethodType(classSymbol);
                }
                if (!type) {
                    type = this._tsType = getBuiltinTypeFromTs(symbols_1.BuiltinType.Any, this.context);
                }
            }
            return type;
        },
        enumerable: true,
        configurable: true
    });
    PipeSymbol.prototype.findClassSymbol = function (type) {
        return findClassSymbolInContext(type, this.context);
    };
    PipeSymbol.prototype.findTransformMethodType = function (classSymbol) {
        var classType = this.context.checker.getDeclaredTypeOfSymbol(classSymbol);
        if (classType) {
            var transform = classType.getProperty('transform');
            if (transform) {
                return this.context.checker.getTypeOfSymbolAtLocation(transform, this.context.node);
            }
        }
    };
    return PipeSymbol;
}());
function findClassSymbolInContext(type, context) {
    var sourceFile = context.program.getSourceFile(type.filePath);
    if (sourceFile) {
        var moduleSymbol = sourceFile.module || sourceFile.symbol;
        var exports_1 = context.checker.getExportsOfModule(moduleSymbol);
        return (exports_1 || []).find(function (symbol) { return symbol.name == type.name; });
    }
}
var EmptyTable = (function () {
    function EmptyTable() {
    }
    Object.defineProperty(EmptyTable.prototype, "size", {
        get: function () { return 0; },
        enumerable: true,
        configurable: true
    });
    EmptyTable.prototype.get = function (key) { return undefined; };
    EmptyTable.prototype.has = function (key) { return false; };
    EmptyTable.prototype.values = function () { return []; };
    return EmptyTable;
}());
EmptyTable.instance = new EmptyTable();
function findTsConfig(fileName) {
    var dir = path.dirname(fileName);
    while (fs.existsSync(dir)) {
        var candidate = path.join(dir, 'tsconfig.json');
        if (fs.existsSync(candidate))
            return candidate;
        var parentDir = path.dirname(dir);
        if (parentDir === dir)
            break;
        dir = parentDir;
    }
}
function isBindingPattern(node) {
    return !!node && (node.kind === ts.SyntaxKind.ArrayBindingPattern ||
        node.kind === ts.SyntaxKind.ObjectBindingPattern);
}
function walkUpBindingElementsAndPatterns(node) {
    while (node && (node.kind === ts.SyntaxKind.BindingElement || isBindingPattern(node))) {
        node = node.parent;
    }
    return node;
}
function getCombinedNodeFlags(node) {
    node = walkUpBindingElementsAndPatterns(node);
    var flags = node.flags;
    if (node.kind === ts.SyntaxKind.VariableDeclaration) {
        node = node.parent;
    }
    if (node && node.kind === ts.SyntaxKind.VariableDeclarationList) {
        flags |= node.flags;
        node = node.parent;
    }
    if (node && node.kind === ts.SyntaxKind.VariableStatement) {
        flags |= node.flags;
    }
    return flags;
}
function isSymbolPrivate(s) {
    return !!s.valueDeclaration && isPrivate(s.valueDeclaration);
}
function getBuiltinTypeFromTs(kind, context) {
    var type;
    var checker = context.checker;
    var node = context.node;
    switch (kind) {
        case symbols_1.BuiltinType.Any:
            type = checker.getTypeAtLocation(setParents({
                kind: ts.SyntaxKind.AsExpression,
                expression: { kind: ts.SyntaxKind.TrueKeyword },
                type: { kind: ts.SyntaxKind.AnyKeyword }
            }, node));
            break;
        case symbols_1.BuiltinType.Boolean:
            type =
                checker.getTypeAtLocation(setParents({ kind: ts.SyntaxKind.TrueKeyword }, node));
            break;
        case symbols_1.BuiltinType.Null:
            type =
                checker.getTypeAtLocation(setParents({ kind: ts.SyntaxKind.NullKeyword }, node));
            break;
        case symbols_1.BuiltinType.Number:
            var numeric = { kind: ts.SyntaxKind.NumericLiteral };
            setParents({ kind: ts.SyntaxKind.ExpressionStatement, expression: numeric }, node);
            type = checker.getTypeAtLocation(numeric);
            break;
        case symbols_1.BuiltinType.String:
            type = checker.getTypeAtLocation(setParents({ kind: ts.SyntaxKind.NoSubstitutionTemplateLiteral }, node));
            break;
        case symbols_1.BuiltinType.Undefined:
            type = checker.getTypeAtLocation(setParents({
                kind: ts.SyntaxKind.VoidExpression,
                expression: { kind: ts.SyntaxKind.NumericLiteral }
            }, node));
            break;
        default:
            throw new Error("Internal error, unhandled literal kind " + kind + ":" + symbols_1.BuiltinType[kind]);
    }
    return type;
}
function setParents(node, parent) {
    node.parent = parent;
    ts.forEachChild(node, function (child) { return setParents(child, node); });
    return node;
}
function spanOf(node) {
    return { start: node.getStart(), end: node.getEnd() };
}
function shrink(span, offset) {
    if (offset == null)
        offset = 1;
    return { start: span.start + offset, end: span.end - offset };
}
function spanAt(sourceFile, line, column) {
    if (line != null && column != null) {
        var position_1 = ts.getPositionOfLineAndCharacter(sourceFile, line, column);
        var findChild = function findChild(node) {
            if (node.kind > ts.SyntaxKind.LastToken && node.pos <= position_1 && node.end > position_1) {
                var betterNode = ts.forEachChild(node, findChild);
                return betterNode || node;
            }
        };
        var node = ts.forEachChild(sourceFile, findChild);
        if (node) {
            return { start: node.getStart(), end: node.getEnd() };
        }
    }
}
function definitionFromTsSymbol(symbol) {
    var declarations = symbol.declarations;
    if (declarations) {
        return declarations.map(function (declaration) {
            var sourceFile = declaration.getSourceFile();
            return {
                fileName: sourceFile.fileName,
                span: { start: declaration.getStart(), end: declaration.getEnd() }
            };
        });
    }
}
function parentDeclarationOf(node) {
    while (node) {
        switch (node.kind) {
            case ts.SyntaxKind.ClassDeclaration:
            case ts.SyntaxKind.InterfaceDeclaration:
                return node;
            case ts.SyntaxKind.SourceFile:
                return undefined;
        }
        node = node.parent;
    }
}
function getContainerOf(symbol, context) {
    if (symbol.getFlags() & ts.SymbolFlags.ClassMember && symbol.declarations) {
        for (var _i = 0, _a = symbol.declarations; _i < _a.length; _i++) {
            var declaration = _a[_i];
            var parent_1 = parentDeclarationOf(declaration);
            if (parent_1) {
                var type = context.checker.getTypeAtLocation(parent_1);
                if (type) {
                    return new TypeWrapper(type, context);
                }
            }
        }
    }
}
function getTypeParameterOf(type, name) {
    if (type && type.symbol && type.symbol.name == name) {
        var typeArguments = type.typeArguments;
        if (typeArguments && typeArguments.length <= 1) {
            return typeArguments[0];
        }
    }
}
function typeKindOf(type) {
    if (type) {
        if (type.flags & ts.TypeFlags.Any) {
            return symbols_1.BuiltinType.Any;
        }
        else if (type.flags & (ts.TypeFlags.String | ts.TypeFlags.StringLike | ts.TypeFlags.StringLiteral)) {
            return symbols_1.BuiltinType.String;
        }
        else if (type.flags & (ts.TypeFlags.Number | ts.TypeFlags.NumberLike)) {
            return symbols_1.BuiltinType.Number;
        }
        else if (type.flags & (ts.TypeFlags.Undefined)) {
            return symbols_1.BuiltinType.Undefined;
        }
        else if (type.flags & (ts.TypeFlags.Null)) {
            return symbols_1.BuiltinType.Null;
        }
        else if (type.flags & ts.TypeFlags.Union) {
            // If all the constituent types of a union are the same kind, it is also that kind.
            var candidate = null;
            var unionType = type;
            if (unionType.types.length > 0) {
                candidate = typeKindOf(unionType.types[0]);
                for (var _i = 0, _a = unionType.types; _i < _a.length; _i++) {
                    var subType = _a[_i];
                    if (candidate != typeKindOf(subType)) {
                        return symbols_1.BuiltinType.Other;
                    }
                }
            }
            if (candidate != null) {
                return candidate;
            }
        }
        else if (type.flags & ts.TypeFlags.TypeParameter) {
            return symbols_1.BuiltinType.Unbound;
        }
    }
    return symbols_1.BuiltinType.Other;
}
function getFromSymbolTable(symbolTable, key) {
    var table = symbolTable;
    var symbol;
    if (typeof table.get === 'function') {
        // TS 2.2 uses a Map
        symbol = table.get(key);
    }
    else {
        // TS pre-2.2 uses an object
        symbol = table[key];
    }
    return symbol;
}
function toNumbers(value) {
    return value ? value.split('.').map(function (v) { return +v; }) : [];
}
function compareNumbers(a, b) {
    for (var i = 0; i < a.length && i < b.length; i++) {
        if (a[i] > b[i])
            return 1;
        if (a[i] < b[i])
            return -1;
    }
    return 0;
}
function isTypescriptVersion(low, high) {
    var tsNumbers = toNumbers(ts.version);
    return compareNumbers(toNumbers(low), tsNumbers) <= 0 &&
        compareNumbers(toNumbers(high), tsNumbers) >= 0;
}
//# sourceMappingURL=typescript_symbols.js.map