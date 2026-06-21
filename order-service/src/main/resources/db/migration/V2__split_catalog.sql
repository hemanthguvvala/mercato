-- Phase 1 / Step 3: catalog-service now owns products.
-- order_item stops referencing the local product table and stores a snapshot instead.

-- 1) Snapshot columns (nullable: existing rows pre-date the snapshot)
alter table order_item add column product_name varchar(255);
alter table order_item add column unit_price double;

-- 2) Drop the cross-service foreign key, then the local product table.
--    (FK name is the one Hibernate generated in V1__init.sql.)
alter table order_item drop constraint if exists FK551losx9j75ss5d6bfsqvijna;
drop table if exists product;
