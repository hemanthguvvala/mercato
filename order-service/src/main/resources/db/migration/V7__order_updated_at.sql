alter table orders add column updated_at timestamp;
update orders set updated_at = now();
alter table orders alter column updated_at set not null;