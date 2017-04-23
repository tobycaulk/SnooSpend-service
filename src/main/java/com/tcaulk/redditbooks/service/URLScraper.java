package com.tcaulk.redditbooks.service;

import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.sun.tools.corba.se.idl.constExpr.Negative;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class URLScraper {
    private static final Logger log = Logger.getLogger(URLScraper.class);

    private static final Map<String, String> URL_REPLACERS = new HashMap<>();
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    static {
        URL_REPLACERS.put("\\)", " ");
        URL_REPLACERS.put("\"", " ");
        URL_REPLACERS.put("\\\\n", "");
        URL_REPLACERS.put("\\\\r", "");
        URL_REPLACERS.put("\'", " ");
        URL_REPLACERS.put("\\(", " ");
        URL_REPLACERS.put("\\[", " ");
        URL_REPLACERS.put("]", " ");
        URL_REPLACERS.put("\\?", "");
        URL_REPLACERS.put("\\*", "");
    }

    private String file;

    public URLScraper(String file) {
        this.file = file;
    }

    public List<Url> scrape(String host) throws IOException {
        List<Url> detectedUrls = new ArrayList<>();

        File dataFile = openFile();
        LineIterator it = FileUtils.lineIterator(dataFile);
        while(it.hasNext()) {
            String line = removeInvalidCharacters(it.nextLine());
            try {
                detectedUrls = processDetectedUrls(new UrlDetector(line, UrlDetectorOptions.Default).detect(), host);
            } catch(NegativeArraySizeException e) {
                //do nothing, this is a standard error and shouldn't hault scraping
            }
        }

        return detectedUrls;
    }

    public List<Url> scrape() throws IOException {
        return scrape(null);
    }

    public Map<Url, Long> scrapeUrlsPopularity(String host) throws IOException {
        Map<Url, Long> urls = new HashMap<>();

        Map<UrlWrapper, Long> wrappedUrls = new HashMap<>();
        List<Url> detectedUrls = scrape(host);
        if(detectedUrls != null) {
            detectedUrls.forEach(url -> {
                UrlWrapper urlWrapper = new UrlWrapper(url);

                if(!wrappedUrls.containsKey(urlWrapper)) {
                    wrappedUrls.put(urlWrapper, 0L);
                }

                wrappedUrls.put(urlWrapper, urls.get(urlWrapper) + 1);
            });
        }

        wrappedUrls.forEach((wrappedUrl, popularity) -> {
            urls.put(wrappedUrl.url, popularity);
        });

        return urls;
    }

    public Map<Url, Long> scrapeUrlsPopularity() throws IOException {
        return scrapeUrlsPopularity(null);
    }

    private List<Url> processDetectedUrls(List<Url> urls, String host) {
        return urls.stream().filter(url ->
                                host != null ? url.getFullUrl().contains(host) : true &&
                                (StringUtils.countMatches(url.getFullUrl(), HTTP) <= 1 ||
                                StringUtils.countMatches(url.getFullUrl(), HTTPS) <= 1))
                .collect(Collectors.toList());
    }

    private String removeInvalidCharacters(String line) {
        URL_REPLACERS.forEach((key, value) -> {
            line.replaceAll(key, value);
        });

        return line;
    }

    private File openFile() throws FileNotFoundException {
        File dataFile = new File(file);
        if(!dataFile.isFile()) {
            throw new FileNotFoundException();
        }

        return dataFile;
    }
}