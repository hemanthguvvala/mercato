create table product_detail(
	id bigint primary key,
	description varchar(2000),
	constraint fk_detail_product foreign key(id) references product(id)
);