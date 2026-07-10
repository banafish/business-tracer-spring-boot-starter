package com.bananice.businesstracer.application;

/**
 * Port for reading and mutating the ambient trace context from higher layers
 * without depending on the infrastructure-level thread-local holder.
 */
public interface TraceContextPort {

    /**
     * Whether a trace context is currently active on this thread.
     */
    boolean hasActiveTrace();

    /**
     * Business id of the active trace, or {@code null} when none is active.
     */
    String currentBusinessId();

    /**
     * Node id of the active trace, or {@code null} when none is active.
     */
    String currentNodeId();

    /**
     * Flag the active trace as having recorded an error; no-op when none is active.
     */
    void markErrorRecorded();
}
