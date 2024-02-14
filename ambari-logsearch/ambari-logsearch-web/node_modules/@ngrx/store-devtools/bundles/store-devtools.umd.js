(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@angular/core'), require('@ngrx/store'), require('rxjs/ReplaySubject'), require('rxjs/operator/map'), require('rxjs/operator/merge'), require('rxjs/operator/observeOn'), require('rxjs/operator/scan'), require('rxjs/operator/skip'), require('rxjs/operator/withLatestFrom'), require('rxjs/scheduler/queue'), require('rxjs/Observable'), require('rxjs/observable/empty'), require('rxjs/operator/filter'), require('rxjs/operator/share'), require('rxjs/operator/switchMap'), require('rxjs/operator/takeUntil')) :
	typeof define === 'function' && define.amd ? define(['exports', '@angular/core', '@ngrx/store', 'rxjs/ReplaySubject', 'rxjs/operator/map', 'rxjs/operator/merge', 'rxjs/operator/observeOn', 'rxjs/operator/scan', 'rxjs/operator/skip', 'rxjs/operator/withLatestFrom', 'rxjs/scheduler/queue', 'rxjs/Observable', 'rxjs/observable/empty', 'rxjs/operator/filter', 'rxjs/operator/share', 'rxjs/operator/switchMap', 'rxjs/operator/takeUntil'], factory) :
	(factory((global.ngrx = global.ngrx || {}, global.ngrx.storeDevtools = global.ngrx.storeDevtools || {}),global.ng.core,global.ngrx.store,global.Rx,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Scheduler,global.Rx,global.Rx.Observable,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype,global.Rx.Observable.prototype));
}(this, (function (exports,_angular_core,_ngrx_store,rxjs_ReplaySubject,rxjs_operator_map,rxjs_operator_merge,rxjs_operator_observeOn,rxjs_operator_scan,rxjs_operator_skip,rxjs_operator_withLatestFrom,rxjs_scheduler_queue,rxjs_Observable,rxjs_observable_empty,rxjs_operator_filter,rxjs_operator_share,rxjs_operator_switchMap,rxjs_operator_takeUntil) { 'use strict';

var ActionTypes = {
    PERFORM_ACTION: 'PERFORM_ACTION',
    RESET: 'RESET',
    ROLLBACK: 'ROLLBACK',
    COMMIT: 'COMMIT',
    SWEEP: 'SWEEP',
    TOGGLE_ACTION: 'TOGGLE_ACTION',
    SET_ACTIONS_ACTIVE: 'SET_ACTIONS_ACTIVE',
    JUMP_TO_STATE: 'JUMP_TO_STATE',
    IMPORT_STATE: 'IMPORT_STATE'
};
/**
* Action creators to change the History state.
*/
var StoreDevtoolActions = {
    performAction: function (action) {
        if (typeof action.type === 'undefined') {
            throw new Error('Actions may not have an undefined "type" property. ' +
                'Have you misspelled a constant?');
        }
        return { type: ActionTypes.PERFORM_ACTION, action: action, timestamp: Date.now() };
    },
    reset: function () {
        return { type: ActionTypes.RESET, timestamp: Date.now() };
    },
    rollback: function () {
        return { type: ActionTypes.ROLLBACK, timestamp: Date.now() };
    },
    commit: function () {
        return { type: ActionTypes.COMMIT, timestamp: Date.now() };
    },
    sweep: function () {
        return { type: ActionTypes.SWEEP };
    },
    toggleAction: function (id) {
        return { type: ActionTypes.TOGGLE_ACTION, id: id };
    },
    setActionsActive: function (start, end, active) {
        if (active === void 0) { active = true; }
        return { type: ActionTypes.SET_ACTIONS_ACTIVE, start: start, end: end, active: active };
    },
    jumpToState: function (index) {
        return { type: ActionTypes.JUMP_TO_STATE, index: index };
    },
    importState: function (nextLiftedState) {
        return { type: ActionTypes.IMPORT_STATE, nextLiftedState: nextLiftedState };
    }
};

function difference(first, second) {
    return first.filter(function (item) { return second.indexOf(item) < 0; });
}
/**
 * Provides an app's view into the state of the lifted store.
 */
function unliftState(liftedState) {
    var computedStates = liftedState.computedStates, currentStateIndex = liftedState.currentStateIndex;
    var state = computedStates[currentStateIndex].state;
    return state;
}

/**
* Lifts an app's action into an action on the lifted store.
*/
function liftAction(action) {
    return StoreDevtoolActions.performAction(action);
}
function applyOperators(input$, operators) {
    return operators.reduce(function (source$, _a) {
        var operator = _a[0], args = _a.slice(1);
        return operator.apply(source$, args);
    }, input$);
}

var ExtensionActionTypes = {
    START: 'START',
    DISPATCH: 'DISPATCH',
    STOP: 'STOP',
    ACTION: 'ACTION'
};
var REDUX_DEVTOOLS_EXTENSION = new _angular_core.OpaqueToken('Redux Devtools Extension');
var DevtoolsExtension = (function () {
    function DevtoolsExtension(devtoolsExtension) {
        this.instanceId = "ngrx-store-" + Date.now();
        this.devtoolsExtension = devtoolsExtension;
        this.createActionStreams();
    }
    DevtoolsExtension.prototype.notify = function (action, state) {
        if (!this.devtoolsExtension) {
            return;
        }
        this.devtoolsExtension.send(null, state, false, this.instanceId);
    };
    DevtoolsExtension.prototype.createChangesObservable = function () {
        var _this = this;
        if (!this.devtoolsExtension) {
            return rxjs_observable_empty.empty();
        }
        return new rxjs_Observable.Observable(function (subscriber) {
            var connection = _this.devtoolsExtension.connect({ instanceId: _this.instanceId });
            connection.subscribe(function (change) { return subscriber.next(change); });
            return connection.unsubscribe;
        });
    };
    DevtoolsExtension.prototype.createActionStreams = function () {
        var _this = this;
        // Listens to all changes based on our instanceId
        var changes$ = rxjs_operator_share.share.call(this.createChangesObservable());
        // Listen for the start action
        var start$ = rxjs_operator_filter.filter.call(changes$, function (change) { return change.type === ExtensionActionTypes.START; });
        // Listen for the stop action
        var stop$ = rxjs_operator_filter.filter.call(changes$, function (change) { return change.type === ExtensionActionTypes.STOP; });
        // Listen for lifted actions
        var liftedActions$ = applyOperators(changes$, [
            [rxjs_operator_filter.filter, function (change) { return change.type === ExtensionActionTypes.DISPATCH; }],
            [rxjs_operator_map.map, function (change) { return _this.unwrapAction(change.payload); }]
        ]);
        // Listen for unlifted actions
        var actions$ = applyOperators(changes$, [
            [rxjs_operator_filter.filter, function (change) { return change.type === ExtensionActionTypes.ACTION; }],
            [rxjs_operator_map.map, function (change) { return _this.unwrapAction(change.payload); }]
        ]);
        var actionsUntilStop$ = rxjs_operator_takeUntil.takeUntil.call(actions$, stop$);
        var liftedUntilStop$ = rxjs_operator_takeUntil.takeUntil.call(liftedActions$, stop$);
        // Only take the action sources between the start/stop events
        this.actions$ = rxjs_operator_switchMap.switchMap.call(start$, function () { return actionsUntilStop$; });
        this.liftedActions$ = rxjs_operator_switchMap.switchMap.call(start$, function () { return liftedUntilStop$; });
    };
    DevtoolsExtension.prototype.unwrapAction = function (action) {
        return typeof action === 'string' ? eval("(" + action + ")") : action;
    };
    return DevtoolsExtension;
}());
DevtoolsExtension.decorators = [
    { type: _angular_core.Injectable },
];
/** @nocollapse */
DevtoolsExtension.ctorParameters = function () { return [
    { type: undefined, decorators: [{ type: _angular_core.Inject, args: [REDUX_DEVTOOLS_EXTENSION,] },] },
]; };

var INIT_ACTION = { type: _ngrx_store.Dispatcher.INIT };
/**
* Computes the next entry in the log by applying an action.
*/
function computeNextEntry(reducer, action, state, error) {
    if (error) {
        return {
            state: state,
            error: 'Interrupted by an error up the chain'
        };
    }
    var nextState = state;
    var nextError;
    try {
        nextState = reducer(state, action);
    }
    catch (err) {
        nextError = err.toString();
        console.error(err.stack || err);
    }
    return {
        state: nextState,
        error: nextError
    };
}
/**
* Runs the reducer on invalidated actions to get a fresh computation log.
*/
function recomputeStates(computedStates, minInvalidatedStateIndex, reducer, committedState, actionsById, stagedActionIds, skippedActionIds) {
    // Optimization: exit early and return the same reference
    // if we know nothing could have changed.
    if (minInvalidatedStateIndex >= computedStates.length &&
        computedStates.length === stagedActionIds.length) {
        return computedStates;
    }
    var nextComputedStates = computedStates.slice(0, minInvalidatedStateIndex);
    for (var i = minInvalidatedStateIndex; i < stagedActionIds.length; i++) {
        var actionId = stagedActionIds[i];
        var action = actionsById[actionId].action;
        var previousEntry = nextComputedStates[i - 1];
        var previousState = previousEntry ? previousEntry.state : committedState;
        var previousError = previousEntry ? previousEntry.error : undefined;
        var shouldSkip = skippedActionIds.indexOf(actionId) > -1;
        var entry = shouldSkip ?
            previousEntry :
            computeNextEntry(reducer, action, previousState, previousError);
        nextComputedStates.push(entry);
    }
    return nextComputedStates;
}
function liftInitialState(initialCommittedState, monitorReducer) {
    return {
        monitorState: monitorReducer(undefined, {}),
        nextActionId: 1,
        actionsById: { 0: liftAction(INIT_ACTION) },
        stagedActionIds: [0],
        skippedActionIds: [],
        committedState: initialCommittedState,
        currentStateIndex: 0,
        computedStates: []
    };
}
/**
* Creates a history state reducer from an app's reducer.
*/
function liftReducerWith(initialCommittedState, initialLiftedState, monitorReducer, options) {
    if (options === void 0) { options = {}; }
    /**
    * Manages how the history actions modify the history state.
    */
    return function (reducer) { return function (liftedState, liftedAction) {
        var _a = liftedState || initialLiftedState, monitorState = _a.monitorState, actionsById = _a.actionsById, nextActionId = _a.nextActionId, stagedActionIds = _a.stagedActionIds, skippedActionIds = _a.skippedActionIds, committedState = _a.committedState, currentStateIndex = _a.currentStateIndex, computedStates = _a.computedStates;
        if (!liftedState) {
            // Prevent mutating initialLiftedState
            actionsById = Object.create(actionsById);
        }
        function commitExcessActions(n) {
            // Auto-commits n-number of excess actions.
            var excess = n;
            var idsToDelete = stagedActionIds.slice(1, excess + 1);
            for (var i = 0; i < idsToDelete.length; i++) {
                if (computedStates[i + 1].error) {
                    // Stop if error is found. Commit actions up to error.
                    excess = i;
                    idsToDelete = stagedActionIds.slice(1, excess + 1);
                    break;
                }
                else {
                    delete actionsById[idsToDelete[i]];
                }
            }
            skippedActionIds = skippedActionIds.filter(function (id) { return idsToDelete.indexOf(id) === -1; });
            stagedActionIds = [0].concat(stagedActionIds.slice(excess + 1));
            committedState = computedStates[excess].state;
            computedStates = computedStates.slice(excess);
            currentStateIndex = currentStateIndex > excess
                ? currentStateIndex - excess
                : 0;
        }
        // By default, agressively recompute every state whatever happens.
        // This has O(n) performance, so we'll override this to a sensible
        // value whenever we feel like we don't have to recompute the states.
        var minInvalidatedStateIndex = 0;
        switch (liftedAction.type) {
            case ActionTypes.RESET: {
                // Get back to the state the store was created with.
                actionsById = { 0: liftAction(INIT_ACTION) };
                nextActionId = 1;
                stagedActionIds = [0];
                skippedActionIds = [];
                committedState = initialCommittedState;
                currentStateIndex = 0;
                computedStates = [];
                break;
            }
            case ActionTypes.COMMIT: {
                // Consider the last committed state the new starting point.
                // Squash any staged actions into a single committed state.
                actionsById = { 0: liftAction(INIT_ACTION) };
                nextActionId = 1;
                stagedActionIds = [0];
                skippedActionIds = [];
                committedState = computedStates[currentStateIndex].state;
                currentStateIndex = 0;
                computedStates = [];
                break;
            }
            case ActionTypes.ROLLBACK: {
                // Forget about any staged actions.
                // Start again from the last committed state.
                actionsById = { 0: liftAction(INIT_ACTION) };
                nextActionId = 1;
                stagedActionIds = [0];
                skippedActionIds = [];
                currentStateIndex = 0;
                computedStates = [];
                break;
            }
            case ActionTypes.TOGGLE_ACTION: {
                // Toggle whether an action with given ID is skipped.
                // Being skipped means it is a no-op during the computation.
                var actionId_1 = liftedAction.id;
                var index = skippedActionIds.indexOf(actionId_1);
                if (index === -1) {
                    skippedActionIds = [actionId_1].concat(skippedActionIds);
                }
                else {
                    skippedActionIds = skippedActionIds.filter(function (id) { return id !== actionId_1; });
                }
                // Optimization: we know history before this action hasn't changed
                minInvalidatedStateIndex = stagedActionIds.indexOf(actionId_1);
                break;
            }
            case ActionTypes.SET_ACTIONS_ACTIVE: {
                // Toggle whether an action with given ID is skipped.
                // Being skipped means it is a no-op during the computation.
                var start = liftedAction.start, end = liftedAction.end, active = liftedAction.active;
                var actionIds = [];
                for (var i = start; i < end; i++)
                    actionIds.push(i);
                if (active) {
                    skippedActionIds = difference(skippedActionIds, actionIds);
                }
                else {
                    skippedActionIds = skippedActionIds.concat(actionIds);
                }
                // Optimization: we know history before this action hasn't changed
                minInvalidatedStateIndex = stagedActionIds.indexOf(start);
                break;
            }
            case ActionTypes.JUMP_TO_STATE: {
                // Without recomputing anything, move the pointer that tell us
                // which state is considered the current one. Useful for sliders.
                currentStateIndex = liftedAction.index;
                // Optimization: we know the history has not changed.
                minInvalidatedStateIndex = Infinity;
                break;
            }
            case ActionTypes.SWEEP: {
                // Forget any actions that are currently being skipped.
                stagedActionIds = difference(stagedActionIds, skippedActionIds);
                skippedActionIds = [];
                currentStateIndex = Math.min(currentStateIndex, stagedActionIds.length - 1);
                break;
            }
            case ActionTypes.PERFORM_ACTION: {
                // Auto-commit as new actions come in.
                if (options.maxAge && stagedActionIds.length === options.maxAge) {
                    commitExcessActions(1);
                }
                if (currentStateIndex === stagedActionIds.length - 1) {
                    currentStateIndex++;
                }
                var actionId = nextActionId++;
                // Mutation! This is the hottest path, and we optimize on purpose.
                // It is safe because we set a new key in a cache dictionary.
                actionsById[actionId] = liftedAction;
                stagedActionIds = stagedActionIds.concat([actionId]);
                // Optimization: we know that only the new action needs computing.
                minInvalidatedStateIndex = stagedActionIds.length - 1;
                break;
            }
            case ActionTypes.IMPORT_STATE: {
                // Completely replace everything.
                (_b = liftedAction.nextLiftedState, monitorState = _b.monitorState, actionsById = _b.actionsById, nextActionId = _b.nextActionId, stagedActionIds = _b.stagedActionIds, skippedActionIds = _b.skippedActionIds, committedState = _b.committedState, currentStateIndex = _b.currentStateIndex, computedStates = _b.computedStates);
                break;
            }
            case _ngrx_store.Reducer.REPLACE:
            case _ngrx_store.Dispatcher.INIT: {
                // Always recompute states on hot reload and init.
                minInvalidatedStateIndex = 0;
                if (options.maxAge && stagedActionIds.length > options.maxAge) {
                    // States must be recomputed before committing excess.
                    computedStates = recomputeStates(computedStates, minInvalidatedStateIndex, reducer, committedState, actionsById, stagedActionIds, skippedActionIds);
                    commitExcessActions(stagedActionIds.length - options.maxAge);
                    // Avoid double computation.
                    minInvalidatedStateIndex = Infinity;
                }
                break;
            }
            default: {
                // If the action is not recognized, it's a monitor action.
                // Optimization: a monitor action can't change history.
                minInvalidatedStateIndex = Infinity;
                break;
            }
        }
        computedStates = recomputeStates(computedStates, minInvalidatedStateIndex, reducer, committedState, actionsById, stagedActionIds, skippedActionIds);
        monitorState = monitorReducer(monitorState, liftedAction);
        return {
            monitorState: monitorState,
            actionsById: actionsById,
            nextActionId: nextActionId,
            stagedActionIds: stagedActionIds,
            skippedActionIds: skippedActionIds,
            committedState: committedState,
            currentStateIndex: currentStateIndex,
            computedStates: computedStates
        };
        var _b;
    }; };
}

var STORE_DEVTOOLS_CONFIG = new _angular_core.OpaqueToken('@ngrx/devtools Options');
var INITIAL_OPTIONS = new _angular_core.OpaqueToken('@ngrx/devtools Initial Config');

var __extends = (undefined && undefined.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var DevtoolsDispatcher = (function (_super) {
    __extends(DevtoolsDispatcher, _super);
    function DevtoolsDispatcher() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    return DevtoolsDispatcher;
}(_ngrx_store.Dispatcher));
DevtoolsDispatcher.decorators = [
    { type: _angular_core.Injectable },
];
/** @nocollapse */
DevtoolsDispatcher.ctorParameters = function () { return []; };
var StoreDevtools = (function () {
    function StoreDevtools(dispatcher, actions$, reducers$, extension, initialState, config) {
        var liftedInitialState = liftInitialState(initialState, config.monitor);
        var liftReducer = liftReducerWith(initialState, liftedInitialState, config.monitor, {
            maxAge: config.maxAge
        });
        var liftedAction$ = applyOperators(actions$, [
            [rxjs_operator_skip.skip, 1],
            [rxjs_operator_merge.merge, extension.actions$],
            [rxjs_operator_map.map, liftAction],
            [rxjs_operator_merge.merge, dispatcher, extension.liftedActions$],
            [rxjs_operator_observeOn.observeOn, rxjs_scheduler_queue.queue]
        ]);
        var liftedReducer$ = rxjs_operator_map.map.call(reducers$, liftReducer);
        var liftedStateSubject = new rxjs_ReplaySubject.ReplaySubject(1);
        var liftedStateSubscription = applyOperators(liftedAction$, [
            [rxjs_operator_withLatestFrom.withLatestFrom, liftedReducer$],
            [rxjs_operator_scan.scan, function (liftedState, _a) {
                    var action = _a[0], reducer = _a[1];
                    var nextState = reducer(liftedState, action);
                    extension.notify(action, nextState);
                    return nextState;
                }, liftedInitialState]
        ]).subscribe(liftedStateSubject);
        var liftedState$ = liftedStateSubject.asObservable();
        var state$ = rxjs_operator_map.map.call(liftedState$, unliftState);
        this.stateSubscription = liftedStateSubscription;
        this.dispatcher = dispatcher;
        this.liftedState = liftedState$;
        this.state = state$;
    }
    StoreDevtools.prototype.dispatch = function (action) {
        this.dispatcher.dispatch(action);
    };
    StoreDevtools.prototype.next = function (action) {
        this.dispatcher.dispatch(action);
    };
    StoreDevtools.prototype.error = function (error) { };
    StoreDevtools.prototype.complete = function () { };
    StoreDevtools.prototype.performAction = function (action) {
        this.dispatch(StoreDevtoolActions.performAction(action));
    };
    StoreDevtools.prototype.reset = function () {
        this.dispatch(StoreDevtoolActions.reset());
    };
    StoreDevtools.prototype.rollback = function () {
        this.dispatch(StoreDevtoolActions.rollback());
    };
    StoreDevtools.prototype.commit = function () {
        this.dispatch(StoreDevtoolActions.commit());
    };
    StoreDevtools.prototype.sweep = function () {
        this.dispatch(StoreDevtoolActions.sweep());
    };
    StoreDevtools.prototype.toggleAction = function (id) {
        this.dispatch(StoreDevtoolActions.toggleAction(id));
    };
    StoreDevtools.prototype.jumpToState = function (index) {
        this.dispatch(StoreDevtoolActions.jumpToState(index));
    };
    StoreDevtools.prototype.importState = function (nextLiftedState) {
        this.dispatch(StoreDevtoolActions.importState(nextLiftedState));
    };
    return StoreDevtools;
}());
StoreDevtools.decorators = [
    { type: _angular_core.Injectable },
];
/** @nocollapse */
StoreDevtools.ctorParameters = function () { return [
    { type: DevtoolsDispatcher, },
    { type: _ngrx_store.Dispatcher, },
    { type: _ngrx_store.Reducer, },
    { type: DevtoolsExtension, },
    { type: undefined, decorators: [{ type: _angular_core.Inject, args: [_ngrx_store.INITIAL_STATE,] },] },
    { type: undefined, decorators: [{ type: _angular_core.Inject, args: [STORE_DEVTOOLS_CONFIG,] },] },
]; };

function _createReduxDevtoolsExtension() {
    var legacyExtensionKey = 'devToolsExtension';
    var extensionKey = '__REDUX_DEVTOOLS_EXTENSION__';
    if (typeof window === 'object' && typeof window[legacyExtensionKey] !== 'undefined') {
        return window[legacyExtensionKey];
    }
    else if (typeof window === 'object' && typeof window[extensionKey] !== 'undefined') {
        return window[extensionKey];
    }
    else {
        return null;
    }
}
function _createState(devtools) {
    return devtools.state;
}
function _createReducer(dispatcher, reducer) {
    return new _ngrx_store.Reducer(dispatcher, reducer);
}
function _createStateIfExtension(extension, injector, initialState) {
    if (!!extension) {
        var devtools = injector.get(StoreDevtools);
        return _createState(devtools);
    }
    else {
        var dispatcher = injector.get(_ngrx_store.Dispatcher);
        var reducer = injector.get(_ngrx_store.Reducer);
        return new _ngrx_store.State(initialState, dispatcher, reducer);
    }
}
function _createReducerIfExtension(extension, injector, reducer) {
    if (!!extension) {
        var devtoolsDispatcher = injector.get(DevtoolsDispatcher);
        return _createReducer(devtoolsDispatcher, reducer);
    }
    else {
        var dispatcher = injector.get(_ngrx_store.Dispatcher);
        return new _ngrx_store.Reducer(dispatcher, reducer);
    }
}
function noMonitor() {
    return null;
}
function _createOptions(_options) {
    var DEFAULT_OPTIONS = { monitor: noMonitor };
    var options = typeof _options === 'function' ? _options() : _options;
    options = Object.assign({}, DEFAULT_OPTIONS, options);
    if (options.maxAge && options.maxAge < 2) {
        throw new Error("Devtools 'maxAge' cannot be less than 2, got " + options.maxAge);
    }
    return options;
}
var StoreDevtoolsModule = (function () {
    function StoreDevtoolsModule() {
    }
    StoreDevtoolsModule.instrumentStore = function (_options) {
        if (_options === void 0) { _options = {}; }
        return {
            ngModule: StoreDevtoolsModule,
            providers: [
                {
                    provide: _ngrx_store.State,
                    deps: [StoreDevtools],
                    useFactory: _createState
                },
                {
                    provide: INITIAL_OPTIONS,
                    useValue: _options
                },
                {
                    provide: _ngrx_store.Reducer,
                    deps: [DevtoolsDispatcher, _ngrx_store.INITIAL_REDUCER],
                    useFactory: _createReducer
                },
                {
                    provide: STORE_DEVTOOLS_CONFIG,
                    deps: [INITIAL_OPTIONS],
                    useFactory: _createOptions
                }
            ]
        };
    };
    StoreDevtoolsModule.instrumentOnlyWithExtension = function (_options) {
        if (_options === void 0) { _options = {}; }
        return {
            ngModule: StoreDevtoolsModule,
            providers: [
                {
                    provide: _ngrx_store.State,
                    deps: [REDUX_DEVTOOLS_EXTENSION, _angular_core.Injector, _ngrx_store.INITIAL_STATE],
                    useFactory: _createStateIfExtension
                },
                {
                    provide: _ngrx_store.Reducer,
                    deps: [REDUX_DEVTOOLS_EXTENSION, _angular_core.Injector, _ngrx_store.INITIAL_REDUCER],
                    useFactory: _createReducerIfExtension
                },
                {
                    provide: INITIAL_OPTIONS,
                    useValue: _options
                },
                {
                    provide: STORE_DEVTOOLS_CONFIG,
                    deps: [INITIAL_OPTIONS],
                    useFactory: _createOptions
                }
            ]
        };
    };
    return StoreDevtoolsModule;
}());
StoreDevtoolsModule.decorators = [
    { type: _angular_core.NgModule, args: [{
                imports: [
                    _ngrx_store.StoreModule
                ],
                providers: [
                    DevtoolsExtension,
                    DevtoolsDispatcher,
                    StoreDevtools,
                    {
                        provide: REDUX_DEVTOOLS_EXTENSION,
                        useFactory: _createReduxDevtoolsExtension
                    }
                ]
            },] },
];
/** @nocollapse */
StoreDevtoolsModule.ctorParameters = function () { return []; };

exports.StoreDevtoolsModule = StoreDevtoolsModule;
exports.StoreDevtools = StoreDevtools;

Object.defineProperty(exports, '__esModule', { value: true });

})));
