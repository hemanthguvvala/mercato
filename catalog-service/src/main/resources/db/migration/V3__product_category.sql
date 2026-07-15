create table product_category(
	product_id bigint not null,
	category_id bigint not null,
	position int not null,
	primary key (product_id, category_id),
	constraint fk_pc_product foreign key (product_id) references product(id),
	constraint fk_pc_category foreign key (category_id) references category(id)
);