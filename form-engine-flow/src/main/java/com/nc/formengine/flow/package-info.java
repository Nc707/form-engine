/**
 * Vaadin views and components for the form engine, packaged so that an application only has to add
 * the dependency.
 *
 * <p>Everything lives in this one package on purpose. The alternative — a package per kind, or per
 * screen — reads well in a file tree but forces every collaborator a view is built from to be
 * {@code public}, which turns the whole inside of the module into API that cannot be changed
 * without breaking someone. Flat and package-private keeps the supported surface to what is
 * deliberately exposed:
 *
 * <ul>
 *   <li>the seven routed views, which an application navigates to and builds menu items from,
 *       listed in {@link com.nc.formengine.flow.FormEngineViews};
 *   <li>{@link com.nc.formengine.flow.SubmissionBrowser}, {@link com.nc.formengine.flow.ViewToolbar},
 *       {@link com.nc.formengine.flow.FieldEditor} and
 *       {@link com.nc.formengine.flow.FieldComponentFactory}, for embedding rather than routing;
 *   <li>{@link com.nc.formengine.flow.AnswerResolver} and
 *       {@link com.nc.formengine.flow.ResolvedAnswer}, which read a submission back against the
 *       definition version it was filled under;
 *   <li>{@code autoconfigure}, which registers all of it, and {@code spi}, the one question the
 *       module has to ask the application back.
 * </ul>
 *
 * <p>Everything else is package-private, and is meant to stay that way.
 */
package com.nc.formengine.flow;
