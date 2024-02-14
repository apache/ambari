/**
 * @license Angular v4.4.3
 * (c) 2010-2017 Google, Inc. https://angular.io/
 * License: MIT
 */
(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@angular/common'), require('@angular/common/testing'), require('@angular/core'), require('@angular/router')) :
	typeof define === 'function' && define.amd ? define(['exports', '@angular/common', '@angular/common/testing', '@angular/core', '@angular/router'], factory) :
	(factory((global.ng = global.ng || {}, global.ng.router = global.ng.router || {}, global.ng.router.testing = global.ng.router.testing || {}),global.ng.common,global.ng.common.testing,global.ng.core,global.ng.router));
}(this, (function (exports,_angular_common,_angular_common_testing,_angular_core,_angular_router) { 'use strict';

/**
 * @license Angular v4.4.3
 * (c) 2010-2017 Google, Inc. https://angular.io/
 * License: MIT
 */
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * @whatItDoes Allows to simulate the loading of ng modules in tests.
 *
 * @howToUse
 *
 * ```
 * const loader = TestBed.get(NgModuleFactoryLoader);
 *
 * @Component({template: 'lazy-loaded'})
 * class LazyLoadedComponent {}
 * @NgModule({
 *   declarations: [LazyLoadedComponent],
 *   imports: [RouterModule.forChild([{path: 'loaded', component: LazyLoadedComponent}])]
 * })
 *
 * class LoadedModule {}
 *
 * // sets up stubbedModules
 * loader.stubbedModules = {lazyModule: LoadedModule};
 *
 * router.resetConfig([
 *   {path: 'lazy', loadChildren: 'lazyModule'},
 * ]);
 *
 * router.navigateByUrl('/lazy/loaded');
 * ```
 *
 * @stable
 */
var SpyNgModuleFactoryLoader = (function () {
    function SpyNgModuleFactoryLoader(compiler) {
        this.compiler = compiler;
        /**
         * @docsNotRequired
         */
        this._stubbedModules = {};
    }
    Object.defineProperty(SpyNgModuleFactoryLoader.prototype, "stubbedModules", {
        /**
         * @docsNotRequired
         */
        get: function () { return this._stubbedModules; },
        /**
         * @docsNotRequired
         */
        set: function (modules) {
            var res = {};
            for (var _i = 0, _a = Object.keys(modules); _i < _a.length; _i++) {
                var t = _a[_i];
                res[t] = this.compiler.compileModuleAsync(modules[t]);
            }
            this._stubbedModules = res;
        },
        enumerable: true,
        configurable: true
    });
    SpyNgModuleFactoryLoader.prototype.load = function (path) {
        if (this._stubbedModules[path]) {
            return this._stubbedModules[path];
        }
        else {
            return Promise.reject(new Error("Cannot find module " + path));
        }
    };
    return SpyNgModuleFactoryLoader;
}());
SpyNgModuleFactoryLoader.decorators = [
    { type: _angular_core.Injectable },
];
/** @nocollapse */
SpyNgModuleFactoryLoader.ctorParameters = function () { return [
    { type: _angular_core.Compiler, },
]; };
/**
 * Router setup factory function used for testing.
 *
 * @stable
 */
function setupTestingRouter(urlSerializer, contexts, location, loader, compiler, injector, routes, urlHandlingStrategy) {
    var router = new _angular_router.Router(null, urlSerializer, contexts, location, injector, loader, compiler, _angular_router.ɵflatten(routes));
    if (urlHandlingStrategy) {
        router.urlHandlingStrategy = urlHandlingStrategy;
    }
    return router;
}
/**
 * @whatItDoes Sets up the router to be used for testing.
 *
 * @howToUse
 *
 * ```
 * beforeEach(() => {
 *   TestBed.configureTestModule({
 *     imports: [
 *       RouterTestingModule.withRoutes(
 *         [{path: '', component: BlankCmp}, {path: 'simple', component: SimpleCmp}])]
 *       )
 *     ]
 *   });
 * });
 * ```
 *
 * @description
 *
 * The modules sets up the router to be used for testing.
 * It provides spy implementations of {@link Location}, {@link LocationStrategy}, and {@link
 * NgModuleFactoryLoader}.
 *
 * @stable
 */
var RouterTestingModule = (function () {
    function RouterTestingModule() {
    }
    RouterTestingModule.withRoutes = function (routes) {
        return { ngModule: RouterTestingModule, providers: [_angular_router.provideRoutes(routes)] };
    };
    return RouterTestingModule;
}());
RouterTestingModule.decorators = [
    { type: _angular_core.NgModule, args: [{
                exports: [_angular_router.RouterModule],
                providers: [
                    _angular_router.ɵROUTER_PROVIDERS, { provide: _angular_common.Location, useClass: _angular_common_testing.SpyLocation },
                    { provide: _angular_common.LocationStrategy, useClass: _angular_common_testing.MockLocationStrategy },
                    { provide: _angular_core.NgModuleFactoryLoader, useClass: SpyNgModuleFactoryLoader }, {
                        provide: _angular_router.Router,
                        useFactory: setupTestingRouter,
                        deps: [
                            _angular_router.UrlSerializer, _angular_router.ChildrenOutletContexts, _angular_common.Location, _angular_core.NgModuleFactoryLoader, _angular_core.Compiler, _angular_core.Injector,
                            _angular_router.ROUTES, [_angular_router.UrlHandlingStrategy, new _angular_core.Optional()]
                        ]
                    },
                    { provide: _angular_router.PreloadingStrategy, useExisting: _angular_router.NoPreloading }, _angular_router.provideRoutes([])
                ]
            },] },
];
/** @nocollapse */
RouterTestingModule.ctorParameters = function () { return []; };

exports.SpyNgModuleFactoryLoader = SpyNgModuleFactoryLoader;
exports.setupTestingRouter = setupTestingRouter;
exports.RouterTestingModule = RouterTestingModule;

Object.defineProperty(exports, '__esModule', { value: true });

})));
//# sourceMappingURL=router-testing.umd.js.map
