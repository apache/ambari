import { OpaqueToken, ModuleWithProviders } from '@angular/core';
import { Reducer } from './reducer';
import { Dispatcher } from './dispatcher';
import { Store } from './store';
import { State } from './state';
export declare const INITIAL_REDUCER: OpaqueToken;
export declare const INITIAL_STATE: OpaqueToken;
export declare const _INITIAL_REDUCER: OpaqueToken;
export declare const _INITIAL_STATE: OpaqueToken;
export declare function _initialReducerFactory(reducer: any): any;
export declare function _initialStateFactory(initialState: any, reducer: any): any;
export declare function _storeFactory(dispatcher: any, reducer: any, state$: any): Store<{}>;
export declare function _stateFactory(initialState: any, dispatcher: Dispatcher, reducer: Reducer): State<any>;
export declare function _reducerFactory(dispatcher: any, reducer: any): Reducer;
/**
 * @deprecated, use StoreModule.provideStore instead!
 */
export declare function provideStore(_reducer: any, _initialState?: any): any[];
export declare class StoreModule {
    static provideStore(_reducer: any, _initialState?: any): ModuleWithProviders;
}
