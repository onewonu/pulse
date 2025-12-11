package com.pulse.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import javax.sql.DataSource;

@Configuration
@Profile("prod")
public class AwsSecretsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsConfig.class);

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }

    @Bean
    public DataSource dataSource(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring DataSource from AWS Secrets Manager and Parameter Store");

        try {
            String host = getParameter(ssmClient, "/pulse/prod/database/host");
            String port = getParameter(ssmClient, "/pulse/prod/database/port");
            String database = getParameter(ssmClient, "/pulse/prod/database/name");

            String username = getSecret(secretsClient, "/pulse/prod/database/username");
            String password = getSecret(secretsClient, "/pulse/prod/database/password");

            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%s/%s?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true",
                host, port, database
            ));
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            dataSource.setMaximumPoolSize(10);
            dataSource.setMinimumIdle(5);
            dataSource.setConnectionTimeout(30000);
            dataSource.setIdleTimeout(600000);
            dataSource.setMaxLifetime(1800000);

            log.info("DataSource configured successfully for database: {}", database);
            return dataSource;

        } catch (Exception e) {
            log.error("Failed to configure DataSource from Secrets Manager", e);
            throw new RuntimeException("Failed to configure DataSource", e);
        }
    }

    @Bean
    public SeoulApiConfig seoulApiConfig(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring Seoul API settings from AWS");

        try {
            String apiKey = getSecret(secretsClient, "/pulse/prod/seoul-api-key");
            String baseUrl = getParameter(ssmClient, "/pulse/prod/seoul-api-base-url");

            SeoulApiConfig config = new SeoulApiConfig();
            config.setApiKey(apiKey);
            config.setBaseUrl(baseUrl);
            // page-size는 application-prod.yml에서 관리

            log.info("Seoul API configuration loaded successfully");
            return config;

        } catch (Exception e) {
            log.error("Failed to configure Seoul API settings", e);
            throw new RuntimeException("Failed to configure Seoul API", e);
        }
    }

    private String getSecret(SecretsManagerClient client, String secretName) {
        log.debug("Fetching secret: {}", secretName);

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        String secretValue = client.getSecretValue(request).secretString();
        log.debug("Successfully fetched secret: {}", secretName);

        return secretValue;
    }

    private String getParameter(SsmClient client, String parameterName) {
        log.debug("Fetching parameter: {}", parameterName);

        GetParameterRequest request = GetParameterRequest.builder()
                .name(parameterName)
                .build();

        String parameterValue = client.getParameter(request).parameter().value();
        log.debug("Successfully fetched parameter: {}", parameterName);

        return parameterValue;
    }


    public static class SeoulApiConfig {
        private String apiKey;
        private String baseUrl;
        private int pageSize;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
