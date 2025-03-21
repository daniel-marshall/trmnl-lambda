package com.marshallArts.trmnl;

import javax.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import dagger.Component;

public class ListsLambdaMain {

    public static void main(final String[] args) {
        AWSLambda.main(new String[] { DaggerHandler.class.getName() + "::handleRequest" });
    }

    public interface HandlerDelegate<IN> {
        String handle(final IN event, final APIGatewayV2HTTPEvent rawEvent, final Context context) throws Exception;
    }

    @Component(modules = DaggerModule.class)
    interface Lambda {
        @Named("ListsHandler") RequestHandler<APIGatewayV2HTTPEvent, String> handler();
    }

    public static final class DaggerHandler implements RequestHandler<APIGatewayV2HTTPEvent, String> {
        private static final RequestHandler<APIGatewayV2HTTPEvent, String> DELEGATE = DaggerListsLambdaMain_Lambda.builder()
                .build()
                .handler();

        @Override
        public String handleRequest(APIGatewayV2HTTPEvent input, Context context) {
            return DELEGATE.handleRequest(input, context);
        }
    }

}
