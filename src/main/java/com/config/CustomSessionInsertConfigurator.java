/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

/**
 *
 * @author samichael, fix duplicate key when user login [spring session]
 */
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.util.StringUtils;

/**
 * see: <a
 * href="https://github.com/spring-projects/spring-session/issues/1213#issuecomment-516466012">When
 * you fire multiple AJAX calls to a controller and call setAttribute you can
 * get a ERROR: duplicate key value violates unique constraint
 * "spring_session_attributes_pk"</a>
 */
@Configuration
@ConditionalOnProperty(value = "spring.session.use-mysql-specific-insert", havingValue = "true")
public class CustomSessionInsertConfigurator {

    private static final String CREATE_SESSION_ATTRIBUTE_QUERY_ON_DUPLICATE_KEY_UPDATE
            = "INSERT INTO %TABLE_NAME%_ATTRIBUTES(SESSION_PRIMARY_ID, ATTRIBUTE_NAME,  ATTRIBUTE_BYTES) "
            + "SELECT PRIMARY_ID, ?, ? "
            + "FROM %TABLE_NAME% "
            + "WHERE SESSION_ID = ? ON DUPLICATE KEY UPDATE ATTRIBUTE_BYTES=VALUES(ATTRIBUTE_BYTES)";

    private final JdbcIndexedSessionRepository originalRepository;

    @Autowired
    public CustomSessionInsertConfigurator(JdbcIndexedSessionRepository originalRepository) {
        this.originalRepository = originalRepository;
    }

    @PostConstruct
    public void customizedJdbcOperationsSessionRepository() {
        originalRepository.setCreateSessionAttributeQuery(
                StringUtils.replace(
                        CREATE_SESSION_ATTRIBUTE_QUERY_ON_DUPLICATE_KEY_UPDATE,
                        "%TABLE_NAME%",
                        JdbcIndexedSessionRepository.DEFAULT_TABLE_NAME));
    }
}
