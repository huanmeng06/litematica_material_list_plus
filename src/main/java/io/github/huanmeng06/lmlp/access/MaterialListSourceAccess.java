package io.github.huanmeng06.lmlp.access;

import io.github.huanmeng06.lmlp.cache.MaterialListDataSource;

public interface MaterialListSourceAccess {
    MaterialListDataSource lmlp$getDataSource();

    void lmlp$setDataSource(MaterialListDataSource dataSource);
}
