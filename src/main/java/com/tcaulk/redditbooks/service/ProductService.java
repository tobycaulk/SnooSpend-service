package com.tcaulk.redditbooks.service;

import com.ECS.client.jax.ItemLookupRequest;
import com.ECS.client.jax.Items;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.tcaulk.redditbooks.model.Product;
import com.tcaulk.redditbooks.service.mongo.ProductRepository;
import de.malkusch.amazon.ecs.ProductAdvertisingAPI;
import de.malkusch.amazon.ecs.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private static final String AWS_ACCESS_KEY_ID = "AKIAIMJDHDNJ3RYVDGHQ";
    private static final String AWS_SECRET_KEY = "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";
    private static final String ENDPOINT = "webservices.amazon.com";

    private ProductRepository productRepository;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);
    private ProductAdvertisingAPI productAdvertisingAPI;
    private String month;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          @Value("${accessKey}") String accessKey,
                          @Value("${secretKey}") String secretKey,
                          @Value("${associateTag}") String associateTag,
                          @Value("${month}") String month) {

        this.month = month;
        this.productRepository = productRepository;

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

    public void retrieveProducts(String fileUrl) {
        File file = new File(fileUrl);

        long startTime = System.currentTimeMillis();
        try {
            LineIterator it = FileUtils.lineIterator(file);
            while(it.hasNext()) {
                String line = it.nextLine();
                line = line.replaceAll("\\)", " ");
                line = line.replaceAll("\"", " ");
                line = line.replaceAll("\\\\n", "");
                line = line.replaceAll("\\\\r", "");
                line = line.replaceAll("\'", " ");
                line = line.replaceAll("\\(", " ");
                line = line.replaceAll("\\[", " ");
                line = line.replaceAll("]", " ");
                line = line.replaceAll("\\?", "");
                line = line.replaceAll("\\*", "");

                try {
                    List<Url> urls = new UrlDetector(line, UrlDetectorOptions.Default).detect();
                    Map<String, Integer> products = new HashMap<>();
                    urls.stream().filter(url -> url.getFullUrl().contains("amazon") && (StringUtils.countMatches(url.getFullUrl(), "http") <= 1 || StringUtils.countMatches(url.getFullUrl(), "http") <= 1)).forEach(url -> {
                        String asin = getASINFromURL(url.getFullUrl());
                        if(!StringUtils.isEmpty(asin)) {
                            if (!products.containsKey(asin)) {
                                products.put(asin, 0);
                            }
                            products.put(asin, products.get(asin) + 1);
                            System.out.println(url.getFullUrl() + " [" + asin + "] [" + products.get(asin) + "]");
                        }
                    });

                    if(!products.isEmpty()) {
                        processProducts(new HashMap<String, Integer>(products));
                    }

                    products.clear();
                } catch(Exception e) {
                    //e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        System.out.println("Processing took [" + delta + "] milliseconds");
    }

    private String getASINFromURL(String url) {
        String asin = "";

        Pattern asinPattern = Pattern.compile("\\/dp\\/\\w+");
        Matcher asinMatcher = asinPattern.matcher(url);
        while(asinMatcher.find()) {
            String asinWithDP = asinMatcher.group();
            asin = asinWithDP.substring(4, 14);
        }

        return asin;
    }

    public void postProcessProducts() {
        List<Product> stagedProducts = productRepository.findByProcessed(false);
        Collections.sort(stagedProducts);

        AtomicInteger currentIndex = new AtomicInteger(-1);
        executorService.scheduleAtFixedRate(() -> {
            executorService.execute(() -> {
                getAmazonProductDetails(stagedProducts.get(currentIndex.incrementAndGet()));
            });
        },0,1250, TimeUnit.MILLISECONDS);
    }

    private Product getAmazonProductDetails(Product product) {
        if(product.getASIN() != null) {
            ItemLookupRequest request = new ItemLookupRequest();
            request.getItemId().add(product.getASIN());
            request.getResponseGroup().add("Images");
            request.getResponseGroup().add("ItemAttributes");
            request.getResponseGroup().add("EditorialReview");
            request.getResponseGroup().add("Offers");

            try {
                Items items = productAdvertisingAPI.getItemLookup().call(request);
                product.setProcessed(true);
                product.setItem(items.getItem().get(0));

                productRepository.save(product);
                System.out.println("Saved item[" + product.getASIN() + "]");
            } catch (Exception e) {
                e.printStackTrace();
                productRepository.delete(product);
            }
        }

        return product;
    }

    private Product getProduct(String asin, int popularity) {
        Product product = new Product();

        product.setCreateDate(new Date());
        product.setPopularity(popularity);
        product.setMonth(month);

        product.setASIN(asin);

        return product;
    }

    private void processProducts(Map<String, Integer> products) {
        System.out.println("Process products");
        products.keySet().stream().forEach(key -> {
            System.out.println("Processing key[" + key + "]");
            Product product = getProduct(key, products.get(key));
            if(product.getASIN() != null) {
                System.out.println("asin isnt null");
                Product repositoryProduct = null;
                try {
                    repositoryProduct = productRepository.findByAsin(product.getASIN());
                } catch(Exception e) {
                    e.printStackTrace();
                }
                if(repositoryProduct != null) {
                    System.out.println(repositoryProduct.getASIN());
                    System.out.println("Saved");
                    repositoryProduct.setPopularity(repositoryProduct.getPopularity() + products.get(key));
                    productRepository.save(repositoryProduct);
                } else {
                    System.out.print("Inserted");
                    productRepository.insert(product);
                }
            }
        });
    }

    public void consolidate() {
        List<Product> products = productRepository.findAll();
        HashMap<String, Product> map = new HashMap<>();

        for(Product product : products) {
            if(!map.containsKey(product.getASIN())) {
                map.put(product.getASIN(), product);
            } else {
                Product mapProduct = map.get(product.getASIN());
                mapProduct.setPopularity(mapProduct.getPopularity() + 1);

                map.put(product.getASIN(), mapProduct);
                productRepository.delete(product);
            }
        }

        for(String asin : map.keySet()) {
            productRepository.save(map.get(asin));
        }

        System.out.println("Done");
    }
}