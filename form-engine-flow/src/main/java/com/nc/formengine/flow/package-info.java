/**
 * Vaadin views and components for the form engine, packaged so that an application only has to add
 * the dependency.
 *
 * <p>Everything here is one package, laid out in folders that carry no package of their own:
 *
 * <pre>
 *   view/        the routed screens
 *   components/  Vaadin components with no route: dialogs, panels, the field widgets, the toolbar
 *   utils/       what those stand on: policy, formatting, ordering, the write session, CSV, toasts
 * </pre>
 *
 * <p>The folders are for reading; the flat package is for encapsulation. Giving each folder its own
 * package would force every collaborator a view is built from to be {@code public}, which turns the
 * whole inside of the module into API that cannot be changed without breaking somebody. Kept flat,
 * the supported surface is only what is deliberately exposed:
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
 *       module has to ask the application back. These two are real packages, in folders of their
 *       own, because they are the seam an application actually touches.
 * </ul>
 *
 * <p>Everything else is package-private, and is meant to stay that way.
 *
 * <p>One consequence to know: because the folders under this one are not packages, an IDE will warn
 * that the declaration does not match the path. Maven passes every source file explicitly, so the
 * build does not care, and the compiled jar has only the three packages above.
 */
package com.nc.formengine.flow;
