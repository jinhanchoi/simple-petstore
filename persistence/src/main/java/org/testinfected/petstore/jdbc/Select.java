package org.testinfected.petstore.jdbc;

import com.pyxis.petstore.domain.billing.PaymentMethod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Select<T> {

    public static <T> Select<T> from(final Record<T> record) {
        return from(record, record.table());
    }

    public static <T> Select<T> from(final Record<T> record, String alias) {
        return new Select<T>(record, alias);
    }

    private final Record<T> from;

    private final List<Record<?>> joins = new ArrayList<Record<?>>();
    private final StringBuilder joinClause = new StringBuilder();
    private final StringBuilder whereClause = new StringBuilder();
    private final List<Object> parameters = new ArrayList<Object>();
    private final StringBuilder orderByClause = new StringBuilder();
    private final Map<String, String> aliases = new HashMap<String, String>();

    public Select(Record<T> from, String alias) {
        this.from = from;
        aliasTable(from.table(), alias);
    }

    private void aliasTable(final String table, String alias) {
        aliases.put(table, alias);
    }

    public void innerJoin(Record<?> join, String alias, String clause) {
        join(join, alias, "inner join", clause);
    }

    public void leftJoin(Record<PaymentMethod> join, String alias, String clause) {
        join(join, alias, "left outer join", clause);
    }

    private void join(Record<?> join, String alias, String joinType, String clause) {
        joins.add(join);
        aliasTable(join.table(), alias);
        joinClause.append(" ").append(joinType).append(" ").append(join.table()).append(" ").append(aliasOf(join)).append(" on ").append(clause);

    }

    public void where(String clause, Object... values) {
        whereClause.append(" where ").append(clause);
        addParameters(values);
    }

    public T single(final Connection connection) {
        return list(connection).get(0);
    }

    public List<T> list(final Connection connection) {
        List<T> entities = new ArrayList<T>();
        PreparedStatement query = null;
        try {
            query = connection.prepareStatement(buildSelectStatement());
            for (int index = 0; index < parameters.size(); index++) {
                setParameter(query, index);
            }
            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                entities.add(from.hydrate(resultSet));
            }
        } catch (SQLException e) {
            throw new JDBCException("Could not execute query", e);
        } finally {
            Sql.close(query);
        }
        return entities;
    }

    private String buildSelectStatement() {
        StringBuilder sql = new StringBuilder();
        sql.append(selectClause());
        sql.append(fromClause());
        sql.append(joinClause);
        sql.append(whereClause);
        sql.append(orderByClause);
        System.out.println(sql);
        return sql.toString();
    }

    private String fromClause() {
        return " from " + from.table() + " " + aliasOf(from);
    }

    private String selectClause() {
        return "select " + Sql.asString(listColumns());
    }

    private Collection<String> listColumns() {
        Collection<String> names = new ArrayList<String>();
        names.addAll(columnsFor(from));
        for (Record<?> join : joins) {
            names.addAll(columnsFor(join));
        }
        return names;
    }

    private String aliasOf(final Record<?> record) {
        return aliases.get(record.table());
    }


    public List<String> columnsFor(Record<?> record) {
        List<String> columns = new ArrayList<String>();
        for (String column : record.columns()) {
            columns.add(aliasOf(record) + "." + column);
        }
        return columns;
    }

    private void setParameter(PreparedStatement query, int index) throws SQLException {
        int sqlType = query.getParameterMetaData().getParameterType(index + 1);
        query.setObject(index + 1, parameters.get(index), sqlType);
    }

    private void addParameters(Object... values) {
        for (Object value : values) {
            addParameter(value);
        }
    }

    private void addParameter(Object value) {
        parameters.add(value);
    }

    public void orderBy(String clause) {
        orderByClause.append(" order by ").append(clause);
    }
}