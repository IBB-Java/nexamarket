package com.nexamarket.catalog.application;

import java.util.Map;
import java.util.Set;

public interface SellerDirectoryGateway {

    Map<Long, String> displayNames(Set<Long> sellerIds);
}
