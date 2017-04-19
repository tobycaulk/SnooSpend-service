package com.tcaulk.redditbooks.model;

import com.ECS.client.jax.Item;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;

import java.util.Date;

public class Product implements Comparable<Product> {

    private String id;
    private String asin;
    private Date createDate;
    private int popularity;
    private boolean processed;
    private String month;
    private Item item;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("ASIN")
    public String getASIN() {
        return asin;
    }

    public void setASIN(String asin) {
        this.asin = asin;
    }

    @JsonProperty("CreateDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @JsonProperty("Popularity")
    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    @JsonProperty("Processed")
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    @JsonProperty("Month")
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @JsonProperty("Item")
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public int compareTo(Product product) {
        return popularity == product.popularity ? 0 : popularity > product.popularity ? 1 : -1;
    }
}
