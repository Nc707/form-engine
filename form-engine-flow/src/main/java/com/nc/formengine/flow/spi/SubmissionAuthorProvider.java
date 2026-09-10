package com.nc.formengine.flow.spi;

/**
 * Who is filling in and reading forms right now.
 *
 * <p>Every submission is stored against an author, and the list of drafts to resume is that author
 * questioned back. The module cannot know how an application establishes identity, so it asks.
 *
 * <p>The default answers with {@code formengine.flow.default-author}, which is fine for a demo and
 * for anything with no accounts. An application with Spring Security replaces it:
 *
 * <pre>{@code
 * @Bean
 * SubmissionAuthorProvider submissionAuthorProvider() {
 *     return () -> SecurityContextHolder.getContext().getAuthentication().getName();
 * }
 * }</pre>
 *
 * <p>This is identity, not authorisation. It decides whose drafts come back, and nothing more: the
 * responses views show every author's submissions to whoever opens them.
 */
@FunctionalInterface
public interface SubmissionAuthorProvider {

    /** Never null, and stable for as long as the same person is filling in the same form. */
    String currentAuthor();
}
