package com.tcaulk.redditbooks.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BigQueryComment {

    private String body;
    private String subreddit;

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("subreddit")
    public String getSubreddit() {
        return subreddit;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }
}