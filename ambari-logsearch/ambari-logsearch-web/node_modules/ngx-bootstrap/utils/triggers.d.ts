/**
 * @copyright Valor Software
 * @copyright Angular ng-bootstrap team
 */
import { Renderer } from '@angular/core';
import { Trigger } from './trigger.class';
import { ListenOptions } from '../component-loader/listen-options.model';
export declare function parseTriggers(triggers: string, aliases?: any): Trigger[];
export declare function listenToTriggers(renderer: Renderer, target: any, triggers: string, showFn: Function, hideFn: Function, toggleFn: Function): Function;
export declare function listenToTriggersV2(renderer: Renderer, options: ListenOptions): Function;
export declare function registerOutsideClick(renderer: Renderer, options: ListenOptions): Function;
