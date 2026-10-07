package com.postech.restaurantes.infrastructure.persistence.jpa.user.courier;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Tabela {@code couriers}: perfil de entregador. A chave primária é a do usuário, atribuída pela
 * origem de dados; o perfil não referencia o usuário (ver {@code UserJpaEntity}).
 *
 * <p>{@code vehicle_type} e {@code status} são tipos {@code ENUM} do PostgreSQL. Aqui são texto — a
 * infraestrutura não importa os enums do domínio (regra {@code infraestrutura_so_conhece_do_dominio_as_excecoes});
 * quem converte é o gateway. {@code columnDefinition} diz ao {@code ddl-auto: validate} qual é o tipo
 * da coluna, e o {@code ?::tipo} na escrita faz o banco aceitar o texto na coluna {@code ENUM}.
 */
@Entity
@Table(name = "couriers")
public class CourierJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "driver_license_number", unique = true, length = 11)
    private String driverLicense;

    @Column(name = "vehicle_type", nullable = false, columnDefinition = "courier_vehicle_type")
    @ColumnTransformer(write = "?::courier_vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_plate", length = 8)
    private String vehiclePlate;

    @Column(name = "status", nullable = false, columnDefinition = "courier_status")
    @ColumnTransformer(write = "?::courier_status")
    private String status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDriverLicense() {
        return driverLicense;
    }

    public void setDriverLicense(String driverLicense) {
        this.driverLicense = driverLicense;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
