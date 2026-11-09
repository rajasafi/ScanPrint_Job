package com.printdoc.api.plan;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="plans",uniqueConstraints=@UniqueConstraint(name="uk_plan_code",columnNames="code"))
public class Plan {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false,length=32) private String code;
    @Column(nullable=false,length=80) private String name;
    @Column(nullable=false,length=240) private String description;
    @Column(name="monthly_price_minor",nullable=false) private long monthlyPriceMinor;
    @Column(name="printer_limit",nullable=false) private int printerLimit;
    @Column(name="daily_job_limit",nullable=false) private int dailyJobLimit;
    @Column(name="max_file_size_mb",nullable=false) private int maxFileSizeMb;
    @Column(nullable=false) private boolean active=true;
    protected Plan() {}
    public Plan(String code,String name,String description,long monthlyPriceMinor,int printerLimit,int dailyJobLimit,int maxFileSizeMb){this.code=code;this.name=name;this.description=description;this.monthlyPriceMinor=monthlyPriceMinor;this.printerLimit=printerLimit;this.dailyJobLimit=dailyJobLimit;this.maxFileSizeMb=maxFileSizeMb;}
    public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public long getMonthlyPriceMinor(){return monthlyPriceMinor;} public int getPrinterLimit(){return printerLimit;} public int getDailyJobLimit(){return dailyJobLimit;} public int getMaxFileSizeMb(){return maxFileSizeMb;} public boolean isActive(){return active;}
}
