package dst.ass1.jooq.dao;

public interface GenericDAO<T> {
    T findById(Long id);

    T insert(T model);

    void delete(Long id);

}
