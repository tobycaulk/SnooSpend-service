package com.tcaulk.redditbooks;

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
        ///Users/tobycaulk/Documents/RedditBooks/data1.json
        //productService.retrieveProducts("/Users/tobycaulk/Documents/RedditBooks/data1.json");
        productService.postProcessProducts();
        //productService.consolidate();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
