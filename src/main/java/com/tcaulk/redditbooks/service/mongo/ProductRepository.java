package com.tcaulk.redditbooks.service.mongo;

import com.tcaulk.redditbooks.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByProcessed(boolean processed);
    Product findByAsin(String asin);
}