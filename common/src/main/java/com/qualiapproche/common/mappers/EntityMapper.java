package com.qualiapproche.common.mappers;

import java.util.List;
import java.util.UUID;

public interface EntityMapper<D, E> {

    E toEntity(D dto);

    D toDto(E entity);

    List<E> toEntity(List<D> dtoList);

    List<D> toDtos(List<E> entityList);

    void updateEntityFromDto(D dto, @org.mapstruct.MappingTarget E entity);

    E map(UUID id);
    E map(String id);
}
