package com.printdoc.api.plan;

import java.util.UUID;

public final class PlanDtos {
    private PlanDtos(){}
    public record PlanView(UUID id,String code,String name,String description,long monthlyPriceMinor,String currency,int printerLimit,int dailyJobLimit,int maxFileSizeMb){}
}
