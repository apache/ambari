/**
 * AnimationPlayer controls an animation sequence that was produced from a programmatic animation.
 * (see {@link AnimationBuilder AnimationBuilder} for more information on how to create programmatic
 * animations.)
 *
 * @experimental Animation support is experimental.
 */
export interface AnimationPlayer {
    onDone(fn: () => void): void;
    onStart(fn: () => void): void;
    onDestroy(fn: () => void): void;
    init(): void;
    hasStarted(): boolean;
    play(): void;
    pause(): void;
    restart(): void;
    finish(): void;
    destroy(): void;
    reset(): void;
    setPosition(p: any): void;
    getPosition(): number;
    parentPlayer: AnimationPlayer | null;
    readonly totalTime: number;
    beforeDestroy?: () => any;
}
/**
 * @experimental Animation support is experimental.
 */
export declare class NoopAnimationPlayer implements AnimationPlayer {
    private _onDoneFns;
    private _onStartFns;
    private _onDestroyFns;
    private _started;
    private _destroyed;
    private _finished;
    parentPlayer: AnimationPlayer | null;
    totalTime: number;
    constructor();
    private _onFinish();
    onStart(fn: () => void): void;
    onDone(fn: () => void): void;
    onDestroy(fn: () => void): void;
    hasStarted(): boolean;
    init(): void;
    play(): void;
    private _onStart();
    pause(): void;
    restart(): void;
    finish(): void;
    destroy(): void;
    reset(): void;
    setPosition(p: number): void;
    getPosition(): number;
}
