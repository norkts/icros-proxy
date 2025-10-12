package com.norkts.icros.controller;

import com.norkts.icros.util.HttpLoggingInterceptor;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Controller
@RequestMapping("/proxy")
@CrossOrigin("*")
@Slf4j
public class ProxyController {
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor("", HttpLoggingInterceptor.Level.NONE))
            .build();

    @RequestMapping(value = {"/", "", "/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity request(HttpServletRequest oriRequest, HttpServletResponse oriResponse) {

        Request.Builder builder = new Request.Builder();
        String urlStr = oriRequest.getRequestURL().toString();
        urlStr = urlStr.split("/proxy/")[1];
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String queryStr = oriRequest.getQueryString();

        builder.url(url + "?" + queryStr);
        String method = oriRequest.getMethod().toUpperCase();

        URL finalUrl = url;
        oriRequest.getHeaderNames().asIterator().forEachRemaining(name -> {
            if(!"host".equals(name) && !"connection".equals(name)){
                builder.header(name, oriRequest.getHeader(name));
            }
        });

        if(Objects.equals(method, "POST")){
            try {
                byte[]  bytes = oriRequest.getInputStream().readAllBytes();
                if(oriRequest.getContentType() != null){
                    builder.post(RequestBody.create(bytes, MediaType.parse(oriRequest.getContentType())));
                }else{
                    builder.post(RequestBody.create(bytes, MediaType.get("application/octet-stream")));
                }
            } catch (IOException e) {
                log.error("请求内容构建失败:" + urlStr, e);
                throw new RuntimeException(e);
            }
        }else{
            builder.get();
        }


        Request request1 = builder.build();
        long startTime = System.currentTimeMillis();
        try {
            ResponseBody body;
            try (Response response = httpClient.newCall(request1).execute()) {

                body = response.body();

                byte[] bytes = body.bytes();

                ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.status(response.code());

                for (Pair<? extends String, ? extends String> header : response.headers()) {
                    if (header.getFirst().startsWith("Access-Control-Allow")) {
                        continue;
                    }

                    if(header.getFirst().equals("Content-Length") || header.getFirst().equals("Transfer-Encoding")){
                        continue;
                    }

                    bodyBuilder.header(header.getFirst(), header.getSecond());
                }
                bodyBuilder
                        .contentLength(bytes.length)
                        .contentType(org.springframework.http.MediaType.parseMediaType(body.contentType().toString()));

                return bodyBuilder.body(bytes);
            }finally {
                log.info("请求成功:" + urlStr + ", cost=" + (System.currentTimeMillis() - startTime));
            }

        } catch (IOException e) {
            log.error("请求失败:" + urlStr, e);
            throw new RuntimeException(e);
        }
    }
}
