package co.edu.eci.blueprints.filters;

import co.edu.eci.blueprints.model.Blueprint;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Filtro por defecto: devuelve el blueprint sin modificar.
 *
 * El perfil "!redundancy and !undersampling" lo desactiva cuando hay otro filtro
 * activo. Sin esa condicion habria DOS beans de tipo BlueprintsFilter al activar
 * cualquiera de los otros perfiles, y la inyeccion en BlueprintsServices fallaria
 * con NoUniqueBeanDefinitionException.
 */
@Component
@Profile("!redundancy & !undersampling")
public class IdentityFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) { return bp; }
}
