package com.postech.restaurantes.infrastructure.web.error;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Monta as respostas de erro em um formato só, venham do handler de exceções ou da cadeia de
 * segurança — que responde antes de a requisição chegar ao Spring MVC e, sem isso, teria
 * formato próprio.
 *
 * <p>O {@code timestamp} vem do {@code Clock} da aplicação, o mesmo dos casos de uso e da
 * auditoria: um relógio só no sistema.
 */
@Component
public class ProblemDetailFactory {

    static final String TIMESTAMP = "timestamp";

    private final Clock clock;

    public ProblemDetailFactory(Clock clock) {
        this.clock = clock;
    }

    /**
     * Problema da categoria informada. O {@code instance} fica vazio aqui: nas respostas do
     * Spring MVC ele é preenchido com o caminho da requisição pelo próprio framework.
     */
    public ProblemDetail create(ProblemType type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setType(type.type());
        problem.setTitle(type.title());
        problem.setProperty(TIMESTAMP, LocalDateTime.now(clock));
        return problem;
    }

    /** Para respostas escritas fora do Spring MVC, que não recebem o {@code instance} de graça. */
    public ProblemDetail create(ProblemType type, String detail, String requestPath) {
        ProblemDetail problem = create(type, detail);
        problem.setInstance(URI.create(requestPath));
        return problem;
    }

    /**
     * Acrescenta o {@code timestamp} aos problemas que o Spring monta sozinho (método não
     * suportado, rota inexistente, ...), para que <em>toda</em> resposta de erro tenha a mesma
     * forma. Os montados por {@link #create} já o têm e ficam como estão.
     */
    public void stamp(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();
        if (properties == null || !properties.containsKey(TIMESTAMP)) {
            problem.setProperty(TIMESTAMP, LocalDateTime.now(clock));
        }
    }
}
