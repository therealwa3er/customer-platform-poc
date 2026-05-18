package com.customerplatform.loyalty.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/batches")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job recalculateLoyaltyTierJob;

    public BatchController(JobLauncher jobLauncher, Job recalculateLoyaltyTierJob) {
        this.jobLauncher = jobLauncher;
        this.recalculateLoyaltyTierJob = recalculateLoyaltyTierJob;
    }

    @PostMapping("/recalculate-tiers")
    @PreAuthorize("hasRole('ADMIN')")
    public String recalculateTiers() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("startedAt", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(recalculateLoyaltyTierJob, params);

        return "Batch recalculation started";
    }
}
