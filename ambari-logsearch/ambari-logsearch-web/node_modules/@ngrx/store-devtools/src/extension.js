import { OpaqueToken, Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { empty } from 'rxjs/observable/empty';
import { filter } from 'rxjs/operator/filter';
import { map } from 'rxjs/operator/map';
import { share } from 'rxjs/operator/share';
import { switchMap } from 'rxjs/operator/switchMap';
import { takeUntil } from 'rxjs/operator/takeUntil';
import { applyOperators } from './utils';
export var ExtensionActionTypes = {
    START: 'START',
    DISPATCH: 'DISPATCH',
    STOP: 'STOP',
    ACTION: 'ACTION'
};
export var REDUX_DEVTOOLS_EXTENSION = new OpaqueToken('Redux Devtools Extension');
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
            return empty();
        }
        return new Observable(function (subscriber) {
            var connection = _this.devtoolsExtension.connect({ instanceId: _this.instanceId });
            connection.subscribe(function (change) { return subscriber.next(change); });
            return connection.unsubscribe;
        });
    };
    DevtoolsExtension.prototype.createActionStreams = function () {
        var _this = this;
        // Listens to all changes based on our instanceId
        var changes$ = share.call(this.createChangesObservable());
        // Listen for the start action
        var start$ = filter.call(changes$, function (change) { return change.type === ExtensionActionTypes.START; });
        // Listen for the stop action
        var stop$ = filter.call(changes$, function (change) { return change.type === ExtensionActionTypes.STOP; });
        // Listen for lifted actions
        var liftedActions$ = applyOperators(changes$, [
            [filter, function (change) { return change.type === ExtensionActionTypes.DISPATCH; }],
            [map, function (change) { return _this.unwrapAction(change.payload); }]
        ]);
        // Listen for unlifted actions
        var actions$ = applyOperators(changes$, [
            [filter, function (change) { return change.type === ExtensionActionTypes.ACTION; }],
            [map, function (change) { return _this.unwrapAction(change.payload); }]
        ]);
        var actionsUntilStop$ = takeUntil.call(actions$, stop$);
        var liftedUntilStop$ = takeUntil.call(liftedActions$, stop$);
        // Only take the action sources between the start/stop events
        this.actions$ = switchMap.call(start$, function () { return actionsUntilStop$; });
        this.liftedActions$ = switchMap.call(start$, function () { return liftedUntilStop$; });
    };
    DevtoolsExtension.prototype.unwrapAction = function (action) {
        return typeof action === 'string' ? eval("(" + action + ")") : action;
    };
    return DevtoolsExtension;
}());
export { DevtoolsExtension };
DevtoolsExtension.decorators = [
    { type: Injectable },
];
/** @nocollapse */
DevtoolsExtension.ctorParameters = function () { return [
    { type: undefined, decorators: [{ type: Inject, args: [REDUX_DEVTOOLS_EXTENSION,] },] },
]; };
//# sourceMappingURL=extension.js.map