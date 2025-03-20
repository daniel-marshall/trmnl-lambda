package com.marshallArts.trmnl;

import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marshallArts.trmnl.LambdaActual.GetActivityHandler;
import com.marshallArts.trmnl.LambdaActual.GetListsHandler;
import com.marshallArts.trmnl.integ.KeeeyClient;
import dagger.Provides;
import software.amazon.awssdk.services.lambda.LambdaClient;

import static com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER;

@dagger.Module
public class DaggerModule {

    @Provides
    @Named("ActivityHandler")
    final RequestHandler<APIGatewayV2HTTPEvent, String> activityLambda(
            final ObjectMapper objectMapper,
            final KeeeyClient keeeyClient) {
        return new LambdaActual(
                objectMapper,
                new GetActivityHandler(
                        keeeyClient,
                        objectMapper
                )
        );
    }

    @Provides
    @Named("ListsHandler")
    final RequestHandler<APIGatewayV2HTTPEvent, String> calendarLambda(
            final ObjectMapper objectMapper,
            final KeeeyClient keeeyClient) {
        return new LambdaActual(
                objectMapper,
                new GetListsHandler(
                        keeeyClient,
                        objectMapper
                )
        );
    }

    @Provides
    final KeeeyClient getKeeeyClient(
            final ObjectMapper objectMapper,
            final LambdaClient lambdaClient,
            @Named("LAMBDA_NAME") final String lambdaName) {
        return new KeeeyClient(
                objectMapper,
                lambdaClient,
                lambdaName
        );
    }

    @Provides
    final ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .enable(ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();
    }

    @Provides
    final LambdaClient lambdaClient() {
        return LambdaClient.builder().build();
    }

    @Provides
    @Named("LAMBDA_NAME")
    final String lambdaName() {
        return System.getenv("LAMBDA_NAME");
    }
}
