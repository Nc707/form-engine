package com.nc.formengine.flow;

import com.nc.formengine.dataimpl.repository.FormRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
// A datasource of its own, so that what the other tests in this module have written cannot decide
// whether this one passes.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hostpersistence;DB_CLOSE_DELAY=-1")
@Transactional
class HostPersistenceIsLeftAloneTest {

    @Autowired
    private NoteRepository applicationsOwnRepository;

    @Autowired
    private FormRepository theEnginesRepository;

    @Test
    void registersBothTheApplicationsRepositoriesAndTheEngines() {
        // Both being injectable at all is half the assertion: neither would be a bean if the other
        // module's contribution had replaced this one's.
        Note saved = applicationsOwnRepository.save(new Note());
        assertThat(saved.getId()).isNotNull();
        assertThat(applicationsOwnRepository.findById(saved.getId())).isPresent();

        // And the engine's own repository is backed by a real table, not merely registered.
        assertThat(theEnginesRepository.count()).isNotNegative();
    }
}
