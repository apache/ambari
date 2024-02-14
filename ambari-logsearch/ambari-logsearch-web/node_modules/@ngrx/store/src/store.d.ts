import { SelectSignature } from '@ngrx/core';
import { Observer } from 'rxjs/Observer';
import { Observable } from 'rxjs/Observable';
import { Operator } from 'rxjs/Operator';
import { Action } from './dispatcher';
import { ActionReducer } from './reducer';
export declare class Store<T> extends Observable<T> implements Observer<Action> {
    private _dispatcher;
    private _reducer;
    constructor(_dispatcher: Observer<Action>, _reducer: Observer<ActionReducer<any>>, state$: Observable<any>);
    select: SelectSignature<T>;
    lift<R>(operator: Operator<T, R>): Store<R>;
    replaceReducer(reducer: ActionReducer<any>): void;
    dispatch(action: Action): void;
    next(action: Action): void;
    error(err: any): void;
    complete(): void;
}
