package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;


public interface EntityMapper<D, E> {

    E toEntity(D dto);

    D toDto(E entity);

    List<E> toEntity(List<D> dtoList);

    List<D> toDto(List<E> entityList);

    E map(UUID id);
    E map(String id);
}
