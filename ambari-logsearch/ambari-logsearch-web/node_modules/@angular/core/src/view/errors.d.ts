import { DebugContext } from './types';
export declare function expressionChangedAfterItHasBeenCheckedError(context: DebugContext, oldValue: any, currValue: any, isFirstCheck: boolean): Error;
export declare function viewWrappedDebugError(err: any, context: DebugContext): Error;
export declare function viewDebugError(msg: string, context: DebugContext): Error;
export declare function isViewDebugError(err: Error): boolean;
export declare function viewDestroyedError(action: string): Error;
