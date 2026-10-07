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

    /** Os mesmos perfis com o de dono incluído ou trocado; o conjunto novo passa pelas mesmas regras. */
    public UserProfiles withOwner(OwnerProfile newOwner) {
        return new UserProfiles(Guard.requireNonNull(newOwner, "Perfil de dono inválido"), client, courier, admin);
    }

    /**
     * Ver {@link #withOwner}. O CPF é da pessoa: <em>alterar</em> o perfil de cliente com outro CPF corrige o
     * CPF também no de entregador. <em>Incluir</em> o perfil de cliente com CPF diferente do entregador
     * continua recusado — ali a divergência é engano, não correção.
     */
    public UserProfiles withClient(ClientProfile newClient) {
        Guard.requireNonNull(newClient, "Perfil de cliente inválido");
        CourierProfile alignedCourier = client != null ? withCpf(courier, newClient.getCpf()) : courier;
        return new UserProfiles(owner, newClient, alignedCourier, admin);
    }

    /** Ver {@link #withClient}, do lado do entregador. */
    public UserProfiles withCourier(CourierProfile newCourier) {
        Guard.requireNonNull(newCourier, "Perfil de entregador inválido");
        ClientProfile alignedClient = courier != null ? withCpf(client, newCourier.getCpf()) : client;
        return new UserProfiles(owner, alignedClient, newCourier, admin);
    }

    /** Ver {@link #withOwner}. */
    public UserProfiles withAdmin(AdminProfile newAdmin) {
        return new UserProfiles(owner, client, courier,
                Guard.requireNonNull(newAdmin, "Perfil de administrador inválido"));
    }

    /**
     * Os mesmos perfis sem o do tipo informado. Tirar o último é recusado pela regra "ao menos um
     * perfil"; tirar um que não existe devolve o mesmo conjunto — quem decide se isso é erro é o caso
     * de uso, que conhece o pedido.
     */
    public UserProfiles without(ProfileType type) {
        Guard.requireNonNull(type, "Tipo de perfil inválido");
        return new UserProfiles(
                type == ProfileType.OWNER ? null : owner,
                type == ProfileType.CLIENT ? null : client,
                type == ProfileType.COURIER ? null : courier,
                type == ProfileType.ADMIN ? null : admin);
    }

    public boolean has(ProfileType type) {
        Guard.requireNonNull(type, "Tipo de perfil inválido");
        if (type == ProfileType.OWNER) {
            return owner != null;
        }
        if (type == ProfileType.CLIENT) {
            return client != null;
        }
        if (type == ProfileType.COURIER) {
            return courier != null;
        }
        return admin != null;
    }

    /** Cópia do perfil com outro CPF; o original não muda, para um pedido recusado não deixar rastro. */
    private static CourierProfile withCpf(CourierProfile courier, Cpf cpf) {
        if (courier == null || courier.getCpf().equals(cpf)) {
            return courier;
        }
        return CourierProfile.restore(cpf.value(), courier.getPhone().value(), courier.getVehicleType(),
                courier.getDriverLicense() == null ? null : courier.getDriverLicense().value(),
                courier.getVehiclePlate() == null ? null : courier.getVehiclePlate().value(), courier.getStatus());
    }

    private static ClientProfile withCpf(ClientProfile client, Cpf cpf) {
        if (client == null || client.getCpf().equals(cpf)) {
            return client;
        }
        return ClientProfile.restore(cpf.value(), client.getPhone().value(), client.getBirthDate());
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
