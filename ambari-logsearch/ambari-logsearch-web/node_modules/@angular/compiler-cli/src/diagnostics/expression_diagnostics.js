"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var compiler_1 = require("@angular/compiler");
var expression_type_1 = require("./expression_type");
var symbols_1 = require("./symbols");
function getTemplateExpressionDiagnostics(info) {
    var visitor = new ExpressionDiagnosticsVisitor(info, function (path, includeEvent) {
        return getExpressionScope(info, path, includeEvent);
    });
    compiler_1.templateVisitAll(visitor, info.templateAst);
    return visitor.diagnostics;
}
exports.getTemplateExpressionDiagnostics = getTemplateExpressionDiagnostics;
function getExpressionDiagnostics(scope, ast, query, context) {
    if (context === void 0) { context = {}; }
    var analyzer = new expression_type_1.AstType(scope, query, context);
    analyzer.getDiagnostics(ast);
    return analyzer.diagnostics;
}
exports.getExpressionDiagnostics = getExpressionDiagnostics;
function getReferences(info) {
    var result = [];
    function processReferences(references) {
        var _loop_1 = function (reference) {
            var type = undefined;
            if (reference.value) {
                type = info.query.getTypeSymbol(compiler_1.tokenReference(reference.value));
            }
            result.push({
                name: reference.name,
                kind: 'reference',
                type: type || info.query.getBuiltinType(symbols_1.BuiltinType.Any),
                get definition() { return getDefintionOf(info, reference); }
            });
        };
        for (var _i = 0, references_1 = references; _i < references_1.length; _i++) {
            var reference = references_1[_i];
            _loop_1(reference);
        }
    }
    var visitor = new (function (_super) {
        __extends(class_1, _super);
        function class_1() {
            return _super !== null && _super.apply(this, arguments) || this;
        }
        class_1.prototype.visitEmbeddedTemplate = function (ast, context) {
            _super.prototype.visitEmbeddedTemplate.call(this, ast, context);
            processReferences(ast.references);
        };
        class_1.prototype.visitElement = function (ast, context) {
            _super.prototype.visitElement.call(this, ast, context);
            processReferences(ast.references);
        };
        return class_1;
    }(compiler_1.RecursiveTemplateAstVisitor));
    compiler_1.templateVisitAll(visitor, info.templateAst);
    return result;
}
function getDefintionOf(info, ast) {
    if (info.fileName) {
        var templateOffset = info.offset;
        return [{
                fileName: info.fileName,
                span: {
                    start: ast.sourceSpan.start.offset + templateOffset,
                    end: ast.sourceSpan.end.offset + templateOffset
                }
            }];
    }
}
function getVarDeclarations(info, path) {
    var result = [];
    var current = path.tail;
    while (current) {
        if (current instanceof compiler_1.EmbeddedTemplateAst) {
            var _loop_2 = function (variable) {
                var name_1 = variable.name;
                // Find the first directive with a context.
                var context = current.directives.map(function (d) { return info.query.getTemplateContext(d.directive.type.reference); })
                    .find(function (c) { return !!c; });
                // Determine the type of the context field referenced by variable.value.
                var type = undefined;
                if (context) {
                    var value = context.get(variable.value);
                    if (value) {
                        type = value.type;
                        var kind = info.query.getTypeKind(type);
                        if (kind === symbols_1.BuiltinType.Any || kind == symbols_1.BuiltinType.Unbound) {
                            // The any type is not very useful here. For special cases, such as ngFor, we can do
                            // better.
                            type = refinedVariableType(type, info, current);
                        }
                    }
                }
                if (!type) {
                    type = info.query.getBuiltinType(symbols_1.BuiltinType.Any);
                }
                result.push({
                    name: name_1,
                    kind: 'variable', type: type, get definition() { return getDefintionOf(info, variable); }
                });
            };
            for (var _i = 0, _a = current.variables; _i < _a.length; _i++) {
                var variable = _a[_i];
                _loop_2(variable);
            }
        }
        current = path.parentOf(current);
    }
    return result;
}
function refinedVariableType(type, info, templateElement) {
    // Special case the ngFor directive
    var ngForDirective = templateElement.directives.find(function (d) {
        var name = compiler_1.identifierName(d.directive.type);
        return name == 'NgFor' || name == 'NgForOf';
    });
    if (ngForDirective) {
        var ngForOfBinding = ngForDirective.inputs.find(function (i) { return i.directiveName == 'ngForOf'; });
        if (ngForOfBinding) {
            var bindingType = new expression_type_1.AstType(info.members, info.query, {}).getType(ngForOfBinding.value);
            if (bindingType) {
                var result = info.query.getElementType(bindingType);
                if (result) {
                    return result;
                }
            }
        }
    }
    // We can't do better, return any
    return info.query.getBuiltinType(symbols_1.BuiltinType.Any);
}
function getEventDeclaration(info, includeEvent) {
    var result = [];
    if (includeEvent) {
        // TODO: Determine the type of the event parameter based on the Observable<T> or EventEmitter<T>
        // of the event.
        result = [{ name: '$event', kind: 'variable', type: info.query.getBuiltinType(symbols_1.BuiltinType.Any) }];
    }
    return result;
}
function getExpressionScope(info, path, includeEvent) {
    var result = info.members;
    var references = getReferences(info);
    var variables = getVarDeclarations(info, path);
    var events = getEventDeclaration(info, includeEvent);
    if (references.length || variables.length || events.length) {
        var referenceTable = info.query.createSymbolTable(references);
        var variableTable = info.query.createSymbolTable(variables);
        var eventsTable = info.query.createSymbolTable(events);
        result = info.query.mergeSymbolTable([result, referenceTable, variableTable, eventsTable]);
    }
    return result;
}
exports.getExpressionScope = getExpressionScope;
var ExpressionDiagnosticsVisitor = (function (_super) {
    __extends(ExpressionDiagnosticsVisitor, _super);
    function ExpressionDiagnosticsVisitor(info, getExpressionScope) {
        var _this = _super.call(this) || this;
        _this.info = info;
        _this.getExpressionScope = getExpressionScope;
        _this.diagnostics = [];
        _this.path = new compiler_1.AstPath([]);
        return _this;
    }
    ExpressionDiagnosticsVisitor.prototype.visitDirective = function (ast, context) {
        // Override the default child visitor to ignore the host properties of a directive.
        if (ast.inputs && ast.inputs.length) {
            compiler_1.templateVisitAll(this, ast.inputs, context);
        }
    };
    ExpressionDiagnosticsVisitor.prototype.visitBoundText = function (ast) {
        this.push(ast);
        this.diagnoseExpression(ast.value, ast.sourceSpan.start.offset, false);
        this.pop();
    };
    ExpressionDiagnosticsVisitor.prototype.visitDirectiveProperty = function (ast) {
        this.push(ast);
        this.diagnoseExpression(ast.value, this.attributeValueLocation(ast), false);
        this.pop();
    };
    ExpressionDiagnosticsVisitor.prototype.visitElementProperty = function (ast) {
        this.push(ast);
        this.diagnoseExpression(ast.value, this.attributeValueLocation(ast), false);
        this.pop();
    };
    ExpressionDiagnosticsVisitor.prototype.visitEvent = function (ast) {
        this.push(ast);
        this.diagnoseExpression(ast.handler, this.attributeValueLocation(ast), true);
        this.pop();
    };
    ExpressionDiagnosticsVisitor.prototype.visitVariable = function (ast) {
        var directive = this.directiveSummary;
        if (directive && ast.value) {
            var context = this.info.query.getTemplateContext(directive.type.reference);
            if (context && !context.has(ast.value)) {
                if (ast.value === '$implicit') {
                    this.reportError('The template context does not have an implicit value', spanOf(ast.sourceSpan));
                }
                else {
                    this.reportError("The template context does not defined a member called '" + ast.value + "'", spanOf(ast.sourceSpan));
                }
            }
        }
    };
    ExpressionDiagnosticsVisitor.prototype.visitElement = function (ast, context) {
        this.push(ast);
        _super.prototype.visitElement.call(this, ast, context);
        this.pop();
    };
    ExpressionDiagnosticsVisitor.prototype.visitEmbeddedTemplate = function (ast, context) {
        var previousDirectiveSummary = this.directiveSummary;
        this.push(ast);
        // Find directive that refernces this template
        this.directiveSummary =
            ast.directives.map(function (d) { return d.directive; }).find(function (d) { return hasTemplateReference(d.type); });
        // Process children
        _super.prototype.visitEmbeddedTemplate.call(this, ast, context);
        this.pop();
        this.directiveSummary = previousDirectiveSummary;
    };
    ExpressionDiagnosticsVisitor.prototype.attributeValueLocation = function (ast) {
        var path = compiler_1.findNode(this.info.htmlAst, ast.sourceSpan.start.offset);
        var last = path.tail;
        if (last instanceof compiler_1.Attribute && last.valueSpan) {
            // Add 1 for the quote.
            return last.valueSpan.start.offset + 1;
        }
        return ast.sourceSpan.start.offset;
    };
    ExpressionDiagnosticsVisitor.prototype.diagnoseExpression = function (ast, offset, includeEvent) {
        var _this = this;
        var scope = this.getExpressionScope(this.path, includeEvent);
        (_a = this.diagnostics).push.apply(_a, getExpressionDiagnostics(scope, ast, this.info.query, {
            event: includeEvent
        }).map(function (d) { return ({
            span: offsetSpan(d.ast.span, offset + _this.info.offset),
            kind: d.kind,
            message: d.message
        }); }));
        var _a;
    };
    ExpressionDiagnosticsVisitor.prototype.push = function (ast) { this.path.push(ast); };
    ExpressionDiagnosticsVisitor.prototype.pop = function () { this.path.pop(); };
    ExpressionDiagnosticsVisitor.prototype.reportError = function (message, span) {
        if (span) {
            this.diagnostics.push({ span: offsetSpan(span, this.info.offset), kind: expression_type_1.DiagnosticKind.Error, message: message });
        }
    };
    ExpressionDiagnosticsVisitor.prototype.reportWarning = function (message, span) {
        this.diagnostics.push({ span: offsetSpan(span, this.info.offset), kind: expression_type_1.DiagnosticKind.Warning, message: message });
    };
    return ExpressionDiagnosticsVisitor;
}(compiler_1.RecursiveTemplateAstVisitor));
function hasTemplateReference(type) {
    if (type.diDeps) {
        for (var _i = 0, _a = type.diDeps; _i < _a.length; _i++) {
            var diDep = _a[_i];
            if (diDep.token && diDep.token.identifier &&
                compiler_1.identifierName(diDep.token.identifier) == 'TemplateRef')
                return true;
        }
    }
    return false;
}
function offsetSpan(span, amount) {
    return { start: span.start + amount, end: span.end + amount };
}
function spanOf(sourceSpan) {
    return { start: sourceSpan.start.offset, end: sourceSpan.end.offset };
}
//# sourceMappingURL=expression_diagnostics.js.map