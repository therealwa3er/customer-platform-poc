package com.customerplatform.loyalty.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job recalculateLoyaltyTierJob;

    public LoyaltyBatchScheduler(
            JobLauncher jobLauncher,
            Job recalculateLoyaltyTierJob
    ) {
        this.jobLauncher = jobLauncher;
        this.recalculateLoyaltyTierJob = recalculateLoyaltyTierJob;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runNightlyTierRecalculation() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("startedAt", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(recalculateLoyaltyTierJob, params);
    }
}
