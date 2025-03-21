package com.marshallArts.trmnl;

public final class LambdaMain {

    public static void main(final String[] args) {
        switch (System.getenv("LAMBDA_HANDLER_KEY")) {
            case "ACTIVITY":
                ActivityLambdaMain.main(args);
                break;
            case "LISTS":
                ListsLambdaMain.main(args);
                break;
            default:
                throw new UnsupportedOperationException(System.getProperty("LAMBDA_HANDLER_KEY"));
        }
    }

}
