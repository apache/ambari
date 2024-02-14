/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { CompileReflector, DirectiveResolver } from '@angular/compiler';
import { Directive, Injector, Provider, Type, ɵViewMetadata as ViewMetadata } from '@angular/core';
/**
 * An implementation of {@link DirectiveResolver} that allows overriding
 * various properties of directives.
 */
export declare class MockDirectiveResolver extends DirectiveResolver {
    private _injector;
    private _directives;
    private _providerOverrides;
    private _viewProviderOverrides;
    private _views;
    private _inlineTemplates;
    constructor(_injector: Injector, reflector: CompileReflector);
    private readonly _compiler;
    private _clearCacheFor(component);
    resolve(type: Type<any>): Directive;
    resolve(type: Type<any>, throwIfNotFound: true): Directive;
    resolve(type: Type<any>, throwIfNotFound: boolean): Directive | null;
    /**
     * Overrides the {@link Directive} for a directive.
     */
    setDirective(type: Type<any>, metadata: Directive): void;
    setProvidersOverride(type: Type<any>, providers: Provider[]): void;
    setViewProvidersOverride(type: Type<any>, viewProviders: Provider[]): void;
    /**
     * Overrides the {@link ViewMetadata} for a component.
     */
    setView(component: Type<any>, view: ViewMetadata): void;
    /**
     * Overrides the inline template for a component - other configuration remains unchanged.
     */
    setInlineTemplate(component: Type<any>, template: string): void;
}
