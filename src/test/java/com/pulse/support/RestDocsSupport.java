package com.pulse.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;

@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;
    protected RestDocumentationResultHandler restDocs;

    @BeforeEach
    void setUpRestDocs(RestDocumentationContextProvider provider) {
        this.restDocs = MockMvcRestDocumentation.document(
                "{class-name}/{method-name}",
                Preprocessors.preprocessRequest(Preprocessors.prettyPrint(), uriDecodePreprocessor()),
                Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
        );

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .apply(documentationConfiguration(provider))
                .alwaysDo(restDocs)
                .build();
    }

    private OperationPreprocessor uriDecodePreprocessor() {
        return new OperationPreprocessor() {
            @Override
            public OperationRequest preprocess(OperationRequest request) {
                String decoded = URLDecoder.decode(
                        request.getUri().toASCIIString(), StandardCharsets.UTF_8
                );
                return new OperationRequestFactory().create(
                        URI.create(decoded),
                        request.getMethod(),
                        request.getContent(),
                        request.getHeaders(),
                        request.getParts(),
                        request.getCookies()
                );
            }

            @Override
            public OperationResponse preprocess(OperationResponse response) {
                return response;
            }
        };
    }

    protected abstract Object controller();
}
