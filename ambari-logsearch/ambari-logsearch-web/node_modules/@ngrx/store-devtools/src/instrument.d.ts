import { Injector, ModuleWithProviders } from '@angular/core';
import { Reducer } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { StoreDevtools, DevtoolsDispatcher } from './devtools';
import { StoreDevtoolsConfig } from './config';
export declare function _createReduxDevtoolsExtension(): any;
export declare function _createState(devtools: StoreDevtools): Observable<any>;
export declare function _createReducer(dispatcher: DevtoolsDispatcher, reducer: any): Reducer;
export declare function _createStateIfExtension(extension: any, injector: Injector, initialState: any): Observable<any>;
export declare function _createReducerIfExtension(extension: any, injector: Injector, reducer: any): Reducer;
export declare function noMonitor(): any;
export declare function _createOptions(_options: any): StoreDevtoolsConfig;
export declare class StoreDevtoolsModule {
    static instrumentStore(_options?: StoreDevtoolsConfig | (() => StoreDevtoolsConfig)): ModuleWithProviders;
    static instrumentOnlyWithExtension(_options?: StoreDevtoolsConfig | (() => StoreDevtoolsConfig)): ModuleWithProviders;
}
