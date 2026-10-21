package com.qualiapproche.common.messagerie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface MessageTraiteRepository<E extends AbstractMessageTraite> extends JpaRepository<E, UUID> {
}
