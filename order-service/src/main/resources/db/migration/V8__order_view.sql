create table order_view(
	order_id bigint primary key,
	customer_name varchar(255) not null,
	status varchar(32) not null,
	total double precision  not null,
	item_count int not null,
	created_at timestamp not null 
);

create index idx_order_view_customer on order_view(customer_name);