package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.Optional;
import java.util.UUID;

/**
 * Origem de dados de usuários. Contrato que a infraestrutura implementa (JPA na Etapa 5);
 * o gateway só conhece esta interface.
 */
public interface IUserDataSource {

    Optional<UserData> findById(UUID id);

    Optional<UserData> findByLogin(String login);

    /** O e-mail chega já normalizado (minúsculas) pelo VO do domínio. */
    Optional<UserData> findByEmail(String email);

    /**
     * Busca parcial por nome sem diferenciar maiúsculas; nome nulo lista todos. A
     * propriedade de ordenação já vem validada pelo caso de uso; cabe à implementação
     * traduzi-la para a sua coluna/atributo.
     */
    PageResult<UserData> search(String name, PageRequest request);

    UserData insert(UserData user);

    UserData update(UserData user);

    void delete(UUID id);
}
