package com.marshallArts.trmnl.integ;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownServiceException;

import lombok.AllArgsConstructor;

import static java.nio.charset.StandardCharsets.UTF_8;

@AllArgsConstructor
public class TrmnlWebhookClient {
    private String id;

    public void invoke(final String payload) throws IOException, URISyntaxException {
        final URL endpoint = new URI("https://usetrmnl.com/api/custom_plugins/" + id).toURL();
        final HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try(final OutputStream os = con.getOutputStream()) {
            byte[] input = ("{\"merge_variables\": " + payload + "}").getBytes(UTF_8);
            os.write(input, 0, input.length);
        }

        if (con.getResponseCode() != 200) {
            throw new UnknownServiceException(Integer.toString(con.getResponseCode()));
        }
    }
}
