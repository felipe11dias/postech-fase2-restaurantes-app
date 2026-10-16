package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant.officehour;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Tabela {@code restaurant_office_hours}: um intervalo de funcionamento do restaurante num dia da
 * semana. O {@code restaurant_id} é mapeado do lado do {@code RestaurantJpaEntity} (associação
 * unidirecional), para este pacote não depender do pacote do restaurante.
 *
 * <p>{@code day_of_week} é um tipo {@code ENUM} do PostgreSQL. Aqui é texto — a infraestrutura não
 * importa o tipo do domínio; quem converte é o gateway. {@code columnDefinition} diz ao
 * {@code ddl-auto: validate} qual é o tipo da coluna, e o {@code ?::day_of_week} faz o banco aceitar o
 * texto na coluna {@code ENUM}, como nos enums do entregador.
 */
@Entity
@Table(name = "restaurant_office_hours")
public class OfficeHourJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "day_of_week", nullable = false, columnDefinition = "day_of_week")
    @ColumnTransformer(write = "?::day_of_week")
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
