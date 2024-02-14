var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
import { Injectable, Inject } from '@angular/core';
import { INITIAL_STATE, Dispatcher, Reducer } from '@ngrx/store';
import { ReplaySubject } from 'rxjs/ReplaySubject';
import { map } from 'rxjs/operator/map';
import { merge } from 'rxjs/operator/merge';
import { observeOn } from 'rxjs/operator/observeOn';
import { scan } from 'rxjs/operator/scan';
import { skip } from 'rxjs/operator/skip';
import { withLatestFrom } from 'rxjs/operator/withLatestFrom';
import { queue } from 'rxjs/scheduler/queue';
import { DevtoolsExtension } from './extension';
import { liftAction, unliftState, applyOperators } from './utils';
import { liftReducerWith, liftInitialState } from './reducer';
import { StoreDevtoolActions as actions } from './actions';
import { STORE_DEVTOOLS_CONFIG } from './config';
var DevtoolsDispatcher = (function (_super) {
    __extends(DevtoolsDispatcher, _super);
    function DevtoolsDispatcher() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    return DevtoolsDispatcher;
}(Dispatcher));
export { DevtoolsDispatcher };
DevtoolsDispatcher.decorators = [
    { type: Injectable },
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
            [skip, 1],
            [merge, extension.actions$],
            [map, liftAction],
            [merge, dispatcher, extension.liftedActions$],
            [observeOn, queue]
        ]);
        var liftedReducer$ = map.call(reducers$, liftReducer);
        var liftedStateSubject = new ReplaySubject(1);
        var liftedStateSubscription = applyOperators(liftedAction$, [
            [withLatestFrom, liftedReducer$],
            [scan, function (liftedState, _a) {
                    var action = _a[0], reducer = _a[1];
                    var nextState = reducer(liftedState, action);
                    extension.notify(action, nextState);
                    return nextState;
                }, liftedInitialState]
        ]).subscribe(liftedStateSubject);
        var liftedState$ = liftedStateSubject.asObservable();
        var state$ = map.call(liftedState$, unliftState);
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
        this.dispatch(actions.performAction(action));
    };
    StoreDevtools.prototype.reset = function () {
        this.dispatch(actions.reset());
    };
    StoreDevtools.prototype.rollback = function () {
        this.dispatch(actions.rollback());
    };
    StoreDevtools.prototype.commit = function () {
        this.dispatch(actions.commit());
    };
    StoreDevtools.prototype.sweep = function () {
        this.dispatch(actions.sweep());
    };
    StoreDevtools.prototype.toggleAction = function (id) {
        this.dispatch(actions.toggleAction(id));
    };
    StoreDevtools.prototype.jumpToState = function (index) {
        this.dispatch(actions.jumpToState(index));
    };
    StoreDevtools.prototype.importState = function (nextLiftedState) {
        this.dispatch(actions.importState(nextLiftedState));
    };
    return StoreDevtools;
}());
export { StoreDevtools };
StoreDevtools.decorators = [
    { type: Injectable },
];
/** @nocollapse */
StoreDevtools.ctorParameters = function () { return [
    { type: DevtoolsDispatcher, },
    { type: Dispatcher, },
    { type: Reducer, },
    { type: DevtoolsExtension, },
    { type: undefined, decorators: [{ type: Inject, args: [INITIAL_STATE,] },] },
    { type: undefined, decorators: [{ type: Inject, args: [STORE_DEVTOOLS_CONFIG,] },] },
]; };
//# sourceMappingURL=devtools.js.map