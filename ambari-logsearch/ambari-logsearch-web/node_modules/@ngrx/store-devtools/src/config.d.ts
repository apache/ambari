import { ActionReducer } from '@ngrx/store';
import { OpaqueToken } from '@angular/core';
export interface StoreDevtoolsConfig {
    maxAge?: number;
    monitor?: ActionReducer<any>;
}
export declare const STORE_DEVTOOLS_CONFIG: OpaqueToken;
export declare const INITIAL_OPTIONS: OpaqueToken;
