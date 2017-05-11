package com.tcaulk.redditbooks.service;

import com.ECS.client.jax.ItemLookupRequest;
import com.ECS.client.jax.Items;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.tcaulk.redditbooks.model.BigQueryComment;
import com.tcaulk.redditbooks.model.Product;
import com.tcaulk.redditbooks.service.mongo.ProductRepository;
import de.malkusch.amazon.ecs.ProductAdvertisingAPI;
import de.malkusch.amazon.ecs.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductService {
    private static final Logger log = Logger.getLogger(ProductService.class);

    private static final int DP_START_INDEX = 4;
    private static final int DP_LENGTH = 10;
    private static final int API_RATE_DELAY = 1250;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern ASIN_PATTERN = Pattern.compile("\\/dp\\/\\w+");
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(20);
    private static final List<String> PRODUCT_ITEM_RESPONSE_GROUPS = Arrays.asList("Images", "ItemAttributes", "EditorialReview", "Offers");

    private ProductRepository productRepository;
    private ProductAdvertisingAPI productAdvertisingAPI;
    private Map<String, Product> discoveredProducts = new HashMap<>();

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            @Value("${accessKey}") String accessKey,
            @Value("${secretKey}") String secretKey,
            @Value("${associateTag}") String associateTag) {

        this.productRepository = productRepository;
        this.productAdvertisingAPI = productAdvertisingAPI;

        setupProductAdvertisingAPI(associateTag, accessKey, secretKey);
    }

    private void setupProductAdvertisingAPI(String associateTag, String accessKey, String secretKey) {
        Properties properties = new Properties();
        try {
            properties.setProperty("amazon.accessKey", accessKey);
            properties.setProperty("amazon.secretKey", secretKey);
            properties.setProperty("amazon.associateTag", associateTag);

            productAdvertisingAPI = new ProductAdvertisingAPI(new PropertiesConfiguration(properties));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processFile(String fileUrl) {
        File file = new File(fileUrl);

        long startTime = System.currentTimeMillis();

        try {
            LineIterator it = FileUtils.lineIterator(file);
            while(it.hasNext()) {
                BigQueryComment comment = MAPPER.readValue(it.nextLine(), BigQueryComment.class);
                if(comment != null) {
                    processComment(comment);
                }
            }
        } catch(Exception e) {
            log.error("Error while processing file [" + fileUrl + "]", e);
        }

        long endTime = System.currentTimeMillis();
        log.info("Processing took [" + (endTime - startTime) + "] milliseconds");
    }

    public void processAmazonProducts() {
        List<Product> products = new ArrayList<>();
        discoveredProducts.forEach((key, value) -> {
            products.add(value);
        });
        Collections.sort(products);

        AtomicInteger currentIndex = new AtomicInteger(-1);
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            Product product = products.get(currentIndex.incrementAndGet());

            ItemLookupRequest request = new ItemLookupRequest();
            request.getItemId().add(product.getASIN());
            PRODUCT_ITEM_RESPONSE_GROUPS.forEach(responseGroup -> {
                request.getResponseGroup().add(responseGroup);
            });

            try {
                Items items = productAdvertisingAPI.getItemLookup().call(request);
                product.setCreateDate(new Date());
                product.setItem(items.getItem().get(0));

                productRepository.insert(product);
                log.info("Inserted product with ASIN[" + product.getASIN() + "]");
            } catch(Exception e) {
                log.error("Error while retrieving item from Amazon with ASIN[" + product.getASIN() + "]", e);
            }
        }, 0, API_RATE_DELAY, TimeUnit.MILLISECONDS);
    }

    private void processComment(BigQueryComment comment) {
        List<Url> urls = detectUrlsInComment(comment);
        urls.stream()
            .filter(url ->
                url.getFullUrl().contains("amazon")
                && (StringUtils.countMatches(url.getFullUrl(), "http") <= 1
                || StringUtils.countMatches(url.getFullUrl(), "https") <= 1))
            .forEach(url -> {
                String asin = getAsinFromUrl(url);
                if(!StringUtils.isEmpty(asin)) {
                    if(!discoveredProducts.containsKey(asin)) {
                        Product product = new Product();
                        product.setASIN(asin);
                        product.addSubreddit(comment.getSubreddit());
                        product.setPopularity(0);

                        discoveredProducts.put(asin, product);
                    }

                    Product product = discoveredProducts.get(asin);
                    product.setPopularity(product.getPopularity() + 1);
                    product.addSubreddit(comment.getSubreddit());
                    discoveredProducts.put(asin, product);
                }
            });
    }

    private List<Url> detectUrlsInComment(BigQueryComment comment) {
        return new UrlDetector(comment.getBody(), UrlDetectorOptions.Default).detect();
    }

    private String getAsinFromUrl(Url url) {
        String asin = "";

        Matcher asinMatcher = ASIN_PATTERN.matcher(url.getFullUrl());
        while(asinMatcher.find()) {
           asin = asinMatcher.group().substring(DP_START_INDEX, DP_START_INDEX + DP_LENGTH);
        }

        return asin;
    }
}