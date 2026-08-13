package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.FieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "field_definitions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    @ToString.Exclude
    private FormDefinition formDefinition;

    @Column(nullable = false)
    private String name; 

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    private Boolean required = false;

    /**
     * What to say when a required field was left unanswered, or null for the engine's default.
     *
     * <p>It lives next to the flag it belongs to. It used to be read off the field's {@code NOT_NULL}
     * restriction, which meant a form author had to add a redundant rule just to word the message.
     */
    @Column(name = "required_message")
    private String requiredMessage;

    /**
     * The validation rules of this field, owned by it: deleting the field deletes them, and dropping
     * one from the list deletes that row.
     */
    @OneToMany(mappedBy = "fieldDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    @ToString.Exclude
    private List<FieldRestriction> restrictions = new ArrayList<>();

    /**
     * The choices offered by a SELECT or MULTI_SELECT field, owned the same way. Reading them with
     * the field is what lets a client render it from a single request.
     */
    @OneToMany(mappedBy = "fieldDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    @ToString.Exclude
    private List<FieldOption> options = new ArrayList<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldDefinition that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}