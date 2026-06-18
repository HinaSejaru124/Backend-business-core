package com.yowyob.businesscore.adapter.out.persistence.actor;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface RoleMetierRepository extends ReactiveCrudRepository<RoleMetierEntity, UUID> {
}