package com.ultraschemer.microweb.proxy;

import com.ultraschemer.microweb.domain.CentralUserRepositoryManagement;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.vertx.core.json.Json;
import okhttp3.HttpUrl;

import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Objects;

public class CentralAuthorizedRegisteredReverseProxy extends RegisteredReverseProxy {
    public CentralAuthorizedRegisteredReverseProxy() {
        super();
    }

    public CentralAuthorizedRegisteredReverseProxy(int port) {
        super(port);
    }

    @Override
    protected void evaluateRequestPermission(DefaultHttpRequest request) throws Exception {
        // This support only four Authorization options: Authorization Cookie, Microweb Request Header, Authorization
        // Bearer and Query String. The other authorization methods (Form input, Json Entity Body) are considered invasive and
        // so, if they're used, the server accessed by this reverse proxy should deal with them.
        String token = request.headers().get("Authorization");

        if(token == null) {
            try {
                List<HttpCookie> cookies = HttpCookie.parse(request.headers().get("Cookie"));
                for (HttpCookie cookie : cookies) {
                    if (cookie.getName().trim().equals("Microweb-Access-Token")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            } catch (Exception e) { /* Ignore it - token continues to be null */ }

            if(token == null) {
                token = request.headers().get("Microweb-Access-Token");
            }

            if(token == null) {
                final HttpUrl url = HttpUrl.parse(request.getUri());
                try {
                    token = Objects.requireNonNull(url).queryParameter("Microweb-Access-Token");
                } catch(Exception e) {
                    token = null;
                }
            }

            if(token != null) {
                token = "Bearer " + token;
            }
        }

        request.headers().add("Microweb-User", Json.encode(CentralUserRepositoryManagement.evaluateResourcePermission(request.getMethod().toString(),
                new URI(request.getUri()).getPath(), token)));
    }
}
