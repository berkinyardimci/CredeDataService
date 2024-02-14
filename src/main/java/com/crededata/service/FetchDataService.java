package com.crededata.service;

import com.crededata.config.RestTemplateConfig;
import com.crededata.constant.FileConstant;
import com.crededata.constant.URLConstant;
import com.crededata.dto.request.RequestDto;
import com.crededata.dto.response.FetchDataResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FetchDataService {

    private final RestTemplateConfig restTemplate;

    public FetchDataService(RestTemplateConfig restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void fetchData() {
        trustAllCertificates();
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "text/plain");
        headers.add("content-type", "application/json-patch+json");
        headers.add("x-request-origin", "IGT-UI");
        headers.add("x-requested-with", "XMLHttpRequest");

        int currentPage = 1;
        int skipCount = 0;
        int maxResultCount = 12;
        int maxPage = 10;

        while (currentPage <= maxPage) {
            RequestDto requestDto = RequestDto.builder()
                    .keys(RequestDto.Keys.builder().txv(List.of(9)).currentPage(List.of(currentPage)).build())
                    .skipCount(skipCount)
                    .maxResultCount(maxResultCount)
                    .build();

            HttpEntity<?> requestEntity = new HttpEntity<>(requestDto, headers);

            ResponseEntity<String> response = restTemplate.restTemplateBean().postForEntity(URLConstant.GET_ALL_URL, requestEntity, String.class);
            String responseBody = response.getBody();
            JSONObject json = new JSONObject(responseBody);
            JSONArray jsonArray = json.getJSONObject("result").getJSONArray("ads");

            for (int i = 0; i < maxResultCount; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.getString("id");
                getTenderDetail(id);
            }
            currentPage++;
            skipCount += maxResultCount;
        }
    }

    public String getTenderDetail(String id) {
        trustAllCertificates();
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-request-origin", "IGT-UI");
        headers.add("x-requested-with", "XMLHttpRequest");

        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(URLConstant.GET_DETAIL_URL).queryParam("id", id).build();

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.restTemplateBean().exchange(uriComponents.toUriString(), HttpMethod.GET, requestEntity, String.class);
        String responseBody = response.getBody();

        JSONObject json = new JSONObject(responseBody);
        JSONObject result = json.getJSONObject("result");

        String url = result.optString("urlStr");
        String city = result.optString("addressCityName");

        JSONArray jsonArray = new JSONArray(result.optJSONArray("adTypeFilters"));

        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String key = jsonObject.getString("key");
            String value = jsonObject.getString("value");
            map.put(key, value);
        }
        String tenderType = map.get("İlan Türü");
        String tenderRegistrationNo = map.get("İhale Kayıt No");

        FetchDataResponse fetchDataResponse = FetchDataResponse.builder()
                .placeToDo(city)
                .tenderRegistrationNo(tenderRegistrationNo)
                .tenderType(tenderType)
                .url(url)
                .natureTypeQuantity(getNatureTypeQuantity(result))
                .build();
        writeFile(fetchDataResponse, FileConstant.FILE_PATH);
        return null;
    }


    private String getNatureTypeQuantity(JSONObject result) {
        String col3 = "";
        String string = result.optString("content");

        Document document = Jsoup.parse(string);
        Elements tables = document.select("table");

        for (Element table : tables) {
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() == 3) {
                    String col1 = cols.get(0).text();
                    String col2 = cols.get(1).text();
                    if (col1.contains("Niteliği, türü ve miktarı") && col2.equals(":")) {
                        col3 = cols.get(2).text();
                    }
                }
            }
        }
        return col3;
    }


    public void writeFile(FetchDataResponse fetchDataResponse, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("url: " + fetchDataResponse.getUrl());
            writer.newLine();
            writer.write("ihaleKayitNo: " + fetchDataResponse.getTenderRegistrationNo());
            writer.newLine();
            writer.write("niteligiTuruVeMiktari: " + fetchDataResponse.getNatureTypeQuantity());
            writer.newLine();
            writer.write("yapilacakYer: " + fetchDataResponse.getPlaceToDo());
            writer.newLine();
            writer.write("ihaleTuru: " + fetchDataResponse.getTenderType());
            writer.newLine();

            writer.write("--------------------------------");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllTender() {
        List<String> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FileConstant.FILE_PATH))) {
            String line;
            StringBuilder currentData = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.equals("--------------------------------")) {
                    if (!currentData.isEmpty()) {
                        data.add(currentData.toString());
                        currentData.setLength(0);
                    }
                } else {
                    currentData.append(line).append("\n");
                }
            }
            if (!currentData.isEmpty()) {
                data.add(currentData.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getByUrl(String url) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FileConstant.FILE_PATH))) {
            StringBuilder ilanBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(url)) {
                    ilanBuilder.append(line).append("\n");
                    while ((line = reader.readLine()) != null && !line.contains("--------------------------------")) {
                        ilanBuilder.append(line).append("\n");
                    }
                    return ilanBuilder.toString().trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
