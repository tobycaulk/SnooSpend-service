package com.tcaulk.redditbooks.service.mongo;

import com.tcaulk.redditbooks.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    Product findByAsin(String asin);
}