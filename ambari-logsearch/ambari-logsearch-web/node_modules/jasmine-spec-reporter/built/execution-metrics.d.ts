export declare class ExecutionMetrics {
    private static pluralize(count);
    successfulSpecs: number;
    failedSpecs: number;
    pendingSpecs: number;
    skippedSpecs: number;
    totalSpecsDefined: number;
    executedSpecs: number;
    duration: string;
    random: boolean;
    seed: number;
    private startTime;
    private specStartTime;
    start(info: any): void;
    stop(info: any): void;
    startSpec(): void;
    stopSpec(spec: any): void;
    private formatDuration(durationInMs);
}
