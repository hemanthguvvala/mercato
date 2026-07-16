package com.interview.catalogservice.web;

public record ProductSearchCriteria(String name, Double minPrice, Double maxPrice, Long categoryId) {

}
