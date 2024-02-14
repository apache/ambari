/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { CompileDirectiveMetadata, CompileNgModuleMetadata, CompileProviderMetadata, CompileQueryMetadata } from './compile_metadata';
import { CompileReflector } from './compile_reflector';
import { ParseError, ParseSourceSpan } from './parse_util';
import { AttrAst, DirectiveAst, ProviderAst, QueryMatch, ReferenceAst } from './template_parser/template_ast';
export declare class ProviderError extends ParseError {
    constructor(message: string, span: ParseSourceSpan);
}
export interface QueryWithId {
    meta: CompileQueryMetadata;
    queryId: number;
}
export declare class ProviderViewContext {
    reflector: CompileReflector;
    component: CompileDirectiveMetadata;
    errors: ProviderError[];
    constructor(reflector: CompileReflector, component: CompileDirectiveMetadata);
}
export declare class ProviderElementContext {
    viewContext: ProviderViewContext;
    private _parent;
    private _isViewRoot;
    private _directiveAsts;
    private _sourceSpan;
    private _contentQueries;
    private _transformedProviders;
    private _seenProviders;
    private _allProviders;
    private _attrs;
    private _hasViewContainer;
    private _queriedTokens;
    constructor(viewContext: ProviderViewContext, _parent: ProviderElementContext, _isViewRoot: boolean, _directiveAsts: DirectiveAst[], attrs: AttrAst[], refs: ReferenceAst[], isTemplate: boolean, contentQueryStartId: number, _sourceSpan: ParseSourceSpan);
    afterElement(): void;
    readonly transformProviders: ProviderAst[];
    readonly transformedDirectiveAsts: DirectiveAst[];
    readonly transformedHasViewContainer: boolean;
    readonly queryMatches: QueryMatch[];
    private _addQueryReadsTo(token, defaultValue, queryReadTokens);
    private _getQueriesFor(token);
    private _getOrCreateLocalProvider(requestingProviderType, token, eager);
    private _getLocalDependency(requestingProviderType, dep, eager?);
    private _getDependency(requestingProviderType, dep, eager?);
}
export declare class NgModuleProviderAnalyzer {
    private reflector;
    private _transformedProviders;
    private _seenProviders;
    private _allProviders;
    private _errors;
    constructor(reflector: CompileReflector, ngModule: CompileNgModuleMetadata, extraProviders: CompileProviderMetadata[], sourceSpan: ParseSourceSpan);
    parse(): ProviderAst[];
    private _getOrCreateLocalProvider(token, eager);
    private _getDependency(dep, eager, requestorSourceSpan);
}
