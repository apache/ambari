import { OpaqueToken } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { LiftedState } from './reducer';
export declare const ExtensionActionTypes: {
    START: string;
    DISPATCH: string;
    STOP: string;
    ACTION: string;
};
export declare const REDUX_DEVTOOLS_EXTENSION: OpaqueToken;
export interface ReduxDevtoolsExtensionConnection {
    subscribe(listener: (change: any) => void): any;
    unsubscribe(): any;
    send(action: any, state: any): any;
}
export interface ReduxDevtoolsExtension {
    connect(options: {
        shouldStringify?: boolean;
        instanceId: string;
    }): ReduxDevtoolsExtensionConnection;
    send(action: any, state: any, shouldStringify?: boolean, instanceId?: string): any;
}
export declare class DevtoolsExtension {
    private instanceId;
    private devtoolsExtension;
    liftedActions$: Observable<any>;
    actions$: Observable<any>;
    constructor(devtoolsExtension: any);
    notify(action: any, state: LiftedState): void;
    private createChangesObservable();
    private createActionStreams();
    private unwrapAction(action);
}
