package co.edu.eci.blueprints.persistence.impl;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.persistence.BlueprintPersistence;
import co.edu.eci.blueprints.persistence.BlueprintPersistenceException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@Profile("postgres")
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private static final String SELECT_BASE = """
            SELECT b.author AS author, b.name AS name, p.x AS x, p.y AS y
            FROM blueprint b
            LEFT JOIN blueprint_point p
                   ON p.author = b.author AND p.bp_name = b.name
            """;

    private static final String INSERT_POINT =
            "INSERT INTO blueprint_point (author, bp_name, ordinal, x, y) VALUES (?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;

    public PostgresBlueprintPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        try {
            jdbc.update("INSERT INTO blueprint (author, name) VALUES (?, ?)",
                    bp.getAuthor(), bp.getName());
        } catch (DuplicateKeyException e) {
            // Equivalente al containsKey() de la version en memoria.
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: %s:%s".formatted(bp.getAuthor(), bp.getName()));
        }

        List<Point> points = bp.getPoints();
        if (!points.isEmpty()) {
            List<Object[]> batch = new ArrayList<>(points.size());
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                batch.add(new Object[]{bp.getAuthor(), bp.getName(), i, p.x(), p.y()});
            }
            jdbc.batchUpdate(INSERT_POINT, batch);
        }
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        List<Row> rows = jdbc.query(
                SELECT_BASE + "WHERE b.author = ? AND b.name = ? ORDER BY p.ordinal",
                ROW_MAPPER, author, name);

        if (rows.isEmpty()) {
            throw new BlueprintNotFoundException("Blueprint not found: %s/%s".formatted(author, name));
        }
        return group(rows).iterator().next();
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<Row> rows = jdbc.query(
                SELECT_BASE + "WHERE b.author = ? ORDER BY b.name, p.ordinal",
                ROW_MAPPER, author);

        if (rows.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return group(rows);
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        List<Row> rows = jdbc.query(
                SELECT_BASE + "ORDER BY b.author, b.name, p.ordinal", ROW_MAPPER);
        return group(rows);
    }

    @Override
    @Transactional
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blueprint WHERE author = ? AND name = ?",
                Integer.class, author, name);

        if (found == null || found == 0) {
            throw new BlueprintNotFoundException("Blueprint not found: %s/%s".formatted(author, name));
        }

        // El nuevo punto va al final: ordinal = max(ordinal) + 1.
        jdbc.update("""
                INSERT INTO blueprint_point (author, bp_name, ordinal, x, y)
                VALUES (?, ?,
                        (SELECT COALESCE(MAX(ordinal) + 1, 0)
                           FROM blueprint_point
                          WHERE author = ? AND bp_name = ?),
                        ?, ?)
                """, author, name, author, name, x, y);
    }

    // ---------------------------------------------------------------
    // Mapeo filas -> objetos de dominio
    // ---------------------------------------------------------------

    /** Fila cruda del JOIN. x / y son null cuando el blueprint no tiene puntos. */
    private record Row(String author, String name, Integer x, Integer y) {}

    private static final RowMapper<Row> ROW_MAPPER = (rs, i) -> {
        int x = rs.getInt("x");
        boolean xNull = rs.wasNull();
        int y = rs.getInt("y");
        boolean yNull = rs.wasNull();
        return new Row(rs.getString("author"), rs.getString("name"),
                xNull ? null : x, yNull ? null : y);
    };

    /**
     * Agrupa las filas del JOIN en blueprints. Se usa LinkedHashMap/LinkedHashSet
     * para conservar el orden del ORDER BY: los puntos son una secuencia y los
     * filtros (redundancia, undersampling) dependen de ese orden.
     */
    private Set<Blueprint> group(List<Row> rows) {
        Map<String, List<Point>> pointsByKey = new LinkedHashMap<>();
        Map<String, Row> headerByKey = new LinkedHashMap<>();

        for (Row row : rows) {
            String key = row.author() + ":" + row.name();
            headerByKey.putIfAbsent(key, row);
            List<Point> pts = pointsByKey.computeIfAbsent(key, k -> new ArrayList<>());
            if (row.x() != null && row.y() != null) {
                pts.add(new Point(row.x(), row.y()));
            }
        }

        Set<Blueprint> result = new LinkedHashSet<>();
        for (Map.Entry<String, Row> e : headerByKey.entrySet()) {
            Row h = e.getValue();
            result.add(new Blueprint(h.author(), h.name(), pointsByKey.get(e.getKey())));
        }
        return result;
    }
}
