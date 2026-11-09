package com.printdoc.api.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {
    @Value("${local.server.port}") int port;
    final HttpClient client=HttpClient.newHttpClient();
    @Test void publicPlanCatalogIsAccessible() throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/public/plans")).GET().build();
        HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"success\":true","\"basic\"");
    }
    @Test void ownerRoutesRequireBearerToken() throws Exception {
        HttpRequest req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/owner/jobs")).GET().build();
        HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(401,403);
    }
    @Test void registrationCreatesAccountAndReturnsToken() throws Exception {
        String body="{\"shopName\":\"Test Print Shop\",\"ownerName\":\"Test Owner\",\"email\":\"owner-"+System.nanoTime()+"@example.invalid\",\"password\":\"local-test-password-1\"}";
        HttpRequest req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/auth/shop/register")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"accessToken\"","\"OWNER\"","\"shopCode\"");
    }
    @Test void duplicateEmailIsRejected() throws Exception {
        String email="duplicate-"+System.nanoTime()+"@example.invalid";
        String body="{\"shopName\":\"Test Print Shop\",\"ownerName\":\"Test Owner\",\"email\":\""+email+"\",\"password\":\"local-test-password-1\"}";
        for(int i=0;i<2;i++){
            HttpRequest req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/auth/shop/register")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response=client.send(req,HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(i==0?201:409);
        }
    }
    @Test void corsPreflightOnlyAllowsConfiguredOrigin() throws Exception {
        HttpRequest allowed=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/public/plans")).header("Origin","http://localhost:5173").header("Access-Control-Request-Method","GET").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> good=client.send(allowed,HttpResponse.BodyHandlers.ofString());
        assertThat(good.statusCode()).isEqualTo(200);
        assertThat(good.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        HttpRequest denied=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/public/plans")).header("Origin","https://unknown.invalid").header("Access-Control-Request-Method","GET").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> bad=client.send(denied,HttpResponse.BodyHandlers.ofString());
        assertThat(bad.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test void deviceCanRequestApprovalAndOwnerCanApproveThenAgentCanHeartbeat() throws Exception {
        String email="device-owner-"+System.nanoTime()+"@example.invalid";
        String registration="{\"shopName\":\"Device Test Shop\",\"ownerName\":\"Device Owner\",\"email\":\""+email+"\",\"password\":\"local-test-password-1\",\"phone\":\"+919999999999\",\"printerBrand\":\"Test Printer\"}";
        HttpResponse<String> signedUp=send("POST","/api/v1/auth/shop/register",registration,null,null);
        assertThat(signedUp.statusCode()).isEqualTo(201);
        String token=extract(signedUp.body(),"accessToken");
        String shopCode=extract(signedUp.body(),"shopCode");

        HttpResponse<String> credentials=send("POST","/api/v1/owner/devices/credentials","",token,null);
        assertThat(credentials.statusCode()).isEqualTo(200);
        String secret=extract(credentials.body(),"secretApiKey");
        assertThat(secret).as("Credential response: %s",credentials.body()).hasSizeGreaterThan(20);
        String fingerprint="test-fingerprint-"+System.nanoTime();

        HttpResponse<String> pending=send("GET","/api/v1/devices/check-status?shopCode="+shopCode+"&fingerprint="+fingerprint,null,null,null);
        assertThat(pending.statusCode()).isEqualTo(200).as(pending.body());
        assertThat(pending.body()).contains("\"PENDING\"","\"approved\":false");
        String deviceId=extract(pending.body(),"deviceId");

        HttpResponse<String> approved=send("POST","/api/v1/owner/devices/"+deviceId+"/approve","",token,null);
        assertThat(approved.statusCode()).isEqualTo(200).as(approved.body());
        String agentBody="{\"shopCode\":\""+shopCode+"\",\"deviceFingerprint\":\""+fingerprint+"\",\"agentVersion\":\"test\",\"hostname\":\"test-host\",\"platform\":\"test\"}";
        HttpResponse<String> registered=send("POST","/api/v1/agent/register",agentBody,null,secret);
        assertThat(registered.statusCode()).as("Agent registration response: %s",registered.body()).isEqualTo(200);
        HttpResponse<String> heartbeat=send("POST","/api/v1/agent/heartbeat","{\"shopCode\":\""+shopCode+"\",\"deviceFingerprint\":\""+fingerprint+"\"}",null,secret);
        assertThat(heartbeat.statusCode()).isEqualTo(200).as(heartbeat.body());
        assertThat(heartbeat.body()).contains("\"online\":true");
    }

    private HttpResponse<String> send(String method,String path,String body,String bearer,String secret) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Content-Type","application/json");
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);
        if(secret!=null)builder.header("X-Secret-Api-Key",secret);
        HttpRequest request=switch(method){
            case "GET" -> builder.GET().build();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body==null?"":body)).build();
            default -> throw new IllegalArgumentException(method);
        };
        return client.send(request,HttpResponse.BodyHandlers.ofString());
    }

    private String extract(String body,String field){
        Matcher matcher=Pattern.compile("\\\""+field+"\\\":\\\"([^\\\"]+)\\\"").matcher(body);
        assertThat(matcher.find()).as("Missing %s in %s",field,body).isTrue();
        return matcher.group(1);
    }
}
