package com.pulse.config;

import com.pulse.exception.config.AwsConfigurationException;
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
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

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

        } catch (SsmException e) {
            String errorMessage = "Database configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "Database configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Database configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public SeoulApiProperties seoulApiProperties(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring Seoul API settings from AWS");

        try {
            String apiKey = getSecret(secretsClient, "/pulse/prod/seoul-api-key");
            String baseUrl = getParameter(ssmClient, "/pulse/prod/seoul-api-base-url");

            SeoulApiProperties properties = new SeoulApiProperties();
            properties.setKey(apiKey);
            properties.setBaseUrl(baseUrl);

            log.info("Seoul API configuration loaded successfully");
            return properties;

        } catch (SsmException e) {
            String errorMessage = "Seoul API configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "Seoul API configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Seoul API configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public OdsayApiProperties odsayApiProperties(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring ODsay API settings from AWS");

        try {
            String apiKey = getSecret(secretsClient, "/pulse/prod/odsay-api-key");
            String baseUrl = getParameter(ssmClient, "/pulse/prod/odsay-api-base-url");

            OdsayApiProperties properties = new OdsayApiProperties();
            properties.setKey(apiKey);
            properties.setBaseUrl(baseUrl);

            log.info("ODsay API configuration loaded successfully");
            return properties;

        } catch (SsmException e) {
            String errorMessage = "ODsay API configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "ODsay API configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "ODsay API configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public SeoulMetroApiProperties seoulMetroApiProperties(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring Seoul Metro API settings from AWS");

        try {
            String apiKey = getSecret(secretsClient, "/pulse/prod/seoul-metro-api-key");
            String baseUrl = getParameter(ssmClient, "/pulse/prod/seoul-metro-api-base-url");

            SeoulMetroApiProperties properties = new SeoulMetroApiProperties();
            properties.setKey(apiKey);
            properties.setBaseUrl(baseUrl);

            log.info("Seoul Metro API configuration loaded successfully");
            return properties;

        } catch (SsmException e) {
            String errorMessage = "Seoul Metro API configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "Seoul Metro API configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Seoul Metro API configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public KakaoApiProperties kakaoApiProperties(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring Kakao API settings from AWS");

        try {
            String clientId = getSecret(secretsClient, "/pulse/prod/kakao-api-client-id");
            String clientSecret = getSecret(secretsClient, "/pulse/prod/kakao-api-client-secret");
            String redirectUri = getParameter(ssmClient, "/pulse/prod/kakao-api-redirect-uri");

            KakaoApiProperties properties = new KakaoApiProperties();
            properties.setClientId(clientId);
            properties.setClientSecret(clientSecret);
            properties.setRedirectUri(redirectUri);

            log.info("Kakao API configuration loaded successfully");
            return properties;

        } catch (SsmException e) {
            String errorMessage = "Kakao API configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "Kakao API configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Kakao API configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public GoogleApiProperties googleApiProperties(SecretsManagerClient secretsClient, SsmClient ssmClient) {
        log.info("Configuring Google API settings from AWS");

        try {
            String clientId = getSecret(secretsClient, "/pulse/prod/google-api-client-id");
            String clientSecret = getSecret(secretsClient, "/pulse/prod/google-api-client-secret");
            String redirectUri = getParameter(ssmClient, "/pulse/prod/google-api-redirect-uri");

            GoogleApiProperties properties = new GoogleApiProperties();
            properties.setClientId(clientId);
            properties.setClientSecret(clientSecret);
            properties.setRedirectUri(redirectUri);

            log.info("Google API configuration loaded successfully");
            return properties;

        } catch (SsmException e) {
            String errorMessage = "Google API configuration failed: unable to access SSM parameters - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (SecretsManagerException e) {
            String errorMessage = "Google API configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Google API configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        }
    }

    @Bean
    public JwtProperties jwtProperties(SecretsManagerClient secretsClient) {
        log.info("Configuring JWT settings from AWS");

        try {
            String secret = getSecret(secretsClient, "/pulse/prod/jwt/secret");

            JwtProperties properties = new JwtProperties();
            properties.setSecret(secret);
            properties.setAccessTokenExpirationSeconds(3600L);
            properties.setRefreshTokenExpirationSeconds(2592000L);

            log.info("JWT configuration loaded successfully");
            return properties;

        } catch (SecretsManagerException e) {
            String errorMessage = "JWT configuration failed: unable to access secrets - " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "JWT configuration failed: " + e.getMessage();
            throw new AwsConfigurationException(errorMessage, e);
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
}
