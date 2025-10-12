package com.norkts.icros.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpLoggingInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private java.util.logging.Level colorLevel;
    private final Logger logger;
    private volatile Level printLevel = Level.NONE;

    public enum Level {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }

    public HttpLoggingInterceptor(String tag) {
        this.logger = LoggerFactory.getLogger(tag);
    }

    public HttpLoggingInterceptor(String tag, Level level) {
        this.logger = LoggerFactory.getLogger(tag);
        if(level != null){
            setPrintLevel(level);
        }
    }

    public void setPrintLevel(Level level) {
        if (this.printLevel == null) {
            throw new NullPointerException("printLevel == null. Use Level.NONE instead.");
        }
        this.printLevel = level;
    }


    private void log(String message) {
        logger.info(message);
    }

    @Override // okhttp3.Interceptor
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (this.printLevel == Level.NONE) {
            return chain.proceed(request);
        }
        logForRequest(request, chain.connection());
        long startNs = System.nanoTime();
        try {
            Response response = chain.proceed(request);
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            return logForResponse(response, tookMs);
        } catch (Exception e) {
            log("<-- HTTP FAILED: " + e);
            throw e;
        }
    }

    private void logForRequest(Request request, Connection connection) throws IOException {
        StringBuilder sb;
        boolean logBody = this.printLevel == Level.BODY;
        boolean logHeaders = this.printLevel == Level.BODY || this.printLevel == Level.HEADERS;
        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        try {
            try {
                String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
                log(requestStartMessage);
                if (logHeaders) {
                    if (hasRequestBody) {
                        if (requestBody.contentType() != null) {
                            log("\tContent-Type: " + requestBody.contentType());
                        }
                        if (requestBody.contentLength() != -1) {
                            log("\tContent-Length: " + requestBody.contentLength());
                        }
                    }
                    Headers headers = request.headers();
                    int count = headers.size();
                    for (int i = 0; i < count; i++) {
                        String name = headers.name(i);
                        if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                            log("\t" + name + ": " + headers.value(i));
                        }
                    }

                    if (logBody && hasRequestBody) {
                        if (isPlaintext(requestBody.contentType())) {
                            bodyToString(request);
                        } else {
                            log("\tbody: maybe [binary body], omitted!");
                        }
                    }
                }
                sb = new StringBuilder();
            } catch (Exception e) {
                log.error("", e);
                sb = new StringBuilder();
            }
            log(sb.append("--> END ").append(request.method()).toString());
        } catch (Throwable th) {
            log("--> END " + request.method());
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [184=5] */
    private Response logForResponse(Response response, long tookMs) {
        Response.Builder builder = response.newBuilder();
        Response clone = builder.build();
        ResponseBody responseBody = clone.body();
        boolean logBody = this.printLevel == Level.BODY;
        boolean logHeaders = this.printLevel == Level.BODY || this.printLevel == Level.HEADERS;
        try {
            try {
                log("<-- " + clone.code() + ' ' + clone.message() + ' ' + clone.request().url() + " (" + tookMs + "ms\uff09");
                if (logHeaders) {
                    Headers headers = clone.headers();
                    int count = headers.size();
                    for (int i = 0; i < count; i++) {
                        log("\t" + headers.name(i) + ": " + headers.value(i));
                    }

                    if (logBody && HttpHeaders.hasBody(clone)) {
                        if (responseBody == null) {
                            return response;
                        }
                        if (isPlaintext(responseBody.contentType())) {
                            byte[] bytes = readByteArray(responseBody.byteStream());
                            MediaType contentType = responseBody.contentType();
                            String body = new String(bytes, getCharset(contentType));
                            log("\tbody:" + body);
                            return response.newBuilder().body(ResponseBody.create(responseBody.contentType(), bytes)).build();
                        }
                        log("\tbody: maybe [binary body], omitted!");
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
            return response;
        } finally {
            log("<-- END HTTP");
        }
    }

    private static Charset getCharset(MediaType contentType) {
        Charset charset = UTF8;
        if (contentType != null) {
            charset = contentType.charset(charset);
        }
        return charset == null ? UTF8 : charset;
    }

    private static boolean isPlaintext(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        if (mediaType.type() != null && mediaType.type().equals("text")) {
            return true;
        }
        String subtype = mediaType.subtype();
        if (subtype != null) {
            String subtype2 = subtype.toLowerCase();
            if (subtype2.contains("x-www-form-urlencoded") || subtype2.contains("json") || subtype2.contains("xml") || subtype2.contains("html")) {
                return true;
            }
        }
        return false;
    }

    private void bodyToString(Request request) {
        try {
            Request copy = request.newBuilder().build();
            RequestBody body = copy.body();
            if (body == null) {
                return;
            }
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            Charset charset = getCharset(body.contentType());
            log("\tbody:" + buffer.readString(charset));
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public final static int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final int EOF = -1;

    public static byte[] readByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static long copy(InputStream input, OutputStream output) throws IOException {


        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
