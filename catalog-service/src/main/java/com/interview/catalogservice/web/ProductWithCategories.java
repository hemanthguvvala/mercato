package com.interview.catalogservice.web;

public record ProductWithCategories(Long id, String name, double price, java.util.List<CategoryView> categories) {

	public record CategoryView(Long id, String name, int position) {
	}
}
