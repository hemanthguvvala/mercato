create table product_tag(
	product_id bigint not null,
	tag varchar(100) not null,
	primary key (product_id, tag),
	constraint fk_tag_product foreign key (product_id) references product(id) 
);