package com.nc.formengine.flow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Stands in for an entity the application already had. See {@link HostPersistenceIsLeftAloneTest}. */
@Entity
public class Note {

    @Id
    @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }
}
