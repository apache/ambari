export declare const ActionTypes: {
    PERFORM_ACTION: string;
    RESET: string;
    ROLLBACK: string;
    COMMIT: string;
    SWEEP: string;
    TOGGLE_ACTION: string;
    SET_ACTIONS_ACTIVE: string;
    JUMP_TO_STATE: string;
    IMPORT_STATE: string;
};
/**
* Action creators to change the History state.
*/
export declare const StoreDevtoolActions: {
    performAction(action: any): {
        type: string;
        action: any;
        timestamp: number;
    };
    reset(): {
        type: string;
        timestamp: number;
    };
    rollback(): {
        type: string;
        timestamp: number;
    };
    commit(): {
        type: string;
        timestamp: number;
    };
    sweep(): {
        type: string;
    };
    toggleAction(id: any): {
        type: string;
        id: any;
    };
    setActionsActive(start: any, end: any, active?: boolean): {
        type: string;
        start: any;
        end: any;
        active: boolean;
    };
    jumpToState(index: any): {
        type: string;
        index: any;
    };
    importState(nextLiftedState: any): {
        type: string;
        nextLiftedState: any;
    };
};
