/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Compiler, ComponentFactory, Injector, ModuleWithComponentFactories, NgModuleFactory, Type, ɵConsole as Console } from '@angular/core';
import { CompilerConfig } from '../config';
import { CompileMetadataResolver } from '../metadata_resolver';
import { NgModuleCompiler } from '../ng_module_compiler';
import { StyleCompiler } from '../style_compiler';
import { SummaryResolver } from '../summary_resolver';
import { TemplateParser } from '../template_parser/template_parser';
import { ViewCompiler } from '../view_compiler/view_compiler';
/**
 * An internal module of the Angular compiler that begins with component types,
 * extracts templates, and eventually produces a compiled version of the component
 * ready for linking into an application.
 *
 * @security  When compiling templates at runtime, you must ensure that the entire template comes
 * from a trusted source. Attacker-controlled data introduced by a template could expose your
 * application to XSS risks.  For more detail, see the [Security Guide](http://g.co/ng/security).
 */
export declare class JitCompiler implements Compiler {
    private _injector;
    private _metadataResolver;
    private _templateParser;
    private _styleCompiler;
    private _viewCompiler;
    private _ngModuleCompiler;
    private _summaryResolver;
    private _compilerConfig;
    private _console;
    private _compiledTemplateCache;
    private _compiledHostTemplateCache;
    private _compiledDirectiveWrapperCache;
    private _compiledNgModuleCache;
    private _sharedStylesheetCount;
    constructor(_injector: Injector, _metadataResolver: CompileMetadataResolver, _templateParser: TemplateParser, _styleCompiler: StyleCompiler, _viewCompiler: ViewCompiler, _ngModuleCompiler: NgModuleCompiler, _summaryResolver: SummaryResolver<Type<any>>, _compilerConfig: CompilerConfig, _console: Console);
    readonly injector: Injector;
    compileModuleSync<T>(moduleType: Type<T>): NgModuleFactory<T>;
    compileModuleAsync<T>(moduleType: Type<T>): Promise<NgModuleFactory<T>>;
    compileModuleAndAllComponentsSync<T>(moduleType: Type<T>): ModuleWithComponentFactories<T>;
    compileModuleAndAllComponentsAsync<T>(moduleType: Type<T>): Promise<ModuleWithComponentFactories<T>>;
    getNgContentSelectors(component: Type<any>): string[];
    getComponentFactory<T>(component: Type<T>): ComponentFactory<T>;
    loadAotSummaries(summaries: () => any[]): void;
    hasAotSummary(ref: Type<any>): boolean;
    private _filterJitIdentifiers(ids);
    private _compileModuleAndComponents<T>(moduleType, isSync);
    private _compileModuleAndAllComponents<T>(moduleType, isSync);
    private _loadModules(mainModule, isSync);
    private _compileModule<T>(moduleType);
    clearCacheFor(type: Type<any>): void;
    clearCache(): void;
    private _createCompiledHostTemplate(compType, ngModule);
    private _createCompiledTemplate(compMeta, ngModule);
    private _compileTemplate(template);
    private _resolveStylesCompileResult(result, externalStylesheetsByModuleUrl);
    private _resolveAndEvalStylesCompileResult(result, externalStylesheetsByModuleUrl);
}
