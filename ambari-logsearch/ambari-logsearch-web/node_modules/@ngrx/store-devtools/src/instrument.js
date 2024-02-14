import { NgModule, Injector } from '@angular/core';
import { StoreModule, State, INITIAL_STATE, INITIAL_REDUCER, Dispatcher, Reducer } from '@ngrx/store';
import { StoreDevtools, DevtoolsDispatcher } from './devtools';
import { STORE_DEVTOOLS_CONFIG, INITIAL_OPTIONS } from './config';
import { DevtoolsExtension, REDUX_DEVTOOLS_EXTENSION } from './extension';
export function _createReduxDevtoolsExtension() {
    var legacyExtensionKey = 'devToolsExtension';
    var extensionKey = '__REDUX_DEVTOOLS_EXTENSION__';
    if (typeof window === 'object' && typeof window[legacyExtensionKey] !== 'undefined') {
        return window[legacyExtensionKey];
    }
    else if (typeof window === 'object' && typeof window[extensionKey] !== 'undefined') {
        return window[extensionKey];
    }
    else {
        return null;
    }
}
export function _createState(devtools) {
    return devtools.state;
}
export function _createReducer(dispatcher, reducer) {
    return new Reducer(dispatcher, reducer);
}
export function _createStateIfExtension(extension, injector, initialState) {
    if (!!extension) {
        var devtools = injector.get(StoreDevtools);
        return _createState(devtools);
    }
    else {
        var dispatcher = injector.get(Dispatcher);
        var reducer = injector.get(Reducer);
        return new State(initialState, dispatcher, reducer);
    }
}
export function _createReducerIfExtension(extension, injector, reducer) {
    if (!!extension) {
        var devtoolsDispatcher = injector.get(DevtoolsDispatcher);
        return _createReducer(devtoolsDispatcher, reducer);
    }
    else {
        var dispatcher = injector.get(Dispatcher);
        return new Reducer(dispatcher, reducer);
    }
}
export function noMonitor() {
    return null;
}
export function _createOptions(_options) {
    var DEFAULT_OPTIONS = { monitor: noMonitor };
    var options = typeof _options === 'function' ? _options() : _options;
    options = Object.assign({}, DEFAULT_OPTIONS, options);
    if (options.maxAge && options.maxAge < 2) {
        throw new Error("Devtools 'maxAge' cannot be less than 2, got " + options.maxAge);
    }
    return options;
}
var StoreDevtoolsModule = (function () {
    function StoreDevtoolsModule() {
    }
    StoreDevtoolsModule.instrumentStore = function (_options) {
        if (_options === void 0) { _options = {}; }
        return {
            ngModule: StoreDevtoolsModule,
            providers: [
                {
                    provide: State,
                    deps: [StoreDevtools],
                    useFactory: _createState
                },
                {
                    provide: INITIAL_OPTIONS,
                    useValue: _options
                },
                {
                    provide: Reducer,
                    deps: [DevtoolsDispatcher, INITIAL_REDUCER],
                    useFactory: _createReducer
                },
                {
                    provide: STORE_DEVTOOLS_CONFIG,
                    deps: [INITIAL_OPTIONS],
                    useFactory: _createOptions
                }
            ]
        };
    };
    StoreDevtoolsModule.instrumentOnlyWithExtension = function (_options) {
        if (_options === void 0) { _options = {}; }
        return {
            ngModule: StoreDevtoolsModule,
            providers: [
                {
                    provide: State,
                    deps: [REDUX_DEVTOOLS_EXTENSION, Injector, INITIAL_STATE],
                    useFactory: _createStateIfExtension
                },
                {
                    provide: Reducer,
                    deps: [REDUX_DEVTOOLS_EXTENSION, Injector, INITIAL_REDUCER],
                    useFactory: _createReducerIfExtension
                },
                {
                    provide: INITIAL_OPTIONS,
                    useValue: _options
                },
                {
                    provide: STORE_DEVTOOLS_CONFIG,
                    deps: [INITIAL_OPTIONS],
                    useFactory: _createOptions
                }
            ]
        };
    };
    return StoreDevtoolsModule;
}());
export { StoreDevtoolsModule };
StoreDevtoolsModule.decorators = [
    { type: NgModule, args: [{
                imports: [
                    StoreModule
                ],
                providers: [
                    DevtoolsExtension,
                    DevtoolsDispatcher,
                    StoreDevtools,
                    {
                        provide: REDUX_DEVTOOLS_EXTENSION,
                        useFactory: _createReduxDevtoolsExtension
                    }
                ]
            },] },
];
/** @nocollapse */
StoreDevtoolsModule.ctorParameters = function () { return []; };
//# sourceMappingURL=instrument.js.map