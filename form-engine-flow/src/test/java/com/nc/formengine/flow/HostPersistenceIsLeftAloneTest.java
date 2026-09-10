package com.nc.formengine.flow;

import com.nc.formengine.dataimpl.repository.FormRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * That depending on this jar does not cost an application its own persistence.
 *
 * <p>This is the regression test for the one mistake in all of this that would never show up here.
 * The obvious way to make the engine's entities and repositories findable is {@code @EntityScan} and
 * {@code @EnableJpaRepositories} in the modules that own them — and both <em>replace</em> the
 * mechanism an application's own entities and repositories are found by, so ours would work, theirs
 * would silently vanish, and nothing in this repository would notice. The modules contribute their
 * packages instead, which is additive.
 *
 * <p>{@link Note} and {@link NoteRepository} stand in for whatever the application already had.
 */
@SpringBootTest
class HostPersistenceIsLeftAloneTest {

    @Autowired
    private NoteRepository applicationsOwnRepository;

    @Autowired
    private FormRepository theEnginesRepository;

    @Test
    void registersBothTheApplicationsRepositoriesAndTheEngines() {
        assertThat(applicationsOwnRepository.count()).isZero();
        assertThat(theEnginesRepository.count()).isZero();
    }
}
