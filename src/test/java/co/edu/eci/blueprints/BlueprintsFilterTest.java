package co.edu.eci.blueprints;

import co.edu.eci.blueprints.filters.IdentityFilter;
import co.edu.eci.blueprints.filters.RedundancyFilter;
import co.edu.eci.blueprints.filters.UndersamplingFilter;
import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de los filtros (punto 5). No levantan contexto de Spring:
 * los filtros son logica pura sobre la lista de puntos.
 */
class BlueprintsFilterTest {

    private Blueprint bp(Point... pts) {
        return new Blueprint("john", "test", List.of(pts));
    }

    @Test
    void identity_noModificaNada() {
        Blueprint in = bp(new Point(1, 1), new Point(1, 1), new Point(2, 2));
        assertEquals(3, new IdentityFilter().apply(in).getPoints().size());
    }

    @Test
    void redundancy_eliminaDuplicadosConsecutivos() {
        Blueprint in = bp(new Point(1, 1), new Point(1, 1), new Point(1, 1),
                new Point(2, 2), new Point(2, 2), new Point(3, 3));

        List<Point> out = new RedundancyFilter().apply(in).getPoints();

        assertEquals(List.of(new Point(1, 1), new Point(2, 2), new Point(3, 3)), out);
    }

    @Test
    void redundancy_conservaDuplicadosNoConsecutivos() {
        // (1,1) aparece dos veces pero separadas: ambas se conservan.
        Blueprint in = bp(new Point(1, 1), new Point(2, 2), new Point(1, 1));

        assertEquals(3, new RedundancyFilter().apply(in).getPoints().size());
    }

    @Test
    void undersampling_conservaIndicesPares() {
        Blueprint in = bp(new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10));

        List<Point> out = new UndersamplingFilter().apply(in).getPoints();

        assertEquals(List.of(new Point(0, 0), new Point(10, 10)), out);
    }

    @Test
    void undersampling_noReduceCuandoHayDosPuntosOMenos() {
        Blueprint in = bp(new Point(0, 0), new Point(1, 1));
        assertEquals(2, new UndersamplingFilter().apply(in).getPoints().size());
    }

    @Test
    void filtros_noMutanElBlueprintOriginal() {
        Blueprint in = bp(new Point(1, 1), new Point(1, 1));
        new RedundancyFilter().apply(in);
        assertEquals(2, in.getPoints().size(), "el filtro debe devolver una copia nueva");
    }
}
