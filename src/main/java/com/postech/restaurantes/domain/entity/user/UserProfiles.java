package com.postech.restaurantes.domain.entity.user;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.vo.Cpf;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Os perfis de um usuário — as especializações {@code owners}, {@code clients}, {@code couriers} e
 * {@code admins} do modelo. Cada um é opcional; o conjunto, não: todo usuário tem ao menos um
 * (especialização total), e pode ter vários (sobreposta).
 *
 * <p>Também é aqui que mora a regra que cruza dois perfis: cliente e entregador são a mesma pessoa,
 * então o CPF dos dois é o mesmo. E é daqui que sai o papel de autorização — derivado, nunca
 * gravado: perfil e papel não têm como discordar.
 */
public record UserProfiles(OwnerProfile owner, ClientProfile client, CourierProfile courier, AdminProfile admin) {

    public UserProfiles {
        Guard.require(owner != null || client != null || courier != null || admin != null,
                "Usuário deve ter ao menos um perfil");
        Guard.require(client == null || courier == null || client.getCpf().equals(courier.getCpf()),
                "O CPF do perfil de cliente e o do perfil de entregador devem ser o mesmo");
    }

    /** O CPF da pessoa, se algum perfil o tiver — cliente e entregador têm o mesmo, pela regra acima. */
    public Optional<Cpf> cpf() {
        if (client != null) {
            return Optional.of(client.getCpf());
        }
        return Optional.ofNullable(courier).map(CourierProfile::getCpf);
    }

    /** Os papéis que os perfis dão, sempre na mesma ordem. */
    public Set<RoleName> roles() {
        Set<RoleName> roles = new LinkedHashSet<>();
        if (owner != null) {
            roles.add(RoleName.ROLE_OWNER);
        }
        if (client != null) {
            roles.add(RoleName.ROLE_CLIENT);
        }
        if (courier != null) {
            roles.add(RoleName.ROLE_COURIER);
        }
        if (admin != null) {
            roles.add(RoleName.ROLE_ADMIN);
        }
        return Collections.unmodifiableSet(roles);
    }
}
