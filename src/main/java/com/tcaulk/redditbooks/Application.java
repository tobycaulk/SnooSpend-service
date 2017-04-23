package com.tcaulk.redditbooks;

import com.linkedin.urls.Url;
import com.tcaulk.redditbooks.service.ProductService;
import com.tcaulk.redditbooks.service.URLScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Application {

    private ProductService productService;

    @Autowired
    public Application(ProductService productService) {
        this.productService = productService;
    }

    @PostConstruct
    public void onStartup() {
        URLScraper urlScraper = new URLScraper("/Users/tobycaulk/Documents/Projects/SnooSpend/Reddit Data/RC_2006-09.json");
        try {
            Map<Url, Long> imgurUrls = urlScraper.scrapeUrlsPopularity("amazon");
            imgurUrls.forEach((key, value) -> {
                System.out.println(key.getFullUrl() + " [" + value + "]");
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
        ///Users/tobycaulk/Documents/RedditBooks/data1.json
        //productService.retrieveProducts("/Users/tobycaulk/Documents/Projects/SnooSpend/Reddit Data/RC_2006-09.json");
        //productService.postProcessProducts();
        //productService.consolidate();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
