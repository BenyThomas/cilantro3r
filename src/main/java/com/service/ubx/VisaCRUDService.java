package com.service.ubx;

import java.util.List;

public interface VisaCRUDService<T, ID>{
    T create(T entity);

    T update(ID id, T entity);

    T getById(ID id);

    void deleteById(ID id);

    List<T> getAll();
}
