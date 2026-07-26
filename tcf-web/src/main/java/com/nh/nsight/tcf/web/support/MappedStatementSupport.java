package com.nh.nsight.tcf.web.support;

import org.apache.ibatis.mapping.MappedStatement;

final class MappedStatementSupport {

    private MappedStatementSupport() {}

    @SuppressWarnings("deprecation")
    static MappedStatement copyWithTimeout(MappedStatement source, int timeoutSec) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                source.getConfiguration(), source.getId(), source.getSqlSource(), source.getSqlCommandType());
        builder.resource(source.getResource());
        builder.fetchSize(source.getFetchSize());
        builder.statementType(source.getStatementType());
        builder.keyGenerator(source.getKeyGenerator());
        builder.timeout(timeoutSec);
        builder.parameterMap(source.getParameterMap());
        builder.resultMaps(source.getResultMaps());
        builder.resultSetType(source.getResultSetType());
        builder.cache(source.getCache());
        builder.flushCacheRequired(source.isFlushCacheRequired());
        builder.useCache(source.isUseCache());
        builder.resultOrdered(source.isResultOrdered());
        builder.keyColumn(source.getKeyColumns() == null ? null : String.join(",", source.getKeyColumns()));
        builder.keyProperty(source.getKeyProperties() == null ? null : String.join(",", source.getKeyProperties()));
        builder.databaseId(source.getDatabaseId());
        builder.lang(source.getLang());
        builder.resulSets(source.getResulSets() == null ? null : String.join(",", source.getResulSets()));
        return builder.build();
    }
}
