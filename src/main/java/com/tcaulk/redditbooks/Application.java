package com.tcaulk.redditbooks;

import com.tcaulk.redditbooks.service.OldProductService;
import com.tcaulk.redditbooks.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class Application {

    private ProductService productService;

    @Autowired
    public Application(ProductService productService) {
        this.productService = productService;
    }

    @PostConstruct
    public void onStartup() {
        /*URLScraper urlScraper = new URLScraper("/Users/tobycaulk/Documents/Projects/SnooSpend/Reddit Data/RC_2006-09.json");
        try {
            Map<Url, Long> imgurUrls = urlScraper.scrapeUrlsPopularity("amazon");
            imgurUrls.forEach((key, value) -> {
                System.out.println(key.getFullUrl() + " [" + value + "]");
            });
        } catch(Exception e) {
            e.printStackTrace();
        }*/
        ///Users/tobycaulk/Documents/RedditBooks/data1.json
        //productService.retrieveProducts("/Users/tobycaulk/Documents/Projects/SnooSpend/RedditData/RC_2008-03.json");
        //productService.postProcessProducts();
        //productService.consolidate();

        productService.processFile("/Users/tobycaulk/Documents/Projects/SnooSpend/RedditData/results_20170429.json");
        //productService.processAmazonProducts();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
