package com.nc.formengine.demo;

import com.nc.formengine.flow.FormIndexView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * The application root, which the library deliberately does not claim.
 *
 * <p>Every view in {@code form-engine-flow} is mounted under a prefix, so "/" is free for the
 * application to do what it likes with. This one has nothing of its own to show and forwards to the
 * list of forms; it navigates by class, so it keeps working if the paths are reconfigured.
 */
@Route("")
public class DemoHomeView extends Div implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(FormIndexView.class);
    }
}
