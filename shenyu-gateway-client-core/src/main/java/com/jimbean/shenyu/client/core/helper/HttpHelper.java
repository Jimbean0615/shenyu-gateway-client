package com.jimbean.shenyu.client.core.helper;

import com.google.gson.Gson;
import com.jimbean.shenyu.client.core.util.SignUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The type Http helper.
 *
 * @author shenyu
 * @author zhangjb
 */
public class HttpHelper {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHelper.class);

    /**
     * The constant INSTANCE.
     */
    public static final HttpHelper INSTANCE = new HttpHelper();

    private OkHttpClient client = new OkHttpClient.Builder().build();

    /**
     * The constant GATEWAY_ENDPOINT.
     */
    private String GATEWAY_ENDPOINT = "http://localhost:9195";

    /**
     * The constant JSON.
     */
    private static final MediaType JSON = MediaType.parse("application/json");

    private static final Gson GSON = new Gson();

    /**
     * The constant sign enabled.
     */
    private boolean enabled;
    /**
     * The constant appKey.
     */
    private String appKey;

    /**
     * The constant appSecret.
     */
    private String appSecret;

    /**
     * set the gateway endpoint
     *
     * @param gatewayEndPoint
     */
    public void retrofitProperties(String gatewayEndPoint, Boolean enabled, String appKey, String appSecret) {
        this.GATEWAY_ENDPOINT = gatewayEndPoint;
        this.enabled = enabled;
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    /**
     * reset the OkHttpClient
     */
    public void resetClient(OkHttpClient client) {
        this.client = client;
    }

    /**
     * Send a post http request to shenyu gateway.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param req      request body as an object
     * @param respType response type passed to {@link Gson#fromJson(String, Type)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S postGateway(final String path, final Q req, final Type respType) throws Exception {
        return postGateway(path, null, req, respType);
    }

    /**
     * Send a post http request to shenyu gateway.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param headers  http header
     * @param req      request body as an object
     * @param respType response type passed to {@link Gson#fromJson(String, Type)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S postGateway(final String path, final Map<String, Object> headers, final Q req, final Type respType) throws Exception {
        String respBody = post(path, headers, req);
        LOG.info("postGateway({}) resp({})", path, respBody);
        if (respType == null) {
            return null;
        }
        try {
            return GSON.fromJson(respBody, respType);
        } catch (Exception e) {
            return (S) respBody;
        }
    }

    /**
     * Send a post http request to shenyu gateway.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param respType response type passed to {@link Gson#fromJson(String, Type)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S postGateway(final String path, final Type respType) throws Exception {
        return postGateway(path, "", respType);
    }

    /**
     * Send a post http request to shenyu gateway.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param req      request body as an object
     * @param respType response type passed to {@link Gson#fromJson(String, Class)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S postGateway(final String path, final Q req, final Class<S> respType) throws Exception {
        return postGateway(path, null, req, respType);
    }

    /**
     * Send a post http request to shenyu gateway with header.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param headers  http header
     * @param req      request body as an object
     * @param respType response type passed to {@link Gson#fromJson(String, Class)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S postGateway(final String path, final Map<String, Object> headers, final Q req, final Class<S> respType) throws Exception {
        String respBody = post(path, headers, req);
        LOG.info("postGateway({}) resp({})", path, respBody);
        try {
            return GSON.fromJson(respBody, respType);
        } catch (Exception e) {
            return (S) respBody;
        }
    }

    /**
     * Send a post http request to shenyu gateway with custom requestBody.
     *
     * @param <S>         type of response object
     * @param path        path
     * @param requestBody request Body
     * @param respType    response type passed to {@link Gson#fromJson(String, Class)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S> S postGateway(final String path, final RequestBody requestBody, final Class<S> respType) throws Exception {
        Request.Builder requestBuilder = new Request.Builder().post(requestBody).url(GATEWAY_ENDPOINT + path);
        Response response = client.newCall(requestBuilder.build()).execute();
        String respBody = Objects.requireNonNull(response.body()).string();
        try {
            return GSON.fromJson(respBody, respType);
        } catch (Exception e) {
            return (S) respBody;
        }
    }

    private <Q> String post(final String path, final Map<String, Object> headers, final Q req) throws Exception {
        sign(path, headers);
        Request.Builder requestBuilder = new Request.Builder().post(RequestBody.create(GSON.toJson(req), JSON)).url(GATEWAY_ENDPOINT + path);
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach((key, value) -> requestBuilder.addHeader(key, String.valueOf(value)));
        }
        Response response = client.newCall(requestBuilder.build()).execute();
        return Objects.requireNonNull(response.body()).string();
    }

    /**
     * Send a put http request to shenyu gateway.
     *
     * @param <S>      type of response object
     * @param <Q>      type of request object
     * @param path     path
     * @param req      request body as an object
     * @param respType response type passed to {@link Gson#fromJson(String, Class)}
     * @return response s
     * @throws IOException IO exception
     */
    public <S, Q> S putGateway(final String path, final Q req, final Class<S> respType) throws Exception {
        Request request = new Request.Builder().put(RequestBody.create(GSON.toJson(req), JSON)).url(GATEWAY_ENDPOINT + path).build();
        Response response = client.newCall(request).execute();
        String respBody = Objects.requireNonNull(response.body()).string();
        LOG.info("postGateway({}) resp({})", path, respBody);
        try {
            return GSON.fromJson(respBody, respType);
        } catch (Exception e) {
            return (S) respBody;
        }
    }

    /**
     * Send a get http request to shenyu gateway without headers.
     *
     * @param <S>  response type
     * @param path path
     * @param type type of response passed to {@link Gson#fromJson(String, Type)}
     * @return response from gateway
     * @throws IOException IO exception
     */
    public <S> S getFromGateway(final String path, final Type type) throws Exception {
        return this.getFromGateway(path, null, type);
    }

    /**
     * Send a get http request to shenyu gateway with headers.
     *
     * @param <S>     response type
     * @param path    path
     * @param headers headers
     * @param type    type of response passed to {@link Gson#fromJson(String, Type)}
     * @return response from gateway
     * @throws IOException IO exception
     */
    public <S> S getFromGateway(final String path, final Map<String, Object> headers, final Type type) throws Exception {
        Response response = getHttpService(path, headers);
        String respBody = Objects.requireNonNull(response.body()).string();
        LOG.info("getFromGateway({}) resp({})", path, respBody);
        if (type == null) {
            return null;
        }
        try {
            return GSON.fromJson(respBody, type);
        } catch (Exception e) {
            return (S) respBody;
        }
    }

    /**
     * Send a get http request to http service with headers.
     *
     * @param url     url
     * @param headers headers
     * @return response
     * @throws IOException IO exception
     */
    public Response getHttpService(final String url, final Map<String, Object> headers) throws Exception {
        sign(url, headers);
        Request.Builder requestBuilder = new Request.Builder().url(GATEWAY_ENDPOINT + url);
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach((key, value) -> requestBuilder.addHeader(key, String.valueOf(value)));
        }
        Request request = requestBuilder.build();
        return client.newCall(request).execute();
    }

    /**
     * sign
     *
     * @param headers
     */
    private void sign(String path, Map<String, Object> headers) throws Exception {
        if (enabled) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            String timestamp = System.currentTimeMillis() + "";
            String sign = SignUtil.generateSign(appSecret, path, timestamp);

            headers.put("timestamp", timestamp);
            headers.put("appKey", appKey);
            headers.put("sign", sign);
            headers.put("version", "1.0.0");
        }
    }
}
