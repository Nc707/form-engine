package com.nc.formengine.flow;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stands in for a repository the application already had. See {@link HostPersistenceIsLeftAloneTest}. */
public interface NoteRepository extends JpaRepository<Note, Long> {
}
