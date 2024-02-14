import { Dispatcher, Reducer } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { Observer } from 'rxjs/Observer';
import { DevtoolsExtension } from './extension';
import { LiftedState } from './reducer';
import { StoreDevtoolsConfig } from './config';
export declare class DevtoolsDispatcher extends Dispatcher {
}
export declare class StoreDevtools implements Observer<any> {
    private stateSubscription;
    dispatcher: Dispatcher;
    liftedState: Observable<LiftedState>;
    state: Observable<any>;
    constructor(dispatcher: DevtoolsDispatcher, actions$: Dispatcher, reducers$: Reducer, extension: DevtoolsExtension, initialState: any, config: StoreDevtoolsConfig);
    dispatch(action: any): void;
    next(action: any): void;
    error(error: any): void;
    complete(): void;
    performAction(action: any): void;
    reset(): void;
    rollback(): void;
    commit(): void;
    sweep(): void;
    toggleAction(id: number): void;
    jumpToState(index: number): void;
    importState(nextLiftedState: any): void;
}
