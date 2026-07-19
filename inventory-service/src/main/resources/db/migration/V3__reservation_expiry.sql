alter table stock_reservation add column reserved_at timestamp;
update stock_reservation set reserved_at = now();
alter table stock_reservation alter column reserved_at set not null;