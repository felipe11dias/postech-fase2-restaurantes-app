package com.postech.restaurantes.domain.entity.courier;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.Cpf;
import com.postech.restaurantes.domain.vo.DriverLicense;
import com.postech.restaurantes.domain.vo.LicensePlate;
import com.postech.restaurantes.domain.vo.Phone;

/**
 * Perfil de entregador — a especialização {@code couriers} do usuário. Não tem id próprio: a
 * identidade é a do usuário, e o perfil será parte do agregado {@code User}.
 *
 * <p>Invariantes: CPF e telefone válidos; tipo de veículo e status informados. CNH e placa andam
 * com o veículo: motorizado ({@link CourierVehicleType#requiresLicense()}) exige as duas; a pé ou
 * de bicicleta, nenhuma — por isso as três mudam juntas ({@link #changeVehicle}). Todo entregador
 * novo começa {@link CourierStatus#OFFLINE}.
 */
public final class CourierProfile {

    private Cpf cpf;
    private Phone phone;
    private CourierVehicleType vehicleType;
    private DriverLicense driverLicense;
    private LicensePlate vehiclePlate;
    private CourierStatus status;

    private CourierProfile() {
    }

    /** Entregador novo: começa fora de serviço. */
    public static CourierProfile create(String cpf, String phone, CourierVehicleType vehicleType,
                                        String driverLicense, String vehiclePlate) {
        return fill(new CourierProfile(), cpf, phone, vehicleType, driverLicense, vehiclePlate, CourierStatus.OFFLINE);
    }

    /** Perfil reconstruído a partir da origem de dados, com o status gravado. */
    public static CourierProfile restore(String cpf, String phone, CourierVehicleType vehicleType,
                                         String driverLicense, String vehiclePlate, CourierStatus status) {
        return fill(new CourierProfile(), cpf, phone, vehicleType, driverLicense, vehiclePlate, status);
    }

    private static CourierProfile fill(CourierProfile profile, String cpf, String phone,
                                       CourierVehicleType vehicleType, String driverLicense,
                                       String vehiclePlate, CourierStatus status) {
        profile.setCpf(Cpf.of(cpf));
        profile.setPhone(Phone.of(phone));
        profile.changeVehicle(vehicleType, driverLicense, vehiclePlate);
        profile.changeStatus(status);
        return profile;
    }

    public void setCpf(Cpf cpf) {
        this.cpf = Guard.requireNonNull(cpf, "CPF inválido");
    }

    public void setPhone(Phone phone) {
        this.phone = Guard.requireNonNull(phone, "Telefone inválido");
    }

    /**
     * Troca o veículo com a documentação dele. Validado inteiro antes de mudar: uma troca recusada
     * não deixa o perfil com veículo de um tipo e documentos de outro.
     */
    public void changeVehicle(CourierVehicleType newVehicleType, String newDriverLicense, String newVehiclePlate) {
        Guard.requireNonNull(newVehicleType, "Tipo de veículo inválido");
        String license = Guard.trimToNull(newDriverLicense);
        String plate = Guard.trimToNull(newVehiclePlate);
        if (newVehicleType.requiresLicense()) {
            Guard.require(license != null && plate != null, "Moto e carro exigem CNH e placa");
        } else {
            Guard.require(license == null && plate == null, "A pé ou de bicicleta não há CNH nem placa");
        }
        DriverLicense parsedLicense = license == null ? null : DriverLicense.of(license);
        LicensePlate parsedPlate = plate == null ? null : LicensePlate.of(plate);
        this.vehicleType = newVehicleType;
        this.driverLicense = parsedLicense;
        this.vehiclePlate = parsedPlate;
    }

    public void changeStatus(CourierStatus newStatus) {
        this.status = Guard.requireNonNull(newStatus, "Status do entregador inválido");
    }

    public Cpf getCpf() {
        return cpf;
    }

    public Phone getPhone() {
        return phone;
    }

    public CourierVehicleType getVehicleType() {
        return vehicleType;
    }

    /** Ausente quando o veículo não é motorizado. */
    public DriverLicense getDriverLicense() {
        return driverLicense;
    }

    /** Ausente quando o veículo não é motorizado. */
    public LicensePlate getVehiclePlate() {
        return vehiclePlate;
    }

    public CourierStatus getStatus() {
        return status;
    }
}
