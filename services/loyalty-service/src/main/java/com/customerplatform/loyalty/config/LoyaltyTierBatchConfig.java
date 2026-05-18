package com.customerplatform.loyalty.config;

import com.customerplatform.loyalty.model.LoyaltyAccount;
import com.customerplatform.loyalty.model.LoyaltyTier;
import com.customerplatform.loyalty.repository.LoyaltyAccountRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class LoyaltyTierBatchConfig {

    @Bean
    public Job recalculateLoyaltyTierJob(
            JobRepository jobRepository,
            Step recalculateLoyaltyTierStep
    ) {
        return new JobBuilder("recalculateLoyaltyTierJob", jobRepository)
                .start(recalculateLoyaltyTierStep)
                .build();
    }

    @Bean
    public Step recalculateLoyaltyTierStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LoyaltyAccountRepository loyaltyAccountRepository
    ) {
        RepositoryItemReader<LoyaltyAccount> reader = new RepositoryItemReaderBuilder<LoyaltyAccount>()
                .name("loyaltyAccountReader")
                .repository(loyaltyAccountRepository)
                .methodName("findAll")
                .pageSize(100)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();

        return new StepBuilder("recalculateLoyaltyTierStep", jobRepository)
                .<LoyaltyAccount, LoyaltyAccount>chunk(100, transactionManager)
                .reader(reader)
                .processor(account -> {
                    account.setTier(calculateTier(account.getPoints()));
                    return account;
                })
                .writer(accounts -> loyaltyAccountRepository.saveAll(accounts))
                .build();
    }

    private LoyaltyTier calculateTier(int points) {
        if (points >= 5000) return LoyaltyTier.GOLD;
        if (points >= 1000) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }
}
