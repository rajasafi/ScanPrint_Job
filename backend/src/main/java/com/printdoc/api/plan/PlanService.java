package com.printdoc.api.plan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PlanService {
    private final PlanRepository plans;
    public PlanService(PlanRepository plans){this.plans=plans;}
    @Transactional(readOnly=true) public List<PlanDtos.PlanView> catalog(){return plans.findByActiveTrueOrderByMonthlyPriceMinorAsc().stream().map(p->new PlanDtos.PlanView(p.getId(),p.getCode(),p.getName(),p.getDescription(),p.getMonthlyPriceMinor(),"INR",p.getPrinterLimit(),p.getDailyJobLimit(),p.getMaxFileSizeMb())).toList();}
}
