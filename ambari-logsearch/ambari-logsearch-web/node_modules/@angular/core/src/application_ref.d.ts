/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Observable } from 'rxjs/Observable';
import { ErrorHandler } from '../src/error_handler';
import { ApplicationInitStatus } from './application_init';
import { Console } from './console';
import { InjectionToken, Injector, Provider } from './di';
import { CompilerOptions } from './linker/compiler';
import { ComponentFactory, ComponentRef } from './linker/component_factory';
import { ComponentFactoryResolver } from './linker/component_factory_resolver';
import { NgModuleFactory, NgModuleRef } from './linker/ng_module_factory';
import { ViewRef } from './linker/view_ref';
import { Type } from './type';
import { NgZone } from './zone/ng_zone';
export declare const ALLOW_MULTIPLE_PLATFORMS: InjectionToken<boolean>;
/**
 * Disable Angular's development mode, which turns off assertions and other
 * checks within the framework.
 *
 * One important assertion this disables verifies that a change detection pass
 * does not result in additional changes to any bindings (also known as
 * unidirectional data flow).
 *
 * @stable
 */
export declare function enableProdMode(): void;
/**
 * Returns whether Angular is in development mode. After called once,
 * the value is locked and won't change any more.
 *
 * By default, this is true, unless a user calls `enableProdMode` before calling this.
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function isDevMode(): boolean;
/**
 * A token for third-party components that can register themselves with NgProbe.
 *
 * @experimental
 */
export declare class NgProbeToken {
    name: string;
    token: any;
    constructor(name: string, token: any);
}
/**
 * Creates a platform.
 * Platforms have to be eagerly created via this function.
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function createPlatform(injector: Injector): PlatformRef;
/**
 * Creates a factory for a platform
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function createPlatformFactory(parentPlatformFactory: ((extraProviders?: Provider[]) => PlatformRef) | null, name: string, providers?: Provider[]): (extraProviders?: Provider[]) => PlatformRef;
/**
 * Checks that there currently is a platform which contains the given token as a provider.
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function assertPlatform(requiredToken: any): PlatformRef;
/**
 * Destroy the existing platform.
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function destroyPlatform(): void;
/**
 * Returns the current platform.
 *
 * @experimental APIs related to application bootstrap are currently under review.
 */
export declare function getPlatform(): PlatformRef | null;
/**
 * The Angular platform is the entry point for Angular on a web page. Each page
 * has exactly one platform, and services (such as reflection) which are common
 * to every Angular application running on the page are bound in its scope.
 *
 * A page's platform is initialized implicitly when a platform is created via a platform factory
 * (e.g. {@link platformBrowser}), or explicitly by calling the {@link createPlatform} function.
 *
 * @stable
 */
export declare abstract class PlatformRef {
    /**
     * Creates an instance of an `@NgModule` for the given platform
     * for offline compilation.
     *
     * ## Simple Example
     *
     * ```typescript
     * my_module.ts:
     *
     * @NgModule({
     *   imports: [BrowserModule]
     * })
     * class MyModule {}
     *
     * main.ts:
     * import {MyModuleNgFactory} from './my_module.ngfactory';
     * import {platformBrowser} from '@angular/platform-browser';
     *
     * let moduleRef = platformBrowser().bootstrapModuleFactory(MyModuleNgFactory);
     * ```
     *
     * @experimental APIs related to application bootstrap are currently under review.
     */
    abstract bootstrapModuleFactory<M>(moduleFactory: NgModuleFactory<M>): Promise<NgModuleRef<M>>;
    /**
     * Creates an instance of an `@NgModule` for a given platform using the given runtime compiler.
     *
     * ## Simple Example
     *
     * ```typescript
     * @NgModule({
     *   imports: [BrowserModule]
     * })
     * class MyModule {}
     *
     * let moduleRef = platformBrowser().bootstrapModule(MyModule);
     * ```
     * @stable
     */
    abstract bootstrapModule<M>(moduleType: Type<M>, compilerOptions?: CompilerOptions | CompilerOptions[]): Promise<NgModuleRef<M>>;
    /**
     * Register a listener to be called when the platform is disposed.
     */
    abstract onDestroy(callback: () => void): void;
    /**
     * Retrieve the platform {@link Injector}, which is the parent injector for
     * every Angular application on the page and provides singleton providers.
     */
    readonly abstract injector: Injector;
    /**
     * Destroy the Angular platform and all Angular applications on the page.
     */
    abstract destroy(): void;
    readonly abstract destroyed: boolean;
}
/**
 * workaround https://github.com/angular/tsickle/issues/350
 * @suppress {checkTypes}
 */
export declare class PlatformRef_ extends PlatformRef {
    private _injector;
    private _modules;
    private _destroyListeners;
    private _destroyed;
    constructor(_injector: Injector);
    onDestroy(callback: () => void): void;
    readonly injector: Injector;
    readonly destroyed: boolean;
    destroy(): void;
    bootstrapModuleFactory<M>(moduleFactory: NgModuleFactory<M>): Promise<NgModuleRef<M>>;
    private _bootstrapModuleFactoryWithZone<M>(moduleFactory, ngZone?);
    bootstrapModule<M>(moduleType: Type<M>, compilerOptions?: CompilerOptions | CompilerOptions[]): Promise<NgModuleRef<M>>;
    private _bootstrapModuleWithZone<M>(moduleType, compilerOptions?, ngZone?);
    private _moduleDoBootstrap(moduleRef);
}
/**
 * A reference to an Angular application running on a page.
 *
 * @stable
 */
export declare abstract class ApplicationRef {
    /**
     * Bootstrap a new component at the root level of the application.
     *
     * ### Bootstrap process
     *
     * When bootstrapping a new root component into an application, Angular mounts the
     * specified application component onto DOM elements identified by the [componentType]'s
     * selector and kicks off automatic change detection to finish initializing the component.
     *
     * Optionally, a component can be mounted onto a DOM element that does not match the
     * [componentType]'s selector.
     *
     * ### Example
     * {@example core/ts/platform/platform.ts region='longform'}
     */
    abstract bootstrap<C>(componentFactory: ComponentFactory<C> | Type<C>, rootSelectorOrNode?: string | any): ComponentRef<C>;
    /**
     * Invoke this method to explicitly process change detection and its side-effects.
     *
     * In development mode, `tick()` also performs a second change detection cycle to ensure that no
     * further changes are detected. If additional changes are picked up during this second cycle,
     * bindings in the app have side-effects that cannot be resolved in a single change detection
     * pass.
     * In this case, Angular throws an error, since an Angular application can only have one change
     * detection pass during which all change detection must complete.
     */
    abstract tick(): void;
    /**
     * Get a list of component types registered to this application.
     * This list is populated even before the component is created.
     */
    readonly abstract componentTypes: Type<any>[];
    /**
     * Get a list of components registered to this application.
     */
    readonly abstract components: ComponentRef<any>[];
    /**
     * Attaches a view so that it will be dirty checked.
     * The view will be automatically detached when it is destroyed.
     * This will throw if the view is already attached to a ViewContainer.
     */
    abstract attachView(view: ViewRef): void;
    /**
     * Detaches a view from dirty checking again.
     */
    abstract detachView(view: ViewRef): void;
    /**
     * Returns the number of attached views.
     */
    readonly abstract viewCount: number;
    /**
     * Returns an Observable that indicates when the application is stable or unstable.
     */
    readonly abstract isStable: Observable<boolean>;
}
/**
 * workaround https://github.com/angular/tsickle/issues/350
 * @suppress {checkTypes}
 */
export declare class ApplicationRef_ extends ApplicationRef {
    private _zone;
    private _console;
    private _injector;
    private _exceptionHandler;
    private _componentFactoryResolver;
    private _initStatus;
    private _bootstrapListeners;
    private _rootComponents;
    private _rootComponentTypes;
    private _views;
    private _runningTick;
    private _enforceNoNewChanges;
    private _isStable;
    private _stable;
    constructor(_zone: NgZone, _console: Console, _injector: Injector, _exceptionHandler: ErrorHandler, _componentFactoryResolver: ComponentFactoryResolver, _initStatus: ApplicationInitStatus);
    attachView(viewRef: ViewRef): void;
    detachView(viewRef: ViewRef): void;
    bootstrap<C>(componentOrFactory: ComponentFactory<C> | Type<C>, rootSelectorOrNode?: string | any): ComponentRef<C>;
    private _loadComponent(componentRef);
    private _unloadComponent(componentRef);
    tick(): void;
    ngOnDestroy(): void;
    readonly viewCount: number;
    readonly componentTypes: Type<any>[];
    readonly components: ComponentRef<any>[];
    readonly isStable: Observable<boolean>;
}
