package com.tcaulk.redditbooks.service;

import com.linkedin.urls.Url;

public class UrlWrapper {

    public final Url url;

    public UrlWrapper(Url url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object obj) {
        boolean equal = false;

        if(obj != null && obj instanceof UrlWrapper) {
            UrlWrapper otherUrl = (UrlWrapper) obj;
            equal = otherUrl.url.getFullUrl().equals(url.getFullUrl());
        }

        return equal;
    }
}
