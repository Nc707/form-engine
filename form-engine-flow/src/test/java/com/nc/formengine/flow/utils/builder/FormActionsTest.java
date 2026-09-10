package com.nc.formengine.flow;

import com.nc.formengine.model.enums.FormDefinitionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormActionsTest {

    @Test
    void aDraftWithFieldsCanDoEverythingButBranch() {
        var actions = FormActions.of(FormDefinitionStatus.DRAFT, 3);

        assertThat(actions.editable()).isTrue();
        assertThat(actions.publishable()).isTrue();
        assertThat(actions.deletable()).isTrue();
        assertThat(actions.versionable()).isFalse();
        assertThat(actions.reason()).isNull();
        assertThat(actions.publishBlockedReason()).isNull();
    }

    @Test
    void anEmptyDraftCannotBePublished() {
        var actions = FormActions.of(FormDefinitionStatus.DRAFT, 0);

        assertThat(actions.editable()).isTrue();
        assertThat(actions.publishable()).isFalse();
        assertThat(actions.publishBlockedReason()).contains("at least one field");
    }

    @Test
    void aPublishedFormIsFrozenAndSaysWhy() {
        var actions = FormActions.of(FormDefinitionStatus.PUBLISHED, 3);

        assertThat(actions.editable()).isFalse();
        assertThat(actions.publishable()).isFalse();
        assertThat(actions.deletable()).isFalse();
        assertThat(actions.versionable()).isTrue();
        assertThat(actions.reason()).contains("published").contains("new version");
        assertThat(actions.publishBlockedReason()).isEqualTo(actions.reason());
    }

    @Test
    void anArchivedFormIsFrozenTheSameWay() {
        var actions = FormActions.of(FormDefinitionStatus.ARCHIVED, 3);

        assertThat(actions.editable()).isFalse();
        assertThat(actions.deletable()).isFalse();
        assertThat(actions.versionable()).isTrue();
        assertThat(actions.reason()).contains("archived");
    }

    @Test
    void aFormWithoutAStatusIsTreatedAsFrozen() {
        // Guessing "editable" here would be the guess that loses data.
        var actions = FormActions.of(null, 3);

        assertThat(actions.editable()).isFalse();
        assertThat(actions.publishable()).isFalse();
        assertThat(actions.deletable()).isFalse();
        assertThat(actions.versionable()).isFalse();
    }
}
