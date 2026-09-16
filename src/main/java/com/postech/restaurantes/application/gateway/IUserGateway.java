package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.vo.Email;
import java.util.Optional;
import java.util.UUID;

/** Acesso a usuários, em termos de domínio. Quem implementa traduz para a origem de dados. */
public interface IUserGateway {

    Optional<User> findById(UUID id);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(Email email);

    /** Busca parcial por nome (sem diferenciar maiúsculas); nome nulo lista todos. */
    PageResult<User> search(String name, PageRequest request);

    /** Persiste um usuário novo e devolve a instância com id e auditoria. */
    User insert(User user);

    /** Persiste alterações de um usuário existente e devolve a instância atualizada. */
    User update(User user);

    void delete(UUID id);
}
