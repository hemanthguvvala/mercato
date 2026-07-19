package com.interview.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.inventoryservice.entity.InboxMessage;

@Repository
public interface InboxRepository extends JpaRepository<InboxMessage, String> {

}
