/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.queue;

import com.config.SYSENV;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jakarta.jms.DeliveryMode;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.DeadLetterStrategy;
import org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.apache.activemq.usage.SystemUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.util.unit.DataSize;

/**
 *
 * @author samichael
 */
@EnableJms
@Configuration
public class ActiveMQConfig implements JmsListenerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConfig.class);
    @Autowired
    SYSENV systemVariable;
    @Autowired
    @Qualifier("pensionExecutor")
    TaskExecutor executor;

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        registrar.setContainerFactory(queueListenerFactory());
    }

    @Bean
    public JmsListenerContainerFactory<?> queueListenerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(messageConverter());
        // factory.setErrorHandler(new DefaultErrorHandler());
        factory.setConnectionFactory(activeMQConnectionFactory());
        factory.setConcurrency("1-300");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        factory.setMaxMessagesPerTask(5);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(10000L);
        factory.setRecoveryInterval(15000L);
        return factory;
    }
   @Bean
    public JmsListenerContainerFactory<?> queueListenerFactoryIncomingSTP() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(messageConverter());
        // factory.setErrorHandler(new DefaultErrorHandler());
        factory.setConnectionFactory(activeMQConnectionFactory());
        factory.setConcurrency("1-100");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        factory.setMaxMessagesPerTask(5);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(10000L);
        factory.setRecoveryInterval(15000L);
        return factory;
    }


    @Bean
    public JmsListenerContainerFactory<?> queueListenerFactoryPensioners() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(messageConverter());
        // factory.setErrorHandler(new DefaultErrorHandler());
        factory.setConnectionFactory(activeMQConnectionFactory());
        factory.setConcurrency("1-2000");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        factory.setTaskExecutor(executor);
        factory.setMaxMessagesPerTask(100);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(10000L);
        factory.setRecoveryInterval(15000L);
        return factory;
    }

    /*
    EFT LISTENER
     */
    @Bean
    public JmsListenerContainerFactory<?> queueListenerEFTFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setMessageConverter(messageConverter());
        // factory.setErrorHandler(new DefaultErrorHandler());
        factory.setConnectionFactory(activeMQConnectionFactoryEFTServer());
        factory.setConcurrency("1-150");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(ActiveMQSession.INDIVIDUAL_ACKNOWLEDGE);
        factory.setMaxMessagesPerTask(5);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(10000L);
        factory.setRecoveryInterval(15000L);
        return factory;
    }

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        //Sets the global default redelivery policy to be used when a message is delivered but the session is rolled back
        //activeMQConnectionFactory.setRedeliveryPolicy(reDeliveryPolicy());
        //url for producer/consumer
        activeMQConnectionFactory.setBrokerURL(systemVariable.ACTIVE_MQ_API_BASE_URL);
        activeMQConnectionFactory.setRedeliveryPolicyMap(redeliveryPolicyMap());
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setMaxConnections(10);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(pooledConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(10);
        // optimize AMQ to be as fast as possible so unit testing is quicker
        activeMQConnectionFactory.setCopyMessageOnSend(false);
        activeMQConnectionFactory.setOptimizeAcknowledge(true);
        activeMQConnectionFactory.setOptimizedMessageDispatch(true);
        activeMQConnectionFactory.setTrustAllPackages(true);
        activeMQConnectionFactory.setUseAsyncSend(true);
        activeMQConnectionFactory.setAlwaysSessionAsync(true);
        return activeMQConnectionFactory;
    }

    @Bean
    public BrokerService broker() throws Exception {
        final BrokerService broker = new BrokerService();
        broker.addConnector(systemVariable.ACTIVE_MQ_API_BASE_URL);
        broker.addConnector("vm://localhost");
        KahaDBPersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
        File dir = new File(System.getProperty("user.home") + File.separator + "kahaDB");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        persistenceAdapter.setDirectory(dir);
        persistenceAdapter.setJournalMaxFileLength(1024 * 3000);
        persistenceAdapter.setIndexWriteBatchSize(1000);
        persistenceAdapter.setEnableIndexWriteAsync(true);
        broker.setPersistenceAdapter(persistenceAdapter);
        broker.setPersistent(false);
        broker.setUseJmx(true);
        broker.setDestinationPolicy(policyMap());
        broker.setSchedulerSupport(true);
        SystemUsage systemUsage = new SystemUsage();
        systemUsage.getStoreUsage().setLimit(DataSize.parse("30GB").toBytes());
        systemUsage.getTempUsage().setLimit(DataSize.parse("4GB").toBytes());
        systemUsage.getMemoryUsage().setLimit(DataSize.parse("4GB").toBytes());
        systemUsage.setSendFailIfNoSpace(true);
        broker.setSystemUsage(systemUsage);
        broker.setProducerSystemUsage(systemUsage);
        broker.setEnableStatistics(true);
        //broker.getDestinations();
        LOGGER.trace("ActiveMQConfig - Broker Store Usage <" + systemUsage.getStoreUsage().getLimit() + " bytes>");
        LOGGER.trace("ActiveMQConfig - Broker Temp Usage <" + systemUsage.getTempUsage().getLimit() + " bytes>");
        LOGGER.trace("ActiveMQConfig - Broker Memory Usage <" + systemUsage.getMemoryUsage().getLimit() + " bytes>");
        return broker;
    }

    @Bean
    public PolicyMap policyMap() {
        PolicyMap destinationPolicy = new PolicyMap();
        List<PolicyEntry> entries = new ArrayList<>();
        PolicyEntry queueEntry = new PolicyEntry();
        queueEntry.setProducerFlowControl(true);
        queueEntry.setUseCache(true);
        queueEntry.setQueue(">");
        //set memory limit at queue level
        //queueEntry.setMemoryLimit(DataSize.parse("512MB").toBytes());
        queueEntry.setDeadLetterStrategy(deadLetterStrategy());
        PolicyEntry topicEntry = new PolicyEntry();
        topicEntry.setTopic(">");
        topicEntry.setDeadLetterStrategy(deadLetterStrategy());
        entries.add(queueEntry);
        entries.add(topicEntry);
        destinationPolicy.setPolicyEntries(entries);
        return destinationPolicy;
    }

    @Bean
    public DeadLetterStrategy deadLetterStrategy() {
        IndividualDeadLetterStrategy deadLetterStrategy = new IndividualDeadLetterStrategy();
        deadLetterStrategy.setQueueSuffix(".dlq");
        deadLetterStrategy.setUseQueueForQueueMessages(true);
        return deadLetterStrategy;
    }

    @Bean
    public RedeliveryPolicyMap redeliveryPolicyMap() {
        RedeliveryPolicy queuePolicy = new RedeliveryPolicy();
        queuePolicy.setMaximumRedeliveries(10);
        queuePolicy.setRedeliveryDelay(5000);
        queuePolicy.setBackOffMultiplier(2);
        queuePolicy.setInitialRedeliveryDelay(1000);
        queuePolicy.setUseCollisionAvoidance(true);
        queuePolicy.setUseExponentialBackOff(true);

        RedeliveryPolicy queuePolicyTanescoSaccos = new RedeliveryPolicy();
        queuePolicy.setMaximumRedeliveries(10000);
        queuePolicy.setRedeliveryDelay(5000);
//        queuePolicy.setBackOffMultiplier(2);
        queuePolicy.setInitialRedeliveryDelay(1000);
        queuePolicy.setUseCollisionAvoidance(true);
//        queuePolicy.setUseExponentialBackOff(true);

        RedeliveryPolicy queueUrlApiPolicy = new RedeliveryPolicy();
        queueUrlApiPolicy.setMaximumRedeliveries(30);
        queueUrlApiPolicy.setRedeliveryDelay(2000);
        queueUrlApiPolicy.setInitialRedeliveryDelay(10000);
        queueUrlApiPolicy.setUseCollisionAvoidance(true);

        RedeliveryPolicy queuePensionersPolicy = new RedeliveryPolicy();
        queuePensionersPolicy.setMaximumRedeliveries(50);
        queuePensionersPolicy.setRedeliveryDelay(2000);
        queuePensionersPolicy.setInitialRedeliveryDelay(10000);
        queuePensionersPolicy.setUseCollisionAvoidance(true);

        RedeliveryPolicy stpIncomingPolicy = new RedeliveryPolicy();
        stpIncomingPolicy.setMaximumRedeliveries(60);
        stpIncomingPolicy.setRedeliveryDelay(2000);
        stpIncomingPolicy.setInitialRedeliveryDelay(10000);
        stpIncomingPolicy.setUseCollisionAvoidance(true);
        stpIncomingPolicy.setBackOffMultiplier(2);
        stpIncomingPolicy.setUseExponentialBackOff(true);
        //Hii inaongeza ,muda wa kutrigger message, yaani RedeliveryDelay times(X) BackOffMultiplier, hivyo inapelekea kuchelewa kuanza muda zaidi kadri inavyo execute {(RedeliveryDelay times(X) BackOffMultiplier)XBackOffMultiplier}
        //queueUrlApiPolicy.setBackOffMultiplier(2);
        // queueUrlApiPolicy.setUseExponentialBackOff(false);
        RedeliveryPolicy topicPolicy = new RedeliveryPolicy();
        topicPolicy.setInitialRedeliveryDelay(0);
        topicPolicy.setRedeliveryDelay(1000);
        topicPolicy.setUseExponentialBackOff(false);
        topicPolicy.setMaximumRedeliveries(30);
        // Receive a message with the JMS API
        RedeliveryPolicyMap map = new RedeliveryPolicyMap();
        //map.put(new ActiveMQTopic(">"), topicPolicy);
        map.put(new ActiveMQQueue("queue.gepg.>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("queue.rtgs.>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("queue.muse.>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("queue.rtgs.>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("queue.eft.>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("PROCESS>"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("PROCESS_ACKNOWLEDGEMENT_TO_TANESCO_SACCOS>"), queuePolicyTanescoSaccos);
        map.put(new ActiveMQQueue("PENSION>"), queuePensionersPolicy);

        map.put(new ActiveMQQueue("QUEUE_EFT_INCOMING_TO_CBS"), queueUrlApiPolicy);
        map.put(new ActiveMQQueue("INCOMING_SWIFT_STP_INWARD_TRANSACTION>"), stpIncomingPolicy);
        return map;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    //currently we are using spring default bean.
    @Bean(name = "JmsTemplate")
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactory());
        jmsTemplate.setDeliveryPersistent(false);
        jmsTemplate.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setMessageIdEnabled(true);
        jmsTemplate.setDefaultDestinationName("empty");
        jmsTemplate.setReceiveTimeout(20000);
        jmsTemplate.setTimeToLive(3600000);
        jmsTemplate.setPriority(100);
        jmsTemplate.setMessageConverter(messageConverter());
        return jmsTemplate;
    }

    /*
    connect to broker eft 
    OBTAIN CONNECTION TO MAPPED DRIVE BOT
     */
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactoryEFTServer() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
//activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy());
//url for producer/consumer
        activeMQConnectionFactory.setBrokerURL(systemVariable.EFT_BROKER_URL);
        activeMQConnectionFactory.setRedeliveryPolicyMap(redeliveryPolicyMap());
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setMaxConnections(10);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(pooledConnectionFactory);
        cachingConnectionFactory.setSessionCacheSize(10);
// optimize AMQ to be as fast as possible so unit testing is quicker
        activeMQConnectionFactory.setCopyMessageOnSend(false);
        activeMQConnectionFactory.setOptimizeAcknowledge(true);
        activeMQConnectionFactory.setOptimizedMessageDispatch(true);
        activeMQConnectionFactory.setTrustAllPackages(true);
        activeMQConnectionFactory.setUseAsyncSend(true);
        activeMQConnectionFactory.setAlwaysSessionAsync(true);
        return activeMQConnectionFactory;
    }

    /*
    * EFT BEAN TEMPLATE FOR CONNECTION TO EFT SERVER
     */
    @Bean(name = "JmsTemplateEFT")
    public JmsTemplate jmsTemplatePush() {
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactoryEFTServer());
        jmsTemplate.setDeliveryPersistent(false);
        jmsTemplate.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setMessageIdEnabled(true);
        jmsTemplate.setDefaultDestinationName("empty");
        jmsTemplate.setReceiveTimeout(2000);
        jmsTemplate.setTimeToLive(3600000);
        jmsTemplate.setPriority(100);
        jmsTemplate.setMessageConverter(messageConverter());
        return jmsTemplate;
    }
}
