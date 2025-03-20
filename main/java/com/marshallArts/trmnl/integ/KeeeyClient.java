package com.marshallArts.trmnl.integ;

import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class KeeeyClient {

    private final ObjectMapper objectMapper;

    private final LambdaClient lambdaClient;

    @Named("LAMBDA_NAME")
    private final String lambdaName;

    public <T> T getKey(String key, TypeReference<T> type) throws JsonProcessingException {
        final ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("key", key);

        final String payload = objectMapper.writeValueAsString(APIGatewayV2HTTPEvent.builder()
                .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
                        .withHttp(APIGatewayV2HTTPEvent.RequestContext.Http.builder()
                                .withMethod("GET")
                                .build()
                        )
                        .build()
                )
                .withBody(objectNode.toString())
                .build()
        );

        final InvokeResponse lambdaResponse = lambdaClient.invoke(InvokeRequest.builder()
                .functionName(lambdaName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build()
        );

        String jsonValue = lambdaResponse.payload()
                .asUtf8String()
                .replace("\\\"", "\"");

        while (jsonValue.startsWith("\"")) {
            jsonValue = jsonValue.substring(1);
        }
        while (jsonValue.endsWith("\"")) {
            jsonValue = jsonValue.substring(0, jsonValue.length() - 1);
        }

        return objectMapper.readValue(jsonValue, type);
    }

}
