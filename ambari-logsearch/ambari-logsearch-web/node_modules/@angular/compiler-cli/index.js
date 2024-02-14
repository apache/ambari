"use strict";
function __export(m) {
    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
}
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var compiler_1 = require("@angular/compiler");
exports.StaticReflector = compiler_1.StaticReflector;
exports.StaticSymbol = compiler_1.StaticSymbol;
var codegen_1 = require("./src/codegen");
exports.CodeGenerator = codegen_1.CodeGenerator;
var compiler_host_1 = require("./src/compiler_host");
exports.CompilerHost = compiler_host_1.CompilerHost;
exports.ModuleResolutionHostAdapter = compiler_host_1.ModuleResolutionHostAdapter;
exports.NodeCompilerHostContext = compiler_host_1.NodeCompilerHostContext;
var extractor_1 = require("./src/extractor");
exports.Extractor = extractor_1.Extractor;
__export(require("@angular/tsc-wrapped"));
var version_1 = require("./src/version");
exports.VERSION = version_1.VERSION;
var expression_diagnostics_1 = require("./src/diagnostics/expression_diagnostics");
exports.getTemplateExpressionDiagnostics = expression_diagnostics_1.getTemplateExpressionDiagnostics;
exports.getExpressionScope = expression_diagnostics_1.getExpressionScope;
var expression_type_1 = require("./src/diagnostics/expression_type");
exports.AstType = expression_type_1.AstType;
var typescript_symbols_1 = require("./src/diagnostics/typescript_symbols");
exports.getClassMembersFromDeclaration = typescript_symbols_1.getClassMembersFromDeclaration;
exports.getPipesTable = typescript_symbols_1.getPipesTable;
exports.getSymbolQuery = typescript_symbols_1.getSymbolQuery;
var symbols_1 = require("./src/diagnostics/symbols");
exports.BuiltinType = symbols_1.BuiltinType;
__export(require("./src/transformers/api"));
__export(require("./src/transformers/entry_points"));
var ngc_1 = require("./src/ngc");
exports.ngc = ngc_1.main;
// TODO(hansl): moving to Angular 4 need to update this API.
var ngtools_api_1 = require("./src/ngtools_api");
exports.__NGTOOLS_PRIVATE_API_2 = ngtools_api_1.NgTools_InternalApi_NG_2;
//# sourceMappingURL=index.js.map