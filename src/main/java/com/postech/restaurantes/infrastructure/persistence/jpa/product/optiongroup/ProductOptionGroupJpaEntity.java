package com.postech.restaurantes.infrastructure.persistence.jpa.product.optiongroup;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optionvalue.ProductOptionValueJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Grupo de escolhas persistido como parte de um produto. */
@Entity
@Table(name = "product_option_groups")
public class ProductOptionGroupJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "min_selections", nullable = false)
    private int minSelections;

    @Column(name = "max_selections", nullable = false)
    private int maxSelections;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false, updatable = false)
    private Set<ProductOptionValueJpaEntity> values = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getMinSelections() {
        return minSelections;
    }

    public void setMinSelections(int minSelections) {
        this.minSelections = minSelections;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public void setMaxSelections(int maxSelections) {
        this.maxSelections = maxSelections;
    }

    public Set<ProductOptionValueJpaEntity> getValues() {
        return values;
    }

    public void replaceValues(Set<ProductOptionValueJpaEntity> values) {
        this.values.clear();
        this.values.addAll(values);
    }
}
