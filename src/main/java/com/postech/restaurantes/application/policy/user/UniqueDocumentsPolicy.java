package com.postech.restaurantes.application.policy.user;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Cpf;
import java.util.Optional;
import java.util.UUID;

/**
 * CPF e CNPJ identificam a pessoa e a empresa: outro cadastro com o mesmo documento é conflito, com a
 * mensagem certa — como e-mail e login —, e não a recusa genérica da restrição do banco. Usada pelo
 * cadastro e pela manutenção de perfis, para a regra existir uma vez só.
 *
 * <p>Só o documento que <em>mudou</em> é consultado: um que o usuário já tinha já foi conferido quando
 * entrou, e o banco garante que continua dele (unicidade de CPF entre {@code clients} e
 * {@code couriers}, migration V7).
 */
public final class UniqueDocumentsPolicy {

    private final IUserGateway userGateway;

    private UniqueDocumentsPolicy(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static UniqueDocumentsPolicy create(IUserGateway userGateway) {
        return new UniqueDocumentsPolicy(userGateway);
    }

    /**
     * @param before os perfis atuais, ou {@code null} num cadastro novo
     * @param after  os perfis pedidos
     * @param self   o id do próprio usuário, que não conta como "outro"; {@code null} num cadastro novo
     */
    public void requireUnique(UserProfiles before, UserProfiles after, UUID self) {
        Optional<Cpf> cpf = after.cpf();
        Optional<Cpf> previousCpf = before == null ? Optional.empty() : before.cpf();
        if (cpf.isPresent() && !cpf.equals(previousCpf)
                && userGateway.findByCpf(cpf.get()).filter(other -> !other.getId().equals(self)).isPresent()) {
            throw new DuplicateResourceException("CPF já cadastrado");
        }
        Cnpj cnpj = after.owner() == null ? null : after.owner().getCnpj();
        Cnpj previous = before == null || before.owner() == null ? null : before.owner().getCnpj();
        if (cnpj != null && !cnpj.equals(previous)
                && userGateway.findByCnpj(cnpj).filter(other -> !other.getId().equals(self)).isPresent()) {
            throw new DuplicateResourceException("CNPJ já cadastrado");
        }
    }
}
