import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Dispatcher, Action } from './dispatcher';
export interface ActionReducer<T> {
    (state: T, action: Action): T;
}
export declare class Reducer extends BehaviorSubject<ActionReducer<any>> {
    private _dispatcher;
    static REPLACE: string;
    constructor(_dispatcher: Dispatcher, initialReducer: ActionReducer<any>);
    replaceReducer(reducer: ActionReducer<any>): void;
    next(reducer: ActionReducer<any>): void;
}
