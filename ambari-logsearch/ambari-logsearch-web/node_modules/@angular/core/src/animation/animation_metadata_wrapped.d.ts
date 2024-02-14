/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { AnimateTimings, AnimationMetadataType } from './dsl';
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare const AUTO_STYLE = "*";
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationMetadata {
    type: AnimationMetadataType;
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationTriggerMetadata {
    name: string;
    definitions: AnimationMetadata[];
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationStateMetadata extends AnimationMetadata {
    name: string;
    styles: AnimationStyleMetadata;
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationTransitionMetadata extends AnimationMetadata {
    expr: string | ((fromState: string, toState: string) => boolean);
    animation: AnimationMetadata | AnimationMetadata[];
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationKeyframesSequenceMetadata extends AnimationMetadata {
    steps: AnimationStyleMetadata[];
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationStyleMetadata extends AnimationMetadata {
    styles: '*' | {
        [key: string]: string | number;
    } | Array<{
        [key: string]: string | number;
    } | '*'>;
    offset: number | null;
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationAnimateMetadata extends AnimationMetadata {
    timings: string | number | AnimateTimings;
    styles: AnimationStyleMetadata | AnimationKeyframesSequenceMetadata | null;
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationSequenceMetadata extends AnimationMetadata {
    steps: AnimationMetadata[];
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export interface AnimationGroupMetadata extends AnimationMetadata {
    steps: AnimationMetadata[];
}
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function trigger(name: string, definitions: AnimationMetadata[]): AnimationTriggerMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function animate(timings: string | number, styles?: AnimationStyleMetadata | AnimationKeyframesSequenceMetadata): AnimationAnimateMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function group(steps: AnimationMetadata[]): AnimationGroupMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function sequence(steps: AnimationMetadata[]): AnimationSequenceMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function style(tokens: {
    [key: string]: string | number;
} | Array<{
    [key: string]: string | number;
}>): AnimationStyleMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function state(name: string, styles: AnimationStyleMetadata): AnimationStateMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function keyframes(steps: AnimationStyleMetadata[]): AnimationKeyframesSequenceMetadata;
/**
 * @deprecated This symbol has moved. Please Import from @angular/animations instead!
 */
export declare function transition(stateChangeExpr: string, steps: AnimationMetadata | AnimationMetadata[]): AnimationTransitionMetadata;
/**
 * @deprecated This has been renamed to `AnimationEvent`. Please import it from @angular/animations.
 */
export interface AnimationTransitionEvent {
    fromState: string;
    toState: string;
    totalTime: number;
    phaseName: string;
    element: any;
    triggerName: string;
}
