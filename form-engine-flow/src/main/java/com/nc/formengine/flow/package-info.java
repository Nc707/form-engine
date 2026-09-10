/**
 * Vaadin views and components for the form engine, packaged so that an application only has to add
 * the dependency.
 *
 * <p>The module is laid out by what a class <em>is</em>, not by which screen it happens to serve:
 *
 * <ul>
 *   <li>{@code view} — the routed screens, one subpackage per feature. These are what
 *       {@link com.nc.formengine.flow.autoconfigure.FormEngineRouteRegistrar} mounts, and what an
 *       application names to navigate or to build a menu item.
 *   <li>{@code components} — Vaadin components with no route of their own: the dialogs and panels
 *       the builder is made of, the submission browser, the shared toolbar, and the field widgets
 *       every screen renders answers with.
 *   <li>{@code utils} — the helpers the two above stand on: policy, formatting, ordering, the
 *       builder's write session, CSV, and the notification toasts.
 *   <li>{@code autoconfigure} and {@code spi} — how the module registers itself, and the one
 *       question it has to ask the application back.
 * </ul>
 *
 * <p>One consequence worth knowing: because a view and the components it is made of no longer share
 * a package, most of this is {@code public} that would otherwise not need to be. Being public here
 * is not a promise of API stability — {@code view}, {@code spi},
 * {@link com.nc.formengine.flow.FormEngineViews} and the properties are the supported surface.
 */
package com.nc.formengine.flow;
