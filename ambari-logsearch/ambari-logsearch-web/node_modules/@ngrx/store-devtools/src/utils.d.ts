import { Action } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { LiftedState } from './reducer';
export declare function difference(first: any[], second: any[]): any[];
/**
 * Provides an app's view into the state of the lifted store.
 */
export declare function unliftState(liftedState: LiftedState): any;
export declare function unliftAction(liftedState: LiftedState): {
    action: Action;
};
/**
* Lifts an app's action into an action on the lifted store.
*/
export declare function liftAction(action: any): {
    type: string;
    action: any;
    timestamp: number;
};
export declare function applyOperators(input$: Observable<any>, operators: any[][]): Observable<any>;
