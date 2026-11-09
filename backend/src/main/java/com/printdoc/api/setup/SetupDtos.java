package com.printdoc.api.setup;

import java.util.List;

public final class SetupDtos {
    private SetupDtos() {}

    public enum State { READY, PARTIAL, MISSING, BLOCKED }

    public record Step(String id, int number, String title, State state, String note, String requiredComponent) {}

    public record Status(String shopCode, int readySteps, int totalSteps, List<Step> steps, List<String> blockingGaps) {}
}
