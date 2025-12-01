/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.helper;

/**
 *
 * @author melleji.mollel
 */
//@Configuration
//@EnableAspectJAutoProxy
public class AuditConfiguration {

//    @Autowired
//    private Environment environment;
//
//    @Bean
//    public Layout layout() {
//        return new SimpleLayout();
//    }
//
//    @Bean
//    public MetaData metaData() {
//        return new CilantroMetaData();
//    }
//
//    @Bean
//    public DatabaseAuditHandler databaseHandler() {
//        DatabaseAuditHandler databaseHandler = new DatabaseAuditHandler();
//        databaseHandler.setEmbedded("false");
//        databaseHandler.setDb_user(environment.getRequiredProperty("spring.datasource.username"));
//        databaseHandler.setDb_password(environment.getRequiredProperty("spring.datasource.password"));
//        databaseHandler.setDb_url(environment.getRequiredProperty("spring.datasource.url"));
//        databaseHandler.setDb_driver(environment.getRequiredProperty("spring.datasource.driverClassName"));
//        return databaseHandler;
//    }
//
//    @Bean
//    public FileAuditHandler fileAuditHandler() {
//        FileAuditHandler fileAuditHandler = new FileAuditHandler();
//        return fileAuditHandler;
//    }
//
//    @Bean
//    public ConsoleAuditHandler consoleAuditHandler() {
//        return new ConsoleAuditHandler();
//    }
//
//    @Bean
//    public SpringAudit4jConfig springAudit4jConfig() {
//        SpringAudit4jConfig audit4jConfig = new SpringAudit4jConfig();
//        Map<String, String> props = new HashMap<>();
//        props.put("log.file.location", ".");
//        List<Handler> handlers = new ArrayList<>();
//        handlers.add(consoleAuditHandler());
//        handlers.add(fileAuditHandler());
//        handlers.add(databaseHandler());
//        audit4jConfig.setHandlers(handlers);
//        audit4jConfig.setLayout(layout());
//        audit4jConfig.setMetaData(metaData());
//        audit4jConfig.setProperties(props);
//        return audit4jConfig;
//
//    }
//
//    @Bean
//    public AuditAspect auditAspect() {
//        return new AuditAspect();
//    }
}
