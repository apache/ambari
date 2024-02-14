import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Dispatcher } from './dispatcher';
import { Reducer } from './reducer';
export declare class State<T> extends BehaviorSubject<T> {
    constructor(_initialState: T, action$: Dispatcher, reducer$: Reducer);
}
