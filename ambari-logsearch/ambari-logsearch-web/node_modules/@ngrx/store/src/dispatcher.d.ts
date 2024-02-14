import { BehaviorSubject } from 'rxjs/BehaviorSubject';
export interface Action {
    type: string;
    payload?: any;
}
export declare class Dispatcher extends BehaviorSubject<Action> {
    static INIT: string;
    constructor();
    dispatch(action: Action): void;
    complete(): void;
}
