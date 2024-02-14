import { Action } from '@ngrx/store';
export declare const INIT_ACTION: {
    type: string;
};
export interface LiftedState {
    monitorState: any;
    nextActionId: number;
    actionsById: {
        [id: number]: {
            action: Action;
        };
    };
    stagedActionIds: number[];
    skippedActionIds: number[];
    committedState: any;
    currentStateIndex: number;
    computedStates: {
        state: any;
        error: any;
    }[];
}
export declare function liftInitialState(initialCommittedState?: any, monitorReducer?: any): LiftedState;
/**
* Creates a history state reducer from an app's reducer.
*/
export declare function liftReducerWith(initialCommittedState: any, initialLiftedState: LiftedState, monitorReducer?: any, options?: {
    maxAge?: number;
}): (reducer: any) => (liftedState: any, liftedAction: any) => {
    monitorState: any;
    actionsById: any;
    nextActionId: any;
    stagedActionIds: any;
    skippedActionIds: any;
    committedState: any;
    currentStateIndex: any;
    computedStates: any;
};
