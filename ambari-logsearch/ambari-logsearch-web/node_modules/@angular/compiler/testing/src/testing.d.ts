/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * @module
 * @description
 * Entry point for all APIs of the compiler package.
 *
 * <div class="callout is-critical">
 *   <header>Unstable APIs</header>
 *   <p>
 *     All compiler apis are currently considered experimental and private!
 *   </p>
 *   <p>
 *     We expect the APIs in this package to keep on changing. Do not rely on them.
 *   </p>
 * </div>
 */
export * from './schema_registry_mock';
export * from './directive_resolver_mock';
export * from './ng_module_resolver_mock';
export * from './pipe_resolver_mock';
import { ModuleWithComponentFactories, CompilerOptions, CompilerFactory, ComponentFactory, NgModuleFactory, Injector, NgModule, Component, Directive, Pipe, Type, PlatformRef } from '@angular/core';
import { MetadataOverride, ɵTestingCompilerFactory as TestingCompilerFactory, ɵTestingCompiler as TestingCompiler } from '@angular/core/testing';
import { JitCompiler, CompileMetadataResolver } from '@angular/compiler';
import { MockDirectiveResolver } from './directive_resolver_mock';
import { MockNgModuleResolver } from './ng_module_resolver_mock';
import { MockPipeResolver } from './pipe_resolver_mock';
export declare class TestingCompilerFactoryImpl implements TestingCompilerFactory {
    private _compilerFactory;
    constructor(_compilerFactory: CompilerFactory);
    createTestingCompiler(options: CompilerOptions[]): TestingCompiler;
}
export declare class TestingCompilerImpl implements TestingCompiler {
    private _compiler;
    private _directiveResolver;
    private _pipeResolver;
    private _moduleResolver;
    private _metadataResolver;
    private _overrider;
    constructor(_compiler: JitCompiler, _directiveResolver: MockDirectiveResolver, _pipeResolver: MockPipeResolver, _moduleResolver: MockNgModuleResolver, _metadataResolver: CompileMetadataResolver);
    readonly injector: Injector;
    compileModuleSync<T>(moduleType: Type<T>): NgModuleFactory<T>;
    compileModuleAsync<T>(moduleType: Type<T>): Promise<NgModuleFactory<T>>;
    compileModuleAndAllComponentsSync<T>(moduleType: Type<T>): ModuleWithComponentFactories<T>;
    compileModuleAndAllComponentsAsync<T>(moduleType: Type<T>): Promise<ModuleWithComponentFactories<T>>;
    getNgContentSelectors(component: Type<any>): string[];
    getComponentFactory<T>(component: Type<T>): ComponentFactory<T>;
    checkOverrideAllowed(type: Type<any>): void;
    overrideModule(ngModule: Type<any>, override: MetadataOverride<NgModule>): void;
    overrideDirective(directive: Type<any>, override: MetadataOverride<Directive>): void;
    overrideComponent(component: Type<any>, override: MetadataOverride<Component>): void;
    overridePipe(pipe: Type<any>, override: MetadataOverride<Pipe>): void;
    loadAotSummaries(summaries: () => any[]): void;
    clearCache(): void;
    clearCacheFor(type: Type<any>): void;
}
/**
 * Platform for dynamic tests
 *
 * @experimental
 */
export declare const platformCoreDynamicTesting: (extraProviders?: any[]) => PlatformRef;
