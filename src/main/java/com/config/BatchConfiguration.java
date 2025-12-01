package com.config;

import com.entities.PensionTransaction;
import com.helper.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.database.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    private DataSource dataSource;

    @Autowired
    BatchItemWriteListener batchItemWriteListener;

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfiguration.class);

//    @Bean
//    public JdbcCursorItemReader<PensionTransaction> reader() {
//        JdbcCursorItemReader<PensionTransaction> reader = new JdbcCursorItemReader<>();
//        reader.setDataSource(dataSource);
//        reader.setSql("SELECT * FROM pensioners_payroll where od_loan_status='0' and create_dt>='2022-11-01 00:00:00' " +
//                "and create_dt<='2022-11-30 23:59:59' order by trackingNo asc");
//        reader.setRowMapper(new PensionPayrollMapper());
////        reader.setFetchSize(5000);
//        return reader;
//    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<PensionTransaction> reader(@Value("#{jobParameters['batchRef']}") String batchRef) throws Exception {
        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("SELECT * ");
        provider.setFromClause("FROM pensioners_payroll");
        provider.setWhereClause("where od_loan_status='0' and cbs_status<>'F' and cbs_status is not null and batchReference in (" +batchRef + ")");
        Map<String, Order> sortConfiguration = new HashMap<>();
        sortConfiguration.put("id", Order.ASCENDING);
        provider.setSortKeys(sortConfiguration);
        String query = provider.getObject().generateFirstPageQuery(100);
        LOGGER.info("Query: {}", query);
        JdbcPagingItemReaderBuilder<PensionTransaction> itemReader = new JdbcPagingItemReaderBuilder<>();
        try {
            itemReader.queryProvider(Objects.requireNonNull(provider.getObject()));
        } catch (Exception ex) {
            itemReader.queryProvider((PagingQueryProvider) provider);
            LOGGER.info(null, ex);
        }
        itemReader.name("pensionPayrollTransactionReader");
        itemReader.dataSource(dataSource);
        itemReader.pageSize(5000);
        itemReader.fetchSize(5000);
        itemReader.rowMapper(new PensionPayrollMapper());
        itemReader.saveState(true);
//        Map<String, Object> parameterValues = new HashMap<>();
//        parameterValues.put("batchRef", batchRef);
//        itemReader.parameterValues(parameterValues);

        return itemReader.build();
    }


    public static class PensionPayrollMapper implements RowMapper<PensionTransaction> {
        @Override
        public PensionTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            PensionTransaction PensionTransaction = new PensionTransaction();
            PensionTransaction.setId(rs.getString("id"));
            PensionTransaction.setTrackingNo(rs.getString("trackingNo"));
            PensionTransaction.setName(rs.getString("name"));
            PensionTransaction.setCbsName(rs.getString("cbs_name"));
            PensionTransaction.setPercentageMatch(rs.getInt("percentage_match"));
            PensionTransaction.setCurrency(rs.getString("currency"));
            PensionTransaction.setAmount(rs.getDouble("amount"));
            PensionTransaction.setAccount(rs.getString("account"));
            PensionTransaction.setChannelIdentifier(rs.getString("channel_identifier"));
            PensionTransaction.setBankCode(rs.getString("bankCode"));
            PensionTransaction.setPensionerId(rs.getString("pensioner_id"));
            PensionTransaction.setBatchReference(rs.getString("batchReference")+DateUtil.now("yyyyMMddHHmmss"));
            PensionTransaction.setBankReference(rs.getString("bankReference"));
            PensionTransaction.setDescription(rs.getString("description"));
            PensionTransaction.setCreatedBy(rs.getString("created_by"));
            PensionTransaction.setCreateDt(rs.getDate("create_dt"));
            PensionTransaction.setStatus(rs.getString("status"));
            PensionTransaction.setResponseCode(rs.getString("responseCode"));
            PensionTransaction.setMessage(rs.getString("message"));
            PensionTransaction.setComments(rs.getString("comments"));
            PensionTransaction.setCbsStatus(rs.getString("cbs_status"));
            PensionTransaction.setVerifiedBy(rs.getString("verified_by"));
            PensionTransaction.setVerifiedDt(rs.getDate("verified_dt"));
            PensionTransaction.setApprovedBy(rs.getString("approved_by"));
            PensionTransaction.setApprovedDt(rs.getDate("approved_dt"));
            PensionTransaction.setPayrollMonth(rs.getInt("payroll_month"));
            PensionTransaction.setPayrollYear(rs.getInt("payroll_year"));
            PensionTransaction.setBranchId("-5");
            PensionTransaction.setDrAccount("1-070-00-2064-2064002");
            PensionTransaction.setModule("PENSION PAYROLL");
            PensionTransaction.setReverse("N");
            PensionTransaction.setTries(0);
            PensionTransaction.setTimestamp(DateUtil.now("yyyy-MM-dd HH:mm:ss"));
            return PensionTransaction;
        }
    }

    @Bean
    public TransactionItemProcessor processor() {
        return new TransactionItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<PensionTransaction> writer() {
        JdbcBatchItemWriter<PensionTransaction> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("INSERT INTO tmp_batch_transaction_item (txnRef, amount, batch, buId, drAcct, createDt, currency," +
                " crAcct, module, narration, recId, recSt, reverse, tries, `timestamp`) VALUES (:bankReference, :amount," +
                " :batchReference, :branchId, :drAccount, :createDt, :currency, :account, :module, :description," +
                " :trackingNo, :status, :reverse, :tries, :timestamp)");
        writer.setDataSource(dataSource);
        return writer;
    }

    @Bean
    public Job job(JobRepository jobRepository, JobCompletionNotificationListener listener, Step start) {
        return new JobBuilder("job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(start)
                .build();
    }

    @Bean
    public Step start(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new StepBuilder("start", jobRepository)
                .<PensionTransaction, PensionTransaction>chunk(5000, transactionManager)
                .reader(reader(null))
                .processor(processor())
                .writer(writer())
                .listener(batchItemWriteListener)
                .build();
    }
}

